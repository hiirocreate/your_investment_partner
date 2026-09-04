// ============================================================================
// 投資情報モニター - Web版 (Phase 1相当)
// Android版(Kotlin/Jetpack Compose)と同じモデル・命名・スコアリングロジックを、
// ビルド不要のプレーンなJavaScriptで再実装したもの。GitHub Pagesでそのまま配信できる。
//
// 設計方針(Android版と共通):
//  - 株価・法人番号検索は「設定」でユーザー自身のAPIキーを登録すれば実データ、
//    未登録またはエラー時はサンプル(モック)データに自動フォールバックする。
//  - ニュース(TDnet)はキー不要で常に実データ取得を試みる。取得に失敗した場合は
//    サンプルデータにこっそり差し替えず、「取得できませんでした」と正直に表示する
//    (ブラウザのCORS制限で失敗する可能性があるため、これが正直な状態)。
// ============================================================================

// ---------------------------------------------------------------------------
// 定数・表示ラベル
// ---------------------------------------------------------------------------
const EXCHANGE_LABELS = {
  TSE_PRIME: "東証プライム", TSE_STANDARD: "東証スタンダード", TSE_GROWTH: "東証グロース",
  OTHER: "その他", UNKNOWN: "不明"
};
const NEWS_CATEGORY_LABELS = {
  IR: "IR", EARNINGS: "決算", MERGER_ACQUISITION: "M&A", BUSINESS_PERFORMANCE: "業績",
  NEW_PRODUCT: "新商品", PARTNERSHIP: "提携", PERSONNEL: "人事", SCANDAL: "不祥事", OTHER: "その他"
};
const IMPORTANCE = {
  HIGHEST: { stars: 5, label: "最重要" }, HIGH: { stars: 4, label: "重要" },
  NOTABLE: { stars: 3, label: "注目" }, NORMAL: { stars: 2, label: "普通" },
  REFERENCE: { stars: 1, label: "参考" }
};
const TREND_CATEGORY_LABELS = {
  SURGING: "急上昇", VOLUME_SPIKE: "出来高急増", NEWS_SPIKE: "ニュース急増",
  NEW_IPO: "新規上場", PLUNGING: "急落", LONG_TERM_WATCH: "中長期注目"
};
const TREND_CATEGORY_ORDER = ["SURGING", "VOLUME_SPIKE", "NEWS_SPIKE", "NEW_IPO", "PLUNGING", "LONG_TERM_WATCH"];
const CHART_RANGES = [
  { key: "D1", label: "1D", days: 1 }, { key: "W1", label: "1W", days: 7 },
  { key: "M1", label: "1M", days: 30 }, { key: "M3", label: "3M", days: 90 },
  { key: "M6", label: "6M", days: 180 }, { key: "Y1", label: "1Y", days: 365 },
  { key: "Y5", label: "5Y", days: 1825 }
];

// ---------------------------------------------------------------------------
// ユーティリティ
// ---------------------------------------------------------------------------
function escapeHtml(str) {
  return String(str).replace(/[&<>"']/g, c => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
function formatYen(value) {
  if (value == null || Number.isNaN(value)) return "-";
  return new Intl.NumberFormat("ja-JP", { maximumFractionDigits: 1 }).format(value);
}
function relativeTimeLabel(epochMillis) {
  const diffMin = Math.floor((Date.now() - epochMillis) / 60000);
  if (diffMin < 1) return "たった今";
  if (diffMin < 60) return `${diffMin}分前`;
  if (diffMin < 60 * 24) return `${Math.floor(diffMin / 60)}時間前`;
  return `${Math.floor(diffMin / (60 * 24))}日前`;
}
function isNewsNew(publishedAtEpochMillis) {
  return Date.now() - publishedAtEpochMillis < 3 * 60 * 60 * 1000;
}
function hashString(str) {
  let h = 0;
  for (let i = 0; i < str.length; i++) { h = (h * 31 + str.charCodeAt(i)) | 0; }
  return h;
}
// mulberry32: 軽量なシード付き擬似乱数生成器(モックデータを毎回それらしく、かつ再現性を持って生成するため)
function mulberry32(seed) {
  let a = seed >>> 0;
  return function () {
    a |= 0; a = (a + 0x6D2B79F5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
function randRange(rng, min, max) { return min + rng() * (max - min); }
function randInt(rng, min, max) { return Math.floor(randRange(rng, min, max + 1)); }
function fmtDate(epochMillis) {
  const d = new Date(epochMillis);
  return `${d.getMonth() + 1}/${d.getDate()}`;
}

// ---------------------------------------------------------------------------
// モックデータ(Android版 MockCompanyProvider.sampleCompanies と同一内容)
// ---------------------------------------------------------------------------
const SAMPLE_COMPANIES = [
  { companyId: "7203", companyName: "トヨタ自動車", officialName: "トヨタ自動車株式会社", corporateNumber: "6180301018771", stockCode: "7203", exchange: "TSE_PRIME", website: "https://global.toyota/jp/", irUrl: "https://global.toyota/jp/ir/", industry: "輸送用機器", marketCapBillionYen: 45000, per: 10.2, pbr: 1.1, roe: 11.5, revenueBillionYen: 45000, operatingIncomeBillionYen: 4800, fiveYearGrowthScore: 72 },
  { companyId: "7267", companyName: "ホンダ", officialName: "本田技研工業株式会社", corporateNumber: "7010401027570", stockCode: "7267", exchange: "TSE_PRIME", industry: "輸送用機器", marketCapBillionYen: 8900, per: 7.8, pbr: 0.7, roe: 9.0, revenueBillionYen: 20400, operatingIncomeBillionYen: 1300, fiveYearGrowthScore: 58 },
  { companyId: "9984", companyName: "ソフトバンクグループ", officialName: "ソフトバンクグループ株式会社", corporateNumber: "5010401018261", stockCode: "9984", exchange: "TSE_PRIME", industry: "情報・通信業", marketCapBillionYen: 12000, per: 14.5, pbr: 1.3, roe: 6.2, revenueBillionYen: 6200, operatingIncomeBillionYen: 900, fiveYearGrowthScore: 65 },
  { companyId: "6758", companyName: "ソニーグループ", officialName: "ソニーグループ株式会社", corporateNumber: "3010401067193", stockCode: "6758", exchange: "TSE_PRIME", industry: "電気機器", marketCapBillionYen: 16000, per: 19.4, pbr: 2.5, roe: 12.8, revenueBillionYen: 13000, operatingIncomeBillionYen: 1350, fiveYearGrowthScore: 80 },
  { companyId: "9432", companyName: "NTT", officialName: "日本電信電話株式会社", corporateNumber: "7010001008844", stockCode: "9432", exchange: "TSE_PRIME", industry: "情報・通信業", marketCapBillionYen: 15500, per: 12.1, pbr: 1.6, roe: 13.9, revenueBillionYen: 13400, operatingIncomeBillionYen: 1800, fiveYearGrowthScore: 55 },
  { companyId: "8306", companyName: "三菱UFJフィナンシャル・グループ", officialName: "株式会社三菱UFJフィナンシャル・グループ", corporateNumber: "7010001008889", stockCode: "8306", exchange: "TSE_PRIME", industry: "銀行業", marketCapBillionYen: 22000, per: 11.0, pbr: 0.9, roe: 8.5, revenueBillionYen: 7000, operatingIncomeBillionYen: 1900, fiveYearGrowthScore: 61 },
  { companyId: "6501", companyName: "日立製作所", officialName: "株式会社日立製作所", corporateNumber: "5010001008771", stockCode: "6501", exchange: "TSE_PRIME", industry: "電気機器", marketCapBillionYen: 17000, per: 22.0, pbr: 2.9, roe: 14.0, revenueBillionYen: 9600, operatingIncomeBillionYen: 900, fiveYearGrowthScore: 84 },
  { companyId: "4661", companyName: "オリエンタルランド", officialName: "株式会社オリエンタルランド", corporateNumber: "4030001018108", stockCode: "4661", exchange: "TSE_PRIME", industry: "サービス業", marketCapBillionYen: 5200, per: 32.0, pbr: 6.5, roe: 18.2, revenueBillionYen: 630, operatingIncomeBillionYen: 160, fiveYearGrowthScore: 70 },
  { companyId: "9983", companyName: "ファーストリテイリング", officialName: "株式会社ファーストリテイリング", corporateNumber: "7011001008738", stockCode: "9983", exchange: "TSE_PRIME", industry: "小売業", marketCapBillionYen: 13000, per: 38.0, pbr: 9.0, roe: 23.0, revenueBillionYen: 3100, operatingIncomeBillionYen: 500, fiveYearGrowthScore: 88 },
  { companyId: "4755", companyName: "楽天グループ", officialName: "楽天グループ株式会社", corporateNumber: "5011001048510", stockCode: "4755", exchange: "TSE_PRIME", industry: "サービス業", marketCapBillionYen: 1700, per: null, pbr: 3.0, roe: -12.0, revenueBillionYen: 2100, operatingIncomeBillionYen: -150, fiveYearGrowthScore: 30 },
  { companyId: "IPO001", companyName: "ネクストAIホールディングス", officialName: "株式会社ネクストAIホールディングス", corporateNumber: "1234567890123", stockCode: "9999", exchange: "TSE_GROWTH", industry: "情報・通信業", marketCapBillionYen: 85, per: 45.0, pbr: 8.0, roe: 15.0, revenueBillionYen: 12, operatingIncomeBillionYen: 1.5, fiveYearGrowthScore: 90 }
];
const READING_INDEX = {
  "7203": ["とよた", "TOYOTA", "toyota"], "9984": ["SBG", "ソフトバンク"],
  "9432": ["エヌティティ", "NTT", "日本電信電話"], "6758": ["SONY", "sony"], "9983": ["ユニクロ", "UNIQLO"]
};
function companyById(id) { return SAMPLE_COMPANIES.find(c => c.companyId === id) || null; }
const RELATIONS_INDEX = {
  "9984": [{ fromCompanyId: "9984", toCompany: companyById("6758"), relationType: "AFFILIATE" }],
  "7203": [{ fromCompanyId: "7203", toCompany: companyById("7267"), relationType: "PARTNER" }]
};
const RELATION_LABELS = { AFFILIATE: "関連会社", PARTNER: "提携企業" };

const BASE_PRICES = { "7203": 2850, "7267": 1650, "9984": 8200, "6758": 3100, "9432": 165, "8306": 1780, "6501": 3600, "4661": 4600, "9983": 47000, "4755": 780, "IPO001": 3200 };
function basePrice(companyId) { return BASE_PRICES[companyId] ?? 1000; }

const NEWS_TEMPLATES = [
  { title: "通期業績予想を上方修正", summary: n => `${n}は通期の連結業績予想を上方修正したと発表した(モックデータ)。`, category: "EARNINGS", importance: "HIGHEST", impactNote: "業績予想の上方修正のため株価への影響度は高いと考えられます", sourceName: "適時開示(モック)" },
  { title: "新製品を発表", summary: n => `${n}は新製品ラインナップを発表した(モックデータ)。`, category: "NEW_PRODUCT", importance: "NOTABLE", impactNote: "新製品の市場規模により影響度は変動します", sourceName: "企業公式サイト(モック)" },
  { title: "業務提携を締結", summary: n => `${n}は他社との業務提携を発表した(モックデータ)。`, category: "PARTNERSHIP", importance: "HIGH", impactNote: "提携内容次第で中期的な業績への影響が見込まれます", sourceName: "IRページ(モック)" },
  { title: "第2四半期決算を発表", summary: n => `${n}は第2四半期の決算を発表した(モックデータ)。`, category: "EARNINGS", importance: "HIGH", impactNote: "決算内容の詳細確認が推奨されます", sourceName: "決算短信(モック)" },
  { title: "組織変更を発表", summary: n => `${n}は役員人事および組織変更を発表した(モックデータ)。`, category: "PERSONNEL", importance: "REFERENCE", impactNote: "経営体制の変更のため中長期的な影響を注視", sourceName: "プレスリリース(モック)" },
  { title: "自社株買いを発表", summary: n => `${n}は自己株式の取得を発表した(モックデータ)。`, category: "IR", importance: "HIGH", impactNote: "需給改善要因として株価にポジティブな影響の可能性", sourceName: "適時開示(モック)" }
];

// ---------------------------------------------------------------------------
// ローカル保存(端末のブラウザだけに保存。他の誰にも送信されない)
// ---------------------------------------------------------------------------
const LS_WATCHLIST = "invmon:watchlist:v1";
const LS_SETTINGS = "invmon:settings:v1";

function lsGet(key, fallback) {
  try { const raw = localStorage.getItem(key); return raw ? JSON.parse(raw) : fallback; }
  catch (e) { return fallback; }
}
function lsSet(key, value) {
  try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) { /* 保存領域が使えない場合は何もしない */ }
}

const Storage = {
  getWatchlist() { return lsGet(LS_WATCHLIST, []); }, // [{companyId, addedAtEpochMillis}]
  isWatched(companyId) { return this.getWatchlist().some(w => w.companyId === companyId); },
  addToWatchlist(companyId) {
    const list = this.getWatchlist();
    if (!list.some(w => w.companyId === companyId)) {
      list.push({ companyId, addedAtEpochMillis: Date.now() });
      lsSet(LS_WATCHLIST, list);
    }
  },
  removeFromWatchlist(companyId) {
    lsSet(LS_WATCHLIST, this.getWatchlist().filter(w => w.companyId !== companyId));
  },
  getSettings() {
    return lsGet(LS_SETTINGS, {
      themeMode: "SYSTEM", notificationsEnabled: true, notificationLevel: "IMPORTANT_ONLY",
      jquantsApiKey: null, houjinBangouAppId: null
    });
  },
  setSetting(key, value) {
    const s = this.getSettings();
    s[key] = value;
    lsSet(LS_SETTINGS, s);
  }
};

// ---------------------------------------------------------------------------
// CompanyProvider (モックのみ - Android版と同じ企業マスタ)
// ---------------------------------------------------------------------------
const CompanyProvider = {
  async searchCompanies(query) {
    await sleep(150);
    const q = query.trim();
    if (!q) return [];
    const sampleMatches = SAMPLE_COMPANIES.filter(c =>
      c.companyName.includes(q) || c.officialName.includes(q) ||
      (c.stockCode && c.stockCode.includes(q)) ||
      (READING_INDEX[c.companyId] || []).some(alias => alias.toLowerCase().includes(q.toLowerCase()))
    );
    // 監視対象10社(詳細プロフィールあり)に加えて、東証の全上場銘柄(companies.json、簡易プロフィール)
    // からも検索する。「株の購入ができる企業を全て見れるようにしたい」という要望への対応。
    const masterMatches = await CompanyMasterProvider.search(q, 40).catch(() => []);
    const seen = new Set(sampleMatches.map(c => c.companyId));
    const merged = sampleMatches.slice();
    for (const c of masterMatches) {
      if (!seen.has(c.companyId)) { merged.push(c); seen.add(c.companyId); }
    }
    return merged.slice(0, 50);
  },
  async getCompany(companyId) { return companyById(companyId); },
  async getRelatedCompanies(companyId) { return RELATIONS_INDEX[companyId] || []; }
};
function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

// ---------------------------------------------------------------------------
// MarketDataProvider: モック + 実データ(J-Quants) + 自動切り替え(Composite)
// ---------------------------------------------------------------------------
const MockMarketDataProvider = {
  async getQuote(companyId) {
    await sleep(120);
    const base = basePrice(companyId);
    const rng = mulberry32((hashString(companyId) + Math.floor(Date.now() / 60000)) >>> 0);
    const prevClose = base;
    const drift = base * randRange(rng, -0.03, 0.035);
    const price = Math.max(1, base + drift);
    const high = Math.max(price, prevClose) * (1 + randRange(rng, 0, 0.01));
    const low = Math.min(price, prevClose) * (1 - randRange(rng, 0, 0.01));
    return {
      companyId, price, previousClose: prevClose, open: prevClose * (1 + randRange(rng, -0.005, 0.005)),
      dayHigh: high, dayLow: low, volume: randInt(rng, 500000, 20000000),
      asOfEpochMillis: Date.now(), isStale: false
    };
  },
  async getHistory(companyId, rangeKey) {
    await sleep(150);
    const range = CHART_RANGES.find(r => r.key === rangeKey) || CHART_RANGES[2];
    const base = basePrice(companyId);
    const rng = mulberry32(hashString(companyId) >>> 0);
    const count = { D1: 24, W1: 7, M1: 30, M3: 90, M6: 180, Y1: 365, Y5: 60 }[rangeKey] ?? 30;
    const stepMs = rangeKey === "D1" ? 3600e3 : rangeKey === "Y5" ? 30 * 86400e3 : 86400e3;
    const now = Date.now();
    let value = base * 0.85;
    const points = [];
    for (let i = count; i >= 0; i--) {
      const trend = Math.sin((i / count) * Math.PI * 2) * base * 0.04;
      value = Math.max(base * 0.4, value + randRange(rng, -base * 0.01, base * 0.012));
      const close = Math.max(1, value + trend);
      const open = close * (1 + randRange(rng, -0.008, 0.008));
      const high = Math.max(open, close) * (1 + randRange(rng, 0, 0.006));
      const low = Math.min(open, close) * (1 - randRange(rng, 0, 0.006));
      points.push({ timestampEpochMillis: now - i * stepMs, open, high, low, close, volume: randInt(rng, 300000, 15000000) });
    }
    return points;
  }
};

// J-Quants (api.jquants.com) does not send CORS headers, so a browser calling it directly from
// a page hosted on a different origin is always blocked - confirmed by live testing (the same
// way the TDnet CORS block was confirmed), not guessed. There is no client-side workaround, so
// real market data can no longer be fetched with a personally-registered browser-stored key
// (that field has been removed from Settings). Instead, .github/workflows/fetch-market-data.yml
// fetches it server-side (using JQUANTS_API_KEY, a GitHub Secret) on a daily schedule and
// commits it to these same-origin static files, which QuoteSnapshotProvider/CompanyMasterProvider
// below read - the same pattern already used for TDnet news.
const QuoteSnapshotProvider = {
  DATA_URL: "./data/quotes.json",
  CACHE_TTL_MILLIS: 10 * 60 * 1000,
  // 無料プランのデータは常に約12週間遅れなので、常に「最新ではない」という正直な表示にする
  // (更新に失敗しているのではなく、プランの仕様上そもそも最新ではないため)。
  STALE_THRESHOLD_MILLIS: 10 * 24 * 60 * 60 * 1000,
  _cache: null,
  _cacheAtMs: 0,
  _pending: null,
  async loadData() {
    const now = Date.now();
    if (this._cache && (now - this._cacheAtMs) < this.CACHE_TTL_MILLIS) return this._cache;
    if (this._pending) return this._pending;
    this._pending = (async () => {
      try {
        const res = await fetch(this.DATA_URL, { cache: "no-store" });
        if (!res.ok) throw new Error(`quotes.json returned HTTP ${res.status}`);
        const body = await res.json();
        this._cache = body;
        this._cacheAtMs = Date.now();
        return body;
      } finally {
        this._pending = null;
      }
    })();
    return this._pending;
  },
  async getQuote(companyId) {
    const data = await this.loadData();
    const q = data && data.quotes ? data.quotes[companyId] : null;
    if (!q || q.close == null) return null;
    const asOfEpochMillis = Date.parse(`${q.date}T15:00:00+09:00`); // 大引け時刻を基準時刻とする
    const asOf = Number.isNaN(asOfEpochMillis) ? Date.now() : asOfEpochMillis;
    // 前日終値が取得できなかった銘柄は騰落率を計算できないため、変化なし(0%)として安全側に倒す
    const previousClose = q.prevClose != null ? q.prevClose : q.close;
    return {
      companyId, price: q.close, previousClose, open: q.open,
      dayHigh: q.high, dayLow: q.low, volume: q.volume,
      asOfEpochMillis: asOf, isStale: (Date.now() - asOf) > this.STALE_THRESHOLD_MILLIS
    };
  },
  // 「注目企業」を監視対象10社だけでなく実データのある全銘柄から拾えるようにするための一括アクセス。
  async getAllQuotes() {
    const data = await this.loadData().catch(() => null);
    const quotes = data && data.quotes ? data.quotes : {};
    const asOfEpochMillis = data && data.asOfDate ? Date.parse(`${data.asOfDate}T15:00:00+09:00`) : NaN;
    const asOf = Number.isNaN(asOfEpochMillis) ? Date.now() : asOfEpochMillis;
    const isStale = (Date.now() - asOf) > this.STALE_THRESHOLD_MILLIS;
    return Object.keys(quotes).map(companyId => {
      const q = quotes[companyId];
      if (q.close == null) return null;
      const previousClose = q.prevClose != null ? q.prevClose : q.close;
      return { companyId, price: q.close, previousClose, open: q.open, dayHigh: q.high, dayLow: q.low, volume: q.volume, asOfEpochMillis: asOf, isStale };
    }).filter(Boolean);
  }
};

function mapMarketToExchangeKey(marketName) {
  if (!marketName) return "UNKNOWN";
  if (marketName.includes("プライム")) return "TSE_PRIME";
  if (marketName.includes("スタンダード")) return "TSE_STANDARD";
  if (marketName.includes("グロース")) return "TSE_GROWTH";
  return "OTHER";
}
// CompanyMasterProvider: 東証の全上場銘柄(約4,000社)の一覧。SAMPLE_COMPANIESが持つ10社分の
// 詳細プロフィール(PER/PBR/関連企業など)は含まないが、社名・証券コード・市場区分・業種は
// どの銘柄についても実データで表示できる(spec: 「株の購入ができる企業を全て見れるようにしたい」)。
const CompanyMasterProvider = {
  DATA_URL: "./data/companies.json",
  CACHE_TTL_MILLIS: 30 * 60 * 1000,
  _cache: null,
  _cacheAtMs: 0,
  _pending: null,
  _byCode: null,
  async loadData() {
    const now = Date.now();
    if (this._cache && (now - this._cacheAtMs) < this.CACHE_TTL_MILLIS) return this._cache;
    if (this._pending) return this._pending;
    this._pending = (async () => {
      try {
        const res = await fetch(this.DATA_URL, { cache: "no-store" });
        if (!res.ok) throw new Error(`companies.json returned HTTP ${res.status}`);
        const body = await res.json();
        this._cache = body;
        this._cacheAtMs = Date.now();
        this._byCode = new Map((body.companies || []).map(c => [c.code, c]));
        return body;
      } finally {
        this._pending = null;
      }
    })();
    return this._pending;
  },
  async search(query, limit) {
    await this.loadData().catch(() => null);
    if (!this._byCode) return [];
    const q = query.trim();
    if (!q) return [];
    const qLower = q.toLowerCase();
    const out = [];
    for (const c of this._byCode.values()) {
      if (c.name.includes(q) || c.code.includes(q) || (c.nameEn && c.nameEn.toLowerCase().includes(qLower))) {
        out.push(this.toCompanyShape(c));
        if (out.length >= limit) break;
      }
    }
    return out;
  },
  async getByCode(code) {
    await this.loadData().catch(() => null);
    return this._byCode ? (this._byCode.get(code) || null) : null;
  },
  toCompanyShape(m) {
    return {
      companyId: m.code, companyName: m.name, officialName: m.name, corporateNumber: null,
      stockCode: m.code, exchange: mapMarketToExchangeKey(m.market), website: null, irUrl: null,
      industry: m.sector33 || "-", marketCapBillionYen: null, per: null, pbr: null, roe: null,
      revenueBillionYen: null, operatingIncomeBillionYen: null, fiveYearGrowthScore: null,
      isFullProfile: false
    };
  }
};
// 監視企業タブ・検索結果など、SAMPLE_COMPANIES(詳細プロフィールあり)にもCompanyMasterProvider
// (全銘柄・簡易プロフィール)にも一致しなかった場合でも、画面を壊さないための最終フォールバック。
async function resolveCompanyLite(companyId) {
  const full = companyById(companyId);
  if (full) return full;
  const master = await CompanyMasterProvider.getByCode(companyId);
  if (master) return CompanyMasterProvider.toCompanyShape(master);
  return {
    companyId, companyName: companyId, officialName: companyId, corporateNumber: null,
    stockCode: companyId, exchange: "UNKNOWN", website: null, irUrl: null, industry: "-",
    marketCapBillionYen: null, per: null, pbr: null, roe: null,
    revenueBillionYen: null, operatingIncomeBillionYen: null, fiveYearGrowthScore: null,
    isFullProfile: false
  };
}

const MarketRepository = {
  async getQuote(companyId) {
    const snapshot = await QuoteSnapshotProvider.getQuote(companyId).catch(() => null);
    if (snapshot) return snapshot;
    return MockMarketDataProvider.getQuote(companyId);
  },
  async getHistory(companyId, rangeKey) {
    // 日次スナップショットは「1日分」の四本値のみで、任意企業の期間チャートまでは持たない
    // (全銘柄分を日数分さかのぼって取得するとレート制限・実行時間の面で現実的でないため)。
    // 正直に「サンプルデータ」であることを示すため、実データへの自動フォールバックはしない。
    return MockMarketDataProvider.getHistory(companyId, rangeKey);
  },
  // newsCount: そのcompanyIdがニュース「recent」フィード(直近取得分)に何件登場したか(実データ)。
  // 監視対象10社以外(PER・5年成長スコアなどの財務データを持たない)ではgrowth/valuationは中立値に
  // なるため、totalScoreは「動き(値動き・出来高・話題性)」寄りの参考値になる - 財務指標を捏造しない
  // ための意図的な設計。
  computeScores(company, quote, newsCount) {
    const momentum = clamp(((quote.changePercent + 5) / 10) * 100, 0, 100) | 0;
    const growth = clamp(company.fiveYearGrowthScore ?? 0, 0, 100);
    const per = company.per;
    const valuation = per == null ? 50 : per <= 0 ? 20 : per < 10 ? 85 : per < 20 ? 65 : per < 35 ? 45 : 25;
    const news = clamp((newsCount || 0) * 35, 0, 100);
    const volumeScore = clamp((quote.volume / 20000000) * 100, 0, 100) | 0;
    const longTerm = clamp(growth * 0.6 + valuation * 0.4, 0, 100) | 0;
    const totalScore = clamp((momentum + growth + valuation + news + volumeScore + longTerm) / 6, 0, 100) | 0;
    return { momentumScore: momentum, growthScore: growth, valuationScore: valuation, newsScore: news, volumeScore, longTermScore: longTerm, totalScore };
  },
  categorizeTrend(company, quote, scores) {
    const cats = [];
    if (quote.changePercent >= 2.0) cats.push("SURGING");
    if (quote.changePercent <= -2.0) cats.push("PLUNGING");
    if (scores.volumeScore >= 60) cats.push("VOLUME_SPIKE");
    if (scores.newsScore >= 70) cats.push("NEWS_SPIKE");
    if (company.companyId === "IPO001") cats.push("NEW_IPO");
    if (quote.changePercent < 0 && scores.longTermScore >= 60) cats.push("LONG_TERM_WATCH");
    if (cats.length === 0 && Math.abs(quote.changePercent) < 2.0 && scores.longTermScore >= 55) cats.push("LONG_TERM_WATCH");
    return cats;
  },
  // ニュース「recent」フィード(直近取得分、実データ)の中で、companyIdごとに何件登場したかを集計する。
  async newsCountsByCompany() {
    const data = await TdnetProvider.loadData().catch(() => null);
    const counts = {};
    if (data) {
      for (const entry of (data.recent || [])) {
        const item = TdnetProvider.toNewsItem(entry && entry.Tdnet, null);
        if (item) counts[item.companyId] = (counts[item.companyId] || 0) + 1;
      }
    }
    return counts;
  },
  // 監視対象10社だけを対象にした従来のスコアリング(全銘柄の株価スナップショットがまだ無い間の
  // フォールバック - ワークフロー未実行や、GitHub SecretのAPIキー未設定の状態でも動作を確認できる
  // ようにするため)。
  async getTrendingCompaniesFromSample() {
    const newsCounts = await this.newsCountsByCompany();
    return Promise.all(SAMPLE_COMPANIES.map(async company => {
      const quote = withChange(await this.getQuote(company.companyId));
      const newsCount = newsCounts[company.companyId] || 0;
      const scores = this.computeScores(company, quote, newsCount);
      const categories = this.categorizeTrend(company, quote, scores);
      return { company, quote, scores, categories, newsCount24h: newsCount };
    }));
  },
  async getTrendingCompanies() {
    // 監視対象10社だけを対象にすると、実データでは1日の値動きが±2%を超える銘柄が少なく、
    // 「該当する企業がありません」ばかりになってしまう(ご指摘の通り)。実データのある全銘柄
    // (quotes.json)から急騰・急落・出来高急増・話題性のある企業を拾うことで、大企業10社に
    // 限らない、実際に動きのある企業が表示されるようにした。
    const allQuotes = await QuoteSnapshotProvider.getAllQuotes();
    if (allQuotes.length === 0) return this.getTrendingCompaniesFromSample();
    const newsCounts = await this.newsCountsByCompany();
    const results = await Promise.all(allQuotes.map(async q => {
      const company = await resolveCompanyLite(q.companyId);
      const quote = withChange(q);
      const newsCount = newsCounts[q.companyId] || 0;
      const scores = this.computeScores(company, quote, newsCount);
      const categories = this.categorizeTrend(company, quote, scores);
      return { company, quote, scores, categories, newsCount24h: newsCount };
    }));
    // IPOサンプル企業(実データを持たない架空の企業)は別枠(getIpoCompanies)で扱うため、
    // 全銘柄集計にはそもそも含まれない。カテゴリに一つも該当しない銘柄は「注目」ではないので除外する。
    return results.filter(r => r.categories.length > 0);
  },
  async getIpoCompanies() {
    const ipo = companyById("IPO001");
    if (!ipo) return [];
    const quote = withChange(await this.getQuote(ipo.companyId));
    return [{
      company: ipo, listingDateEpochMillis: Date.now() - 14 * 86400e3, market: ipo.exchange,
      offeringPrice: 2500, currentPrice: quote.price, firstDayPrice: 3400
    }];
  }
};
function clamp(v, min, max) { return Math.min(max, Math.max(min, v)); }
function withChange(quote) {
  const change = quote.price - quote.previousClose;
  const changePercent = quote.previousClose === 0 ? 0 : (change / quote.previousClose) * 100;
  return { ...quote, change, changePercent };
}

// ---------------------------------------------------------------------------
// NewsProvider: TDnet実データ(キー不要)。失敗時はモックに差し替えず、正直にエラーを投げる
// ---------------------------------------------------------------------------
function categorizeNewsTitle(title) {
  const has = words => words.some(w => title.includes(w));
  if (has(["決算短信", "四半期決算", "決算説明", "決算補足"])) return "EARNINGS";
  if (has(["業績予想", "上方修正", "下方修正", "業績の修正"])) return "BUSINESS_PERFORMANCE";
  if (has(["合併", "株式交換", "株式移転", "買収", "子会社化", "会社分割"])) return "MERGER_ACQUISITION";
  if (has(["業務提携", "資本提携", "業務・資本提携"])) return "PARTNERSHIP";
  if (has(["新製品", "新サービス", "新商品"])) return "NEW_PRODUCT";
  if (has(["役員", "人事", "代表取締役の異動"])) return "PERSONNEL";
  if (has(["行政処分", "不祥事", "訴訟", "調査委員会", "特別損失", "上場廃止"])) return "SCANDAL";
  if (has(["自己株式", "配当", "株式分割", "増資", "第三者割当", "新株予約権"])) return "IR";
  return "OTHER";
}
function estimateNewsImportance(title, category) {
  const has = words => words.some(w => title.includes(w));
  if (has(["上方修正", "下方修正", "業績予想の修正", "配当予想の修正", "民事再生", "破産", "上場廃止"])) return "HIGHEST";
  if (["EARNINGS", "MERGER_ACQUISITION", "SCANDAL"].includes(category)) return "HIGH";
  if (["PARTNERSHIP", "IR"].includes(category)) return "NOTABLE";
  return "NORMAL";
}
const TdnetProvider = {
  // The TDnet API (webapi.yanoshin.jp) does not send an Access-Control-Allow-Origin header, so
  // browsers block direct fetches to it from a page hosted on a different origin (CORS). There
  // is no client-side workaround for that - the fix is server-side. A GitHub Actions workflow
  // (.github/workflows/fetch-news.yml) fetches TDnet on a schedule from a GitHub-hosted runner
  // (no CORS restriction there) and commits the result to this same-origin static file, which
  // the browser can fetch freely. News here is therefore a periodic snapshot (refreshed roughly
  // every 20 minutes), not literally live - fetchedAtEpochMillis() below is how callers show
  // that honestly instead of implying it's real-time (spec section 40).
  DATA_URL: "./data/news.json",
  CACHE_TTL_MILLIS: 5 * 60 * 1000,
  _cache: null,
  _cacheAtMs: 0,
  _pending: null,
  async loadData() {
    const now = Date.now();
    if (this._cache && (now - this._cacheAtMs) < this.CACHE_TTL_MILLIS) return this._cache;
    // getLatestNews() below fires several fetch() calls (one per tracked company) at once on a
    // cold cache - without this, each would race to fetch the same file separately instead of
    // sharing one in-flight request.
    if (this._pending) return this._pending;
    this._pending = (async () => {
      try {
        const res = await fetch(this.DATA_URL, { cache: "no-store" });
        if (!res.ok) throw new Error(`news.json returned HTTP ${res.status}`);
        const body = await res.json();
        this._cache = body;
        this._cacheAtMs = Date.now();
        return body;
      } finally {
        this._pending = null;
      }
    })();
    return this._pending;
  },
  fetchedAtEpochMillis() {
    return this._cache ? this._cache.fetchedAtEpochMillis || null : null;
  },
  async fetch(condition, limit, companyNameFallback) {
    const data = await this.loadData();
    const items = condition === "recent"
      ? (data.recent || [])
      : ((data.byCompany && data.byCompany[condition]) || []);
    return items.slice(0, limit).map(entry => this.toNewsItem(entry && entry.Tdnet, companyNameFallback)).filter(Boolean);
  },
  toNewsItem(tdnet, companyNameFallback) {
    if (!tdnet || !tdnet.title) return null;
    const rawCode = tdnet.company_code || "";
    const stockCode = rawCode.length >= 4 ? rawCode.slice(0, 4) : rawCode;
    const companyName = tdnet.company_name || companyNameFallback || stockCode;
    const documentUrl = tdnet.document_url || "";
    const publishedAt = Date.parse((tdnet.pubdate || "").replace(" ", "T") + "+09:00");
    const publishedAtEpochMillis = Number.isNaN(publishedAt) ? Date.now() : publishedAt;
    const category = categorizeNewsTitle(tdnet.title);
    const importance = estimateNewsImportance(tdnet.title, category);
    const id = tdnet.id || `${stockCode}-${hashString(tdnet.title)}-${publishedAtEpochMillis}`;
    return {
      id: `tdnet-${id}`, companyId: stockCode, companyName, title: tdnet.title,
      summary: "TDnet(適時開示情報)より取得。詳細は原文(PDF)をご確認ください。",
      source: { sourceName: "TDnet(適時開示情報)", sourceUrl: documentUrl, publishedAtEpochMillis, collectedAtEpochMillis: Date.now() },
      category, importance, stockImpactNote: "適時開示に基づく情報です。株価への影響度はご自身でご判断ください。",
      relatedCount: 0, contentHash: documentUrl || tdnet.title
    };
  }
};

const NewsRepository = {
  async getNewsForCompany(companyId, companyName) {
    const items = await TdnetProvider.fetch(companyId, 20, companyName);
    return this.deduplicate(items);
  },
  async getLatestNews(limit) {
    // The full market-wide "recent" TDnet feed (news about any of the ~4,000 companies listed on
    // TSE), not narrowed to this app's 10 tracked sample companies - users want to see notable
    // market news broadly, not only about the companies they happen to be watching. Tapping a
    // news item about a company outside the tracked roster now opens a lightweight fallback
    // detail view (see renderUntrackedCompanyDetail in the UI section) instead of the old hard
    // "企業情報が見つかりません" error, so broader coverage no longer means dead-end taps.
    const items = await TdnetProvider.fetch("recent", limit, null);
    return this.deduplicate(items).sort((a, b) => b.source.publishedAtEpochMillis - a.source.publishedAtEpochMillis);
  },
  deduplicate(items) {
    const sorted = [...items].sort((a, b) => b.source.publishedAtEpochMillis - a.source.publishedAtEpochMillis);
    const kept = [];
    outer: for (const candidate of sorted) {
      for (let i = 0; i < kept.length; i++) {
        if (this.isDuplicate(kept[i], candidate)) {
          kept[i] = { ...kept[i], relatedCount: kept[i].relatedCount + 1 + candidate.relatedCount };
          continue outer;
        }
      }
      kept.push(candidate);
    }
    return kept;
  },
  isDuplicate(a, b) {
    if (a.companyId !== b.companyId) return false;
    if (a.contentHash && a.contentHash === b.contentHash) return true;
    const sameCategory = a.category === b.category;
    const closeInTime = Math.abs(a.source.publishedAtEpochMillis - b.source.publishedAtEpochMillis) < 6 * 3600e3;
    const similar = this.titleSimilarity(a.title, b.title) > 0.6;
    return sameCategory && closeInTime && similar;
  },
  titleSimilarity(a, b) {
    if (a === b) return 1;
    const bigrams = s => { const set = new Set(); for (let i = 0; i < s.length - 1; i++) set.add(s.slice(i, i + 2)); return set; };
    const A = bigrams(a), B = bigrams(b);
    if (A.size === 0 || B.size === 0) return 0;
    let inter = 0; for (const g of A) if (B.has(g)) inter++;
    const union = A.size + B.size - inter;
    return union === 0 ? 0 : inter / union;
  }
};

// モック生成(TDnetが失敗した場合の「サンプルで見た目を確認したい」用途に、企業詳細/ニュース画面で
// 明示的な操作からのみ使う。自動フォールバックはしない = ニュースは常に正直な失敗表示を優先する)
function generateMockNews(companyId, companyName, count) {
  const rng = mulberry32(((hashString(companyId) * 31 + count) >>> 0));
  const now = Date.now();
  const items = [];
  for (let i = 0; i < count; i++) {
    const t = NEWS_TEMPLATES[Math.floor(rng() * NEWS_TEMPLATES.length)];
    const minutesAgo = (i * 47 + randInt(rng, 0, 29)) * 60000;
    const publishedAt = now - minutesAgo;
    items.push({
      id: `${companyId}-mock-${i}-${t.category}`, companyId, companyName,
      title: `${companyName}、${t.title}`, summary: t.summary(companyName),
      source: { sourceName: t.sourceName, sourceUrl: `https://example.com/news/${companyId}/${i}`, publishedAtEpochMillis: publishedAt, collectedAtEpochMillis: publishedAt + 60000 },
      category: t.category, importance: t.importance, stockImpactNote: t.impactNote,
      relatedCount: randInt(rng, 0, 3), contentHash: ""
    });
  }
  return items;
}

// ---------------------------------------------------------------------------
// CorporateNumberProvider: モック + 実データ(国税庁法人番号Web-API) + 自動切り替え
// ---------------------------------------------------------------------------
const MockCorporateNumberProvider = {
  async findCandidates(companyName) {
    await sleep(200);
    return SAMPLE_COMPANIES
      .filter(c => c.companyName.includes(companyName) || companyName.includes(c.companyName))
      .map(c => ({ corporateNumber: c.corporateNumber || "不明", officialName: c.officialName, location: "東京都(モックデータ)", stockCode: c.stockCode, exchange: c.exchange }));
  }
};
const HoujinBangouProvider = {
  async findCandidates(companyName, appId) {
    const url = `https://api.houjin-bangou.nta.go.jp/4/name?id=${encodeURIComponent(appId)}&name=${encodeURIComponent(companyName)}&type=12&mode=2`;
    const res = await fetch(url);
    if (!res.ok) throw new Error(`法人番号Web-API returned HTTP ${res.status}`);
    const xmlText = await res.text();
    const doc = new DOMParser().parseFromString(xmlText, "application/xml");
    if (doc.querySelector("parsererror")) throw new Error("法人番号Web-APIの応答を解析できませんでした");
    const nodes = Array.from(doc.getElementsByTagName("corporation")).slice(0, 10);
    const text = (el, tag) => { const n = el.getElementsByTagName(tag)[0]; return n && n.textContent ? n.textContent.trim() : ""; };
    return nodes.map(el => {
      const corporateNumber = text(el, "corporateNumber"); const officialName = text(el, "name");
      if (!corporateNumber || !officialName) return null;
      const location = [text(el, "prefectureName"), text(el, "cityName"), text(el, "streetNumber")].filter(Boolean).join("");
      return { corporateNumber, officialName, location, stockCode: null, exchange: "UNKNOWN" };
    }).filter(Boolean);
  }
};
const CorporateNumberRepository = {
  async findCandidates(companyName) {
    const appId = Storage.getSettings().houjinBangouAppId;
    if (appId) {
      try { return await HoujinBangouProvider.findCandidates(companyName, appId); }
      catch (e) { /* 実データ取得に失敗 → サンプルデータへフォールバック */ }
    }
    return MockCorporateNumberProvider.findCandidates(companyName);
  }
};

// ---------------------------------------------------------------------------
// Router / UI
// ---------------------------------------------------------------------------
const screenEl = document.getElementById("screen");
const topbarTitle = document.getElementById("topbarTitle");
const topbarBack = document.getElementById("topbarBack");
const topbarSearch = document.getElementById("topbarSearch");
const fabEl = document.getElementById("fab");
const tabbarEl = document.getElementById("tabbar");

const TAB_ROUTES = ["home", "watchlist", "trending", "news", "settings"];
let currentAbortToken = 0; // 画面遷移後に古い非同期処理の結果を反映しないためのトークン

function parseHash() {
  const raw = (location.hash || "#home").slice(1);
  const [route, param] = raw.split("/");
  return { route: route || "home", param: param ? decodeURIComponent(param) : null };
}
function navigate(hash) { location.hash = hash; }

function setTopbar({ title, showBack, showSearch }) {
  topbarTitle.textContent = title;
  topbarBack.hidden = !showBack;
  topbarSearch.hidden = !showSearch;
}
function setTabActive(route) {
  tabbarEl.querySelectorAll(".tab-btn").forEach(btn => btn.classList.toggle("active", btn.dataset.route === route));
}
function setFab(visible) { fabEl.hidden = !visible; }

function showToast(message) {
  const t = document.createElement("div");
  t.className = "toast";
  t.textContent = message;
  document.body.appendChild(t);
  setTimeout(() => t.remove(), 2300);
}

async function route() {
  const token = ++currentAbortToken;
  const { route: r, param } = parseHash();
  setTabActive(TAB_ROUTES.includes(r) ? r : null);
  screenEl.scrollTop = 0;

  if (r === "home") { setTopbar({ title: "投資情報モニター", showBack: false, showSearch: true }); setFab(false); await renderHome(token); }
  else if (r === "watchlist") { setTopbar({ title: "監視企業", showBack: false, showSearch: false }); setFab(true); await renderWatchlist(token); }
  else if (r === "trending") { setTopbar({ title: "注目企業", showBack: false, showSearch: false }); setFab(false); await renderTrending(token); }
  else if (r === "news") { setTopbar({ title: "ニュース", showBack: false, showSearch: false }); setFab(false); await renderNews(token); }
  else if (r === "settings") { setTopbar({ title: "設定", showBack: false, showSearch: false }); setFab(false); await renderSettings(token); }
  else if (r === "search") { setTopbar({ title: "企業を検索", showBack: true, showSearch: false }); setFab(false); await renderSearch(token); }
  else if (r === "company") { setTopbar({ title: "企業詳細", showBack: true, showSearch: false }); setFab(false); await renderCompanyDetail(param, token); }
  else { navigate("#home"); }
}

window.addEventListener("hashchange", route);
tabbarEl.addEventListener("click", e => {
  const btn = e.target.closest(".tab-btn");
  if (btn) navigate(`#${btn.dataset.route}`);
});
topbarBack.addEventListener("click", () => history.back());
topbarSearch.addEventListener("click", () => navigate("#search"));
fabEl.addEventListener("click", () => navigate("#search"));

function loadingHtml() { return `<div class="loading-center">読み込み中…</div>`; }
function disclaimerHtml() {
  return `<div class="disclaimer">
    <svg width="16" height="16" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10" fill="none" stroke="currentColor" stroke-width="1.6"/><path d="M12 8v5M12 16.2v.1" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
    <span>本アプリは投資情報の収集・整理・分析支援ツールであり、投資助言サービスではありません。「注目企業」等の表示も投資推奨ではありません。投資判断は必ずご自身の責任で行ってください。</span>
  </div>`;
}
function changeBadgeHtml(change, changePercent) {
  const cls = change > 0 ? "up" : change < 0 ? "down" : "flat";
  const sign = change > 0 ? "+" : "";
  return `<span class="change-badge ${cls}">${sign}${change.toFixed(1)} (${sign}${changePercent.toFixed(2)}%)</span>`;
}
function freshnessHtml(asOfEpochMillis, isStale) {
  const label = relativeTimeLabel(asOfEpochMillis);
  return isStale
    ? `<span class="freshness stale">最終更新: ${label} (データを更新できません)</span>`
    : `<span class="freshness">最終更新: ${label}</span>`;
}
function starsHtml(importanceKey) {
  const stars = IMPORTANCE[importanceKey]?.stars ?? 2;
  return `<span class="stars">${"★".repeat(stars)}${"☆".repeat(5 - stars)}</span>`;
}
function newsItemHtml(news) {
  return `<div class="card news-item" data-nav="company/${news.companyId}">
    <div class="head">
      <span class="category-chip">${escapeHtml(NEWS_CATEGORY_LABELS[news.category] || news.category)}</span>
      ${starsHtml(news.importance)}
      ${isNewsNew(news.source.publishedAtEpochMillis) ? '<span class="new-badge">NEW</span>' : ""}
    </div>
    <div class="title">${escapeHtml(news.title)}</div>
    <div class="company">${escapeHtml(news.companyName)}</div>
    <div class="meta">
      <span>${escapeHtml(news.source.sourceName)}</span>
      <span>${relativeTimeLabel(news.source.publishedAtEpochMillis)}</span>
      ${news.relatedCount > 0 ? `<span class="related">関連ニュース ${news.relatedCount}件</span>` : ""}
      ${news.source.sourceUrl ? `<a href="${escapeHtml(news.source.sourceUrl)}" target="_blank" rel="noopener" class="related" onclick="event.stopPropagation()">原文(PDF) →</a>` : ""}
    </div>
  </div>`;
}
function bindNav(container) {
  container.querySelectorAll("[data-nav]").forEach(el => {
    el.addEventListener("click", () => navigate(`#${el.dataset.nav}`));
  });
}

// ---------------- ホーム ----------------
async function renderHome(token) {
  screenEl.innerHTML = loadingHtml();
  try {
    const [news, trending] = await Promise.all([
      NewsRepository.getLatestNews(10).then(list => list.sort((a, b) => IMPORTANCE[b.importance].stars - IMPORTANCE[a.importance].stars).slice(0, 5)).catch(() => null),
      MarketRepository.getTrendingCompanies().then(list => list.sort((a, b) => b.scores.totalScore - a.scores.totalScore).slice(0, 6))
    ]);
    if (token !== currentAbortToken) return;
    // companyById()だけだと監視対象10社以外(companies.jsonの全銘柄)が一覧から消えてしまうため、
    // resolveCompanyLite()で簡易プロフィールにフォールバックする。
    const watchlist = await Promise.all(Storage.getWatchlist().slice(0, 5).map(w => resolveCompanyLite(w.companyId)));
    const newsFailed = news === null;

    const newsAt = TdnetProvider.fetchedAtEpochMillis();
    let html = `<div class="section-header"><p style="margin-top:0">最終更新: ${relativeTimeLabel(newsAt || Date.now())}</p></div>`;
    if (newsFailed) {
      html += `<div class="error-banner"><strong>ニュースを取得できませんでした</strong><span>ニュースデータの読み込みに失敗しました。しばらくしてページを再読み込みしてください。</span></div>`;
    }
    html += `<div class="section-header"><h2>重要ニュース</h2><p>今日、注目すべき動き</p></div>`;
    html += (!news || news.length === 0) ? `<div class="empty-state">${newsFailed ? "" : "まだニュースがありません。企業を登録すると表示されます。"}</div>`
      : news.map(newsItemHtml).join("") + `<div class="card" style="box-shadow:none;color:var(--interactive);font-weight:600" data-nav="news">すべてのニュースを見る →</div>`;

    html += `<div class="section-header"><h2>監視企業</h2><p>登録した企業の最新状況</p></div>`;
    html += watchlist.length === 0
      ? `<div class="empty-state">まだ監視企業がありません。右上の検索から企業を登録しましょう。</div>`
      : watchlist.map(c => `<div class="card" data-nav="company/${c.companyId}"><div class="card-title">${escapeHtml(c.companyName)}</div><div class="card-sub">${escapeHtml(c.stockCode || "-")}</div></div>`).join("");

    html += `<div class="section-header"><h2>市場の注目企業</h2><p>客観的な指標から抽出(投資推奨ではありません)</p></div>`;
    html += `<div class="trend-scroll">${trending.map(t => `
      <div class="card trend-card" data-nav="company/${t.company.companyId}">
        <div class="card-title" style="white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${escapeHtml(t.company.companyName)}</div>
        <div class="card-sub">¥${formatYen(t.quote.price)}</div>
        ${changeBadgeHtml(t.quote.change, t.quote.changePercent)}
        <div class="badge-total">総合${t.scores.totalScore}</div>
      </div>`).join("")}</div>`;
    html += `<div class="card" style="box-shadow:none;color:var(--interactive);font-weight:600" data-nav="trending">注目企業をもっと見る →</div>`;
    html += disclaimerHtml();

    screenEl.innerHTML = html;
    bindNav(screenEl);
  } catch (e) {
    if (token !== currentAbortToken) return;
    screenEl.innerHTML = `<div class="error-banner"><strong>現在データを更新できません。</strong><span>しばらくしてから再度お試しください。</span></div>`;
  }
}

// ---------------- 監視企業一覧 ----------------
async function renderWatchlist(token) {
  const entries = Storage.getWatchlist();
  if (entries.length === 0) {
    screenEl.innerHTML = `<div class="empty-state">監視企業がまだ登録されていません。右下の＋から企業を追加してください。</div>`;
    return;
  }
  screenEl.innerHTML = loadingHtml();
  const rows = await Promise.all(entries.map(async entry => {
    // companyById()だけだと監視対象10社以外(companies.jsonの全銘柄)が一覧から消えてしまうため、
    // resolveCompanyLite()で簡易プロフィールにフォールバックする。
    const company = await resolveCompanyLite(entry.companyId);
    const quote = await MarketRepository.getQuote(company.companyId).then(withChange).catch(() => null);
    let latestNewsTitle = null, newsUnavailable = false, hasNew = false;
    try {
      const news = await NewsRepository.getNewsForCompany(company.companyId, company.companyName);
      const top = news.sort((a, b) => b.source.publishedAtEpochMillis - a.source.publishedAtEpochMillis)[0];
      if (top) { latestNewsTitle = top.title; hasNew = isNewsNew(top.source.publishedAtEpochMillis); }
    } catch (e) { newsUnavailable = true; }
    return { company, quote, latestNewsTitle, hasNew, newsUnavailable };
  }));
  if (token !== currentAbortToken) return;

  const html = rows.filter(Boolean).map(row => `
    <div class="card watch-row" data-nav="company/${row.company.companyId}">
      <div class="card-row">
        <span class="card-title">${escapeHtml(row.company.companyName)}${row.hasNew ? '<span class="new-tag">NEW</span>' : ""}</span>
        ${row.quote ? changeBadgeHtml(row.quote.change, row.quote.changePercent) : ""}
      </div>
      ${row.quote ? `<div class="price">¥${formatYen(row.quote.price)}</div>` : ""}
      ${row.latestNewsTitle ? `<div class="headline">${escapeHtml(row.latestNewsTitle)}</div>` : row.newsUnavailable ? `<div class="headline error">ニュースを取得できませんでした</div>` : ""}
    </div>`).join("");
  screenEl.innerHTML = html;
  bindNav(screenEl);
}

// ---------------- 企業検索 ----------------
async function renderSearch() {
  screenEl.innerHTML = `
    <div class="search-input-wrap"><input class="search-input" id="searchInput" placeholder="企業名・証券コードを入力" autofocus></div>
    <div id="searchResults"></div>`;
  const input = document.getElementById("searchInput");
  const resultsEl = document.getElementById("searchResults");
  let debounceTimer = null;

  const showResults = async (query) => {
    if (!query.trim()) { resultsEl.innerHTML = ""; return; }
    resultsEl.innerHTML = loadingHtml();
    const results = await CompanyProvider.searchCompanies(query);
    if (input.value !== query) return; // 入力が変わっていたら破棄
    // companyById()は監視対象10社にしか使えないため、検索結果自体(companies.json全銘柄も含む)を
    // 保持しておき、クリック時はそこから引く。
    const resultsById = new Map(results.map(c => [c.companyId, c]));
    resultsEl.innerHTML = results.length === 0
      ? `<div class="empty-state">該当する企業が見つかりませんでした。</div>`
      : results.map(c => `<div class="card" data-pick="${c.companyId}">
          <div class="card-title">${escapeHtml(c.companyName)}</div>
          <div class="card-sub">${escapeHtml(c.officialName)} ・ ${escapeHtml(c.stockCode || "コード不明")}${c.isFullProfile === false ? " ・ 簡易プロフィール" : ""}</div>
        </div>`).join("");
    resultsEl.querySelectorAll("[data-pick]").forEach(el => {
      el.addEventListener("click", () => showConfirmation(resultsById.get(el.dataset.pick)));
    });
  };
  input.addEventListener("input", () => {
    clearTimeout(debounceTimer);
    const q = input.value;
    debounceTimer = setTimeout(() => showResults(q), 250);
  });

  const showConfirmation = async (company) => {
    resultsEl.innerHTML = `
      <div class="card">
        <div class="card-title" style="font-size:16px;margin-bottom:8px">企業情報の確認</div>
        <div class="info-row"><span class="label">企業名</span><span class="value">${escapeHtml(company.companyName)}</span></div>
        <div class="info-row"><span class="label">正式名称</span><span class="value">${escapeHtml(company.officialName)}</span></div>
        <div class="info-row"><span class="label">証券コード</span><span class="value">${escapeHtml(company.stockCode || "-")}</span></div>
        <div class="info-row"><span class="label">市場</span><span class="value">${escapeHtml(EXCHANGE_LABELS[company.exchange])}</span></div>
        <div class="info-row"><span class="label">業種</span><span class="value">${escapeHtml(company.industry)}</span></div>
        ${company.corporateNumber ? `<div class="info-row"><span class="label">法人番号</span><span class="value">${escapeHtml(company.corporateNumber)}</span></div>` : ""}
        <div class="freshness" id="candidateStatus" style="display:block;margin-top:6px">法人番号の候補を確認中...</div>
        <button class="btn-primary" id="confirmBtn">この企業を監視する</button>
      </div>`;
    CorporateNumberRepository.findCandidates(company.companyName).then(candidates => {
      const el = document.getElementById("candidateStatus");
      if (el) el.textContent = `法人番号候補: ${candidates.length}件見つかりました`;
    }).catch(() => {
      const el = document.getElementById("candidateStatus");
      if (el) el.textContent = "法人番号候補を確認できませんでした";
    });
    document.getElementById("confirmBtn").addEventListener("click", () => {
      Storage.addToWatchlist(company.companyId);
      showToast(`${company.companyName}を監視リストに追加しました`);
      history.back();
    });
  };
}

// ---------------- 企業詳細 ----------------
let selectedChartRange = "M1";
async function renderCompanyDetail(companyId, token) {
  const company = companyById(companyId);
  screenEl.innerHTML = loadingHtml();
  selectedChartRange = "M1";

  // The news feed is now market-wide (all ~4,000 TSE-listed companies), but this app only holds
  // full profile data (financials, chart, related companies) for its 10 tracked sample companies.
  // Rather than a dead-end "企業情報が見つかりません" for everything else, fall back to a lighter
  // view built from what's actually available for any real stock code: a live/mock quote and
  // whatever TDnet news mentions it (spec section 40 - never fake a fuller profile than we have).
  if (!company) {
    await renderUntrackedCompanyDetail(companyId, token);
    return;
  }
  topbarTitle.textContent = company.companyName;

  const [quote, history_, news, relations] = await Promise.all([
    MarketRepository.getQuote(companyId).then(withChange).catch(() => null),
    MarketRepository.getHistory(companyId, selectedChartRange).catch(() => []),
    NewsRepository.getNewsForCompany(companyId, company.companyName).catch(() => null),
    CompanyProvider.getRelatedCompanies(companyId)
  ]);
  if (token !== currentAbortToken) return;
  renderCompanyDetailBody(company, quote, history_, news, relations);
}
async function renderUntrackedCompanyDetail(companyId, token) {
  const [quote, data, master] = await Promise.all([
    MarketRepository.getQuote(companyId).then(withChange).catch(() => null),
    TdnetProvider.loadData().catch(() => null),
    CompanyMasterProvider.getByCode(companyId).catch(() => null)
  ]);
  if (token !== currentAbortToken) return;
  // Recover the company name from the全銘柄マスタ(companies.json)があればそちらを優先し、
  // 無ければ「recent」ニュースフィードから拾う(ニュースカードのタップ経由で来た場合の保険)。
  const fromRecent = data ? (data.recent || [])
    .map(entry => TdnetProvider.toNewsItem(entry && entry.Tdnet, null))
    .filter(n => n && n.companyId === companyId) : [];
  const news = NewsRepository.deduplicate(fromRecent)
    .sort((a, b) => b.source.publishedAtEpochMillis - a.source.publishedAtEpochMillis);
  const companyName = master?.name || news[0]?.companyName || companyId;
  topbarTitle.textContent = companyName;
  const isWatched = Storage.isWatched(companyId);

  const html = `<div class="quote-row">
      ${quote ? `<div><div class="quote-price">¥${formatYen(quote.price)}</div>${freshnessHtml(quote.asOfEpochMillis, quote.isStale)}</div>${changeBadgeHtml(quote.change, quote.changePercent)}`
              : `<div class="freshness">株価データを取得できませんでした</div>`}
    </div>
    <div class="card">
      <div class="card-title" style="margin-bottom:8px">${escapeHtml(companyName)}</div>
      <div class="info-row"><span class="label">証券コード</span><span class="value">${escapeHtml(companyId)}</span></div>
      ${master ? `<div class="info-row"><span class="label">市場</span><span class="value">${escapeHtml(master.market || "-")}</span></div>
      <div class="info-row"><span class="label">業種</span><span class="value">${escapeHtml(master.sector33 || "-")}</span></div>` : ""}
      <div class="empty-state" style="padding:10px 0 0;text-align:left">この企業は監視対象10社(詳細プロフィールあり)には含まれていないため、財務情報(PER・PBR等)や関連企業は表示できません。株価・ニュースはご覧いただけます。</div>
    </div>
    <button class="btn-outline" id="watchToggleBtnLite" style="margin:12px 16px">${isWatched ? "★ 監視中(タップで解除)" : "☆ この企業を監視する"}</button>
    <div class="section-header"><h2>ニュース</h2></div>
    ${news.length === 0 ? `<div class="empty-state">この企業のニュースが見つかりませんでした。</div>` : news.map(newsItemHtml).join("")}
    ${disclaimerHtml()}`;
  screenEl.innerHTML = html;
  bindNav(screenEl);
  document.getElementById("watchToggleBtnLite").addEventListener("click", () => {
    if (Storage.isWatched(companyId)) { Storage.removeFromWatchlist(companyId); showToast("監視を解除しました"); }
    else { Storage.addToWatchlist(companyId); showToast("監視リストに追加しました"); }
    renderUntrackedCompanyDetail(companyId, token);
  });
}
function renderCompanyDetailBody(company, quote, history_, news, relations) {
  const isWatched = Storage.isWatched(company.companyId);
  let html = `<div class="quote-row">
      ${quote ? `<div><div class="quote-price">¥${formatYen(quote.price)}</div>${freshnessHtml(quote.asOfEpochMillis, quote.isStale)}</div>${changeBadgeHtml(quote.change, quote.changePercent)}`
              : `<div class="freshness">株価データを取得できませんでした</div>`}
    </div>
    <div class="chip-row" id="rangeChips">${CHART_RANGES.map(r => `<button class="chip ${r.key === selectedChartRange ? "selected" : ""}" data-range="${r.key}">${r.label}</button>`).join("")}</div>
    <div class="card chart-card"><canvas id="priceChart" width="600" height="300"></canvas></div>
    <div class="card">
      <div class="card-title" style="margin-bottom:8px">企業概要</div>
      <div class="info-row"><span class="label">証券コード</span><span class="value">${escapeHtml(company.stockCode || "-")}</span></div>
      <div class="info-row"><span class="label">市場</span><span class="value">${escapeHtml(EXCHANGE_LABELS[company.exchange])}</span></div>
      <div class="info-row"><span class="label">業種</span><span class="value">${escapeHtml(company.industry)}</span></div>
      <div class="info-row"><span class="label">時価総額</span><span class="value">${company.marketCapBillionYen != null ? formatYen(company.marketCapBillionYen) + "億円" : "-"}</span></div>
      <div class="info-row"><span class="label">PER</span><span class="value">${company.per != null ? company.per + "倍" : "-"}</span></div>
      <div class="info-row"><span class="label">PBR</span><span class="value">${company.pbr != null ? company.pbr + "倍" : "-"}</span></div>
      <div class="info-row"><span class="label">ROE</span><span class="value">${company.roe != null ? company.roe + "%" : "-"}</span></div>
      <div class="info-row"><span class="label">売上高</span><span class="value">${company.revenueBillionYen != null ? formatYen(company.revenueBillionYen) + "億円" : "-"}</span></div>
      <div class="info-row"><span class="label">営業利益</span><span class="value">${company.operatingIncomeBillionYen != null ? formatYen(company.operatingIncomeBillionYen) + "億円" : "-"}</span></div>
    </div>
    ${relations.length > 0 ? `<div class="section-header"><h2>関連企業</h2></div><div class="card">${relations.map(r => `<div>・${escapeHtml(r.toCompany.companyName)}(${escapeHtml(RELATION_LABELS[r.relationType] || r.relationType)})</div>`).join("")}</div>` : ""}
    <button class="btn-outline" id="watchToggleBtn" style="margin:12px 16px">${isWatched ? "★ 監視中(タップで解除)" : "☆ この企業を監視する"}</button>
    <div class="section-header"><h2>ニュース</h2></div>
    ${news === null ? `<div class="error-banner"><strong>ニュースを取得できませんでした</strong><span>ニュースデータの読み込みに失敗しました。しばらくしてページを再読み込みしてください。</span></div>`
      : news.length === 0 ? `<div class="empty-state">この企業に関するニュースはまだありません。</div>`
      : news.sort((a, b) => b.source.publishedAtEpochMillis - a.source.publishedAtEpochMillis).map(newsItemHtml).join("")}
    ${disclaimerHtml()}`;
  screenEl.innerHTML = html;
  bindNav(screenEl);

  drawPriceChart(document.getElementById("priceChart"), history_);
  document.querySelectorAll("#rangeChips .chip").forEach(chip => {
    chip.addEventListener("click", async () => {
      selectedChartRange = chip.dataset.range;
      document.querySelectorAll("#rangeChips .chip").forEach(c => c.classList.toggle("selected", c === chip));
      const pts = await MarketRepository.getHistory(company.companyId, selectedChartRange).catch(() => []);
      drawPriceChart(document.getElementById("priceChart"), pts);
    });
  });
  document.getElementById("watchToggleBtn").addEventListener("click", () => {
    if (Storage.isWatched(company.companyId)) { Storage.removeFromWatchlist(company.companyId); showToast("監視を解除しました"); }
    else { Storage.addToWatchlist(company.companyId); showToast("監視リストに追加しました"); }
    renderCompanyDetailBody(company, quote, history_, news, relations);
  });
}

// 依存ライブラリなしのシンプルな折れ線チャート(Android版 PriceLineChart と同じ考え方)
function drawPriceChart(canvas, points) {
  if (!canvas) return;
  const dpr = window.devicePixelRatio || 1;
  const cssW = canvas.clientWidth || 320, cssH = 150;
  canvas.width = cssW * dpr; canvas.height = cssH * dpr;
  const ctx = canvas.getContext("2d");
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.clearRect(0, 0, cssW, cssH);

  if (!points || points.length < 2) {
    ctx.fillStyle = getComputedStyle(document.body).getPropertyValue("--on-surface-variant") || "#888";
    ctx.font = "13px sans-serif"; ctx.textAlign = "center";
    ctx.fillText("チャートデータがありません", cssW / 2, cssH / 2);
    return;
  }
  const closes = points.map(p => p.close);
  const min = Math.min(...closes), max = Math.max(...closes);
  const range = (max - min) || 1;
  const up = closes[closes.length - 1] >= closes[0];
  const color = up ? "#D32F2F" : "#2E7D32"; // 日本の相場慣習: 上昇=赤 / 下落=緑

  const stepX = cssW / (points.length - 1);
  ctx.beginPath();
  points.forEach((p, i) => {
    const x = i * stepX;
    const y = cssH - ((p.close - min) / range) * cssH;
    if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
  });
  ctx.strokeStyle = color; ctx.lineWidth = 2.4; ctx.lineJoin = "round"; ctx.lineCap = "round";
  ctx.stroke();

  const baselineY = cssH - ((points[0].close - min) / range) * cssH;
  ctx.beginPath(); ctx.moveTo(0, baselineY); ctx.lineTo(cssW, baselineY);
  ctx.strokeStyle = color + "40"; ctx.lineWidth = 1; ctx.stroke();
}

// ---------------- 注目企業 ----------------
let selectedTrendCategory = "SURGING";
async function renderTrending(token) {
  screenEl.innerHTML = loadingHtml();
  const [companies, ipos] = await Promise.all([MarketRepository.getTrendingCompanies(), MarketRepository.getIpoCompanies()]);
  if (token !== currentAbortToken) return;
  renderTrendingBody(companies, ipos);
}
function renderTrendingBody(companies, ipos) {
  let html = `<div class="chip-row" id="trendChips">${TREND_CATEGORY_ORDER.map(cat => `<button class="chip ${cat === selectedTrendCategory ? "selected" : ""}" data-cat="${cat}">${TREND_CATEGORY_LABELS[cat]}</button>`).join("")}</div>`;
  if (selectedTrendCategory === "LONG_TERM_WATCH") {
    html += `<div style="padding:0 16px 8px;font-size:12px;color:var(--on-surface-variant)">「中長期指標では注目度が高い一方、現在株価が調整している企業」を表示しています。買い時を示すものではありません。</div>`;
  }
  if (selectedTrendCategory === "NEW_IPO") {
    html += ipos.length === 0 ? `<div class="empty-state">新規上場企業のデータがありません。</div>`
      : ipos.map(ipo => `<div class="card" data-nav="company/${ipo.company.companyId}">
          <div class="card-title">${escapeHtml(ipo.company.companyName)}</div>
          <div>市場: ${escapeHtml(EXCHANGE_LABELS[ipo.market])}</div>
          <div>公募価格: ¥${formatYen(ipo.offeringPrice)}</div>
          <div>現在値: ¥${formatYen(ipo.currentPrice)} (公募比 ${(((ipo.currentPrice - ipo.offeringPrice) / ipo.offeringPrice) * 100).toFixed(1)}%)</div>
        </div>`).join("");
  } else {
    const filtered = companies.filter(t => t.categories.includes(selectedTrendCategory)).sort((a, b) => b.scores.totalScore - a.scores.totalScore);
    html += filtered.length === 0 ? `<div class="empty-state">該当する企業がありません。</div>`
      : filtered.map(t => `<div class="card" data-nav="company/${t.company.companyId}">
          <div class="card-row"><span class="card-title">${escapeHtml(t.company.companyName)}</span>${changeBadgeHtml(t.quote.change, t.quote.changePercent)}</div>
          <div class="price">¥${formatYen(t.quote.price)}</div>
          <div class="trend-score">総合注目度 ${t.scores.totalScore} / 100 (モメンタム${t.scores.momentumScore}・成長${t.scores.growthScore}・出来高${t.scores.volumeScore})</div>
        </div>`).join("") + disclaimerHtml();
  }
  screenEl.innerHTML = html;
  bindNav(screenEl);
  document.querySelectorAll("#trendChips .chip").forEach(chip => {
    chip.addEventListener("click", () => { selectedTrendCategory = chip.dataset.cat; renderTrendingBody(companies, ipos); });
  });
}

// ---------------- ニュース ----------------
let selectedNewsCategory = null, selectedNewsSort = "NEWEST";
async function renderNews(token) {
  screenEl.innerHTML = loadingHtml();
  const news = await NewsRepository.getLatestNews(100).catch(() => null);
  if (token !== currentAbortToken) return;
  renderNewsBody(news);
}
function renderNewsBody(allNews) {
  const newsAt = TdnetProvider.fetchedAtEpochMillis();
  let html = `<p class="freshness" style="padding:0 16px">${newsAt ? `ニュース最終更新: ${relativeTimeLabel(newsAt)}(約20分ごとに自動更新)` : ""}</p>
    <div class="chip-row" id="newsCatChips">
      <button class="chip ${selectedNewsCategory === null ? "selected" : ""}" data-cat="">すべて</button>
      ${Object.keys(NEWS_CATEGORY_LABELS).map(cat => `<button class="chip ${cat === selectedNewsCategory ? "selected" : ""}" data-cat="${cat}">${NEWS_CATEGORY_LABELS[cat]}</button>`).join("")}
    </div>
    <div class="chip-row" id="newsSortChips" style="padding-top:0">
      <button class="chip ${selectedNewsSort === "NEWEST" ? "selected" : ""}" data-sort="NEWEST">新着順</button>
      <button class="chip ${selectedNewsSort === "IMPORTANCE" ? "selected" : ""}" data-sort="IMPORTANCE">重要度順</button>
    </div>`;
  if (allNews === null) {
    html += `<div class="error-banner"><strong>ニュースを取得できませんでした。</strong><span>ニュースデータの読み込みに失敗しました。しばらくしてページを再読み込みしてください。</span></div>`;
  } else {
    let filtered = selectedNewsCategory ? allNews.filter(n => n.category === selectedNewsCategory) : allNews.slice();
    filtered = selectedNewsSort === "NEWEST"
      ? filtered.sort((a, b) => b.source.publishedAtEpochMillis - a.source.publishedAtEpochMillis)
      : filtered.sort((a, b) => IMPORTANCE[b.importance].stars - IMPORTANCE[a.importance].stars);
    html += filtered.length === 0 ? `<div class="empty-state">該当するニュースがありません。</div>` : filtered.map(newsItemHtml).join("");
  }
  screenEl.innerHTML = html;
  bindNav(screenEl);
  document.querySelectorAll("#newsCatChips .chip").forEach(chip => chip.addEventListener("click", () => { selectedNewsCategory = chip.dataset.cat || null; renderNewsBody(allNews); }));
  document.querySelectorAll("#newsSortChips .chip").forEach(chip => chip.addEventListener("click", () => { selectedNewsSort = chip.dataset.sort; renderNewsBody(allNews); }));
}

// ---------------- 設定 ----------------
async function renderSettings(token) {
  const s = Storage.getSettings();
  // 株価・企業マスタはもう「自分のブラウザに登録したキー」では動かない(CORSで常にブロックされる
  // ことが確認済みのため)。サイト側がGitHub Actionsで毎日取得する共有データの状態を、正直にここに
  // 表示する(取得前や未設定の間は「未取得」であることが分かるようにする)。
  const [companiesData, quotesData] = await Promise.all([
    CompanyMasterProvider.loadData().catch(() => null),
    QuoteSnapshotProvider.loadData().catch(() => null)
  ]);
  if (token !== undefined && token !== currentAbortToken) return;
  const companiesCount = companiesData?.companies?.length ?? null;
  const quotesAsOfDate = quotesData?.asOfDate ?? null;

  screenEl.innerHTML = `
    <div class="settings-section">
      <h3>通知</h3>
      <div class="setting-switch-row"><span>通知を受け取る</span>
        <label class="switch"><input type="checkbox" id="notifEnabled" ${s.notificationsEnabled ? "checked" : ""}><span class="track"></span><span class="thumb"></span></label>
      </div>
      <p style="font-size:11.5px;color:var(--on-surface-variant);margin:0 0 8px">Web版はブラウザのプッシュ通知に対応していないため、この設定は今後の拡張用です。</p>
    </div>
    <div class="settings-section">
      <h3>株価・上場銘柄データ</h3>
      <p style="font-size:12px;color:var(--on-surface-variant);margin:0 0 8px;line-height:1.6">株価データを提供するJ-Quants APIはブラウザから直接呼び出すとブロックされる(CORS)ため、個人でAPIキーを登録する方式は廃止しました。代わりに、サイトを管理している開発者側のキーでGitHub Actionsが毎日データを取得し、利用者全員に同じ内容を表示しています。無料プランの制約上、株価は「取得できた直近の営業日」時点のもので、常に約3か月前後遅れた参考値です(発注価格として使わないでください)。</p>
      <div class="api-field">
        <div class="field-title">対応している上場銘柄数</div>
        <div class="field-status ${companiesCount ? "registered" : "unset"}">${companiesCount ? `${companiesCount}社(東証上場銘柄)を検索できます` : "未取得(監視対象10社のサンプルのみ表示中)"}</div>
      </div>
      <div class="api-field">
        <div class="field-title">株価データの基準日</div>
        <div class="field-status ${quotesAsOfDate ? "registered" : "unset"}">${quotesAsOfDate ? `${quotesAsOfDate} 時点のデータ` : "未取得(サンプル価格を表示中)"}</div>
      </div>
    </div>
    <div class="settings-section">
      <h3>法人番号検索(任意・現在は動作しません)</h3>
      <p style="font-size:12px;color:var(--on-surface-variant);margin:0 0 8px;line-height:1.6">国税庁「法人番号システムWeb-API」も同じくブラウザからの直接呼び出しがブロックされるため、下にアプリケーションIDを登録しても実際には使われず、常にサンプルの候補が表示されます。株価・企業データと同様の仕組みに移行するかは今後検討します。</p>
      <div class="api-field" id="houjinField">
        <div class="field-title">国税庁 アプリケーションID</div>
        <div class="field-desc">登録方法: invoice-web-api@nta.go.jp 宛にメールで申請(無料)。</div>
        <div class="field-status ${s.houjinBangouAppId ? "registered" : "unset"}">状態: ${s.houjinBangouAppId ? "登録済み(ただし未使用)" : "未登録"}</div>
        <div class="api-input-row"><input type="password" id="houjinInput" placeholder="IDを貼り付け" value="${escapeHtml(s.houjinBangouAppId || "")}"><button data-save="houjinBangouAppId" data-input="houjinInput">保存</button></div>
      </div>
    </div>
    <div class="settings-section">
      <h3>このアプリについて</h3>
      <div class="about-block">
        投資情報モニター Web版(Phase 1)<br>
        本アプリは投資助言サービスではありません。表示される情報は投資情報の収集・整理・分析を支援するものであり、投資判断は必ずご自身の責任で行ってください。<br><br>
        このWeb版はブラウザ内だけで動作し、監視企業の登録内容はこの端末のブラウザにのみ保存されます(他の端末やAndroid版とは同期されません)。iPhoneでは、Safariの共有ボタン→「ホーム画面に追加」でアプリのように使えます。
      </div>
    </div>`;

  document.getElementById("notifEnabled").addEventListener("change", e => Storage.setSetting("notificationsEnabled", e.target.checked));
  screenEl.querySelectorAll("[data-save]").forEach(btn => {
    btn.addEventListener("click", () => {
      const key = btn.dataset.save;
      const input = document.getElementById(btn.dataset.input);
      Storage.setSetting(key, input.value.trim() || null);
      showToast("保存しました");
      renderSettings(token);
    });
  });
}

// ---------------------------------------------------------------------------
// 起動
// ---------------------------------------------------------------------------
route();
