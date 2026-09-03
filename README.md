# 投資情報・企業監視アプリ (Investment Monitor)

Android専用の投資情報・企業監視アプリです。監視したい企業を登録すると、その企業に関する
ニュース・株価・決算情報などを一つのアプリで確認できます。

> **重要な注意事項**
> 本アプリは投資助言サービスではありません。「注目企業」「おすすめ企業」等の表示も投資推奨ではなく、
> 客観的な指標から抽出した参考情報です。投資判断は必ずご自身の責任で行ってください。

## 現在のステータス: Phase 2 着手(ニュースが実データ化・APIは各ユーザーが自分のアカウントを登録する方式)

このリポジトリは仕様書の Phase 1〜5 のうち、**Phase 1 (Mockデータによる MVP)** を完了し、
**Phase 2 (実データ連携)** に着手した状態です。

- [x] Jetpack Compose + Material3 によるUI一式
- [x] 企業検索・企業登録(法人番号は複数候補から選択する方式)
- [x] 監視企業一覧・企業詳細(株価チャート・PER/PBR/ROEなど)
- [x] ニュース一覧・重複排除・重要度表示
- [x] 注目企業(急上昇・出来高急増・ニュース急増・IPO・急落・中長期注目)
- [x] 設定画面(通知・テーマ・更新頻度・**APIキー登録**)
- [x] GitHub ActionsでのAPKビルド
- [x] **ニュース: TDnet(適時開示情報)の実データ取得に切り替え済み**(下記「データソースについて」参照)
- [x] **株価・法人番号検索: アプリの「設定」画面から、ユーザー本人のAPIキーを登録することで実データに切り替え可能**(下記参照)
- [ ] Phase 3: 通知・バックグラウンド更新・重要度判定の高度化
- [ ] Phase 4: スコアリングの精緻化
- [ ] Phase 5: AIによるニュース分析

### 株価・法人番号を実データにするには(任意・アプリ内で完結します)

このアプリを使う人ごとに、それぞれ自分自身の無料アカウント(APIキー)をアプリ内の
**「設定」画面 → 「API連携」** から登録する方式にしています。開発者(私)にキーを送っていただく
必要は一切なく、登録したキーはその端末の中だけに保存されます(GitHub Secretsやコードにも
一切含まれません)。未登録のままでもアプリは問題なく動作し、Mock(サンプル)データが表示されます。

登録の流れ:

1. 下表の登録先で、ご自身のAPIキー/IDを取得する(無料)
2. アプリを起動し、下タブの「設定」→「API連携」を開く
3. 取得したキー/IDを貼り付けて「保存」をタップする
4. 以降、株価・法人番号検索がそのアカウントの実データに自動的に切り替わります

| データ | 登録先 | 費用 | 備考 |
|---|---|---|---|
| 株価 | [J-Quants API](https://jpx-jquants.com/) | 無料プランあり | 無料プランは12週間前のデータまで(リアルタイムではない点に注意)。ダッシュボードで発行される `x-api-key` をそのまま貼り付けてください |
| 法人番号 | [国税庁 法人番号システムWeb-API](https://www.houjin-bangou.nta.go.jp/webapi/index.html) | 無料 | invoice-web-api@nta.go.jp 宛にメールで「アプリケーションID」を申請してください。届いたIDをそのまま貼り付けます |

キーを削除したい場合も、同じ画面で入力欄を空にして保存するか「削除」ボタンで消去できます
(端末からその場で削除されるだけで、外部に残ることはありません)。

## 技術構成

| 項目 | 採用技術 |
|---|---|
| 言語 | Kotlin |
| UI | Jetpack Compose (Material3) |
| アーキテクチャ | MVVM (ViewModel + StateFlow) |
| ローカルDB | Room (監視企業の永続化) |
| 設定保存 | DataStore Preferences |
| 非同期処理 | Kotlin Coroutines / Flow |
| DI | 手書きの ServiceLocator(軽量化のためHilt等は不使用) |
| ビルド | Gradle Kotlin DSL, Android Gradle Plugin |
| CI | GitHub Actions |

データ取得層は `NewsProvider` / `MarketDataProvider` / `CompanyProvider` / `CorporateNumberProvider`
という Provider インターフェースで抽象化しています。Phase 1 では各インターフェースの Mock 実装
(`Mock〜Provider`)のみを使用しており、Phase 2 以降で実APIに差し替える際も
UI・ViewModel 側のコードは変更不要です。

## APKの取得方法

1. GitHubリポジトリの **Actions** タブを開く
2. 一覧から最新の `Build APK` ワークフロー実行を選択(緑のチェックマーク = 成功)
3. 画面下部の **Artifacts** から `app-debug`(デバッグ用APK)または `app-release`(リリース用APK)をダウンロード
4. ダウンロードしたzipを展開すると `.apk` ファイルが得られます
5. Android端末に転送し、「提供元不明のアプリ」のインストールを許可してからインストールしてください

Debug APKは署名不要でそのままインストール可能です。Release APKは、下記の署名設定(GitHub Secrets)を
行っていない場合は未署名のままビルドされます(インストールするには別途署名が必要です)。

## GitHub Actions

`.github/workflows/build-apk.yml` が、push・pull request・手動実行のたびに以下を行います。

1. `./gradlew test` (ユニットテスト)
2. `./gradlew assembleDebug`
3. `./gradlew assembleRelease`
4. 生成されたAPKとテストレポートをArtifactとして保存
5. ビルド結果(成功/失敗・ログ抜粋)を `ci-status` ブランチの `last-build.md` に記録

## Releaseビルドの署名設定(任意)

署名済みのRelease APKを生成したい場合は、以下をリポジトリの **Settings → Secrets and variables →
Actions** に登録してください。未設定の場合はRelease APKは未署名でビルドされます(ビルド自体は成功します)。

| Secret名 | 内容 |
|---|---|
| `RELEASE_STORE_FILE_PATH` | チェックアウト後のリポジトリ内でのkeystoreファイルへの相対パス(keystore自体は別途Secretsやセキュアなストレージから配置する必要があります) |
| `RELEASE_STORE_PASSWORD` | keystoreのパスワード |
| `RELEASE_KEY_ALIAS` | 鍵のエイリアス |
| `RELEASE_KEY_PASSWORD` | 鍵のパスワード |

**keystoreファイルやパスワードを絶対にリポジトリへコミットしないでください。**

## セットアップ(ローカルでビルドする場合)

このリポジトリはGitHub Actions上でのビルドを前提としていますが、Android Studio (Jellyfish以降推奨)
でも開けます。

```bash
git clone <このリポジトリのURL>
cd your_investment_partner
./gradlew assembleDebug
```

必要環境: JDK 17, Android SDK (compileSdk 35 / minSdk 26)

## データソースとAPIの制限について

### ニュース(実データ・稼働中)

`TdnetNewsProvider` が [やのしんTDnet WEB-API](https://webapi.yanoshin.jp/tdnet/) から
東証の適時開示情報(決算・業績修正・M&A・自己株式取得など)を取得しています。

- **重要な注意**: これはJPX(日本取引所グループ)公式のAPIではなく、個人が運営する無料の非公式ミラーです。
  JPX公式のTDnet APIは有料の法人向けサービスです。この非公式APIが将来停止・変更される可能性があります
  (その場合はMockNewsProviderへのフォールバック、または別ソースへの切り替えを検討します)
- 認証キー不要、レート制限の明記なし(数分間隔で東証データと同期とのことなので、過度な連続アクセスは行いません)
- 取得した書類は原文(PDF)へのリンクを提示し、内容確認は必ずリンク先で行っていただく仕様です

### 株価(J-Quants) / 法人番号検索(国税庁Web-API) — ユーザーごとの個人アカウント方式

上記「実データにするには」セクションの通り、株価と法人番号検索はユーザーご自身の無料アカウント
登録が必要です。**1つの共有キーをアプリに組み込む方式ではなく、インストールした人それぞれが
アプリの「設定」画面から自分のキーを登録する方式**にしています(`SettingsRepository` に暗号化なしの
ローカル設定として保存 → `CompositeMarketDataProvider` / `CompositeCorporateNumberProvider` が
毎回そのユーザーの最新の設定を読んで実APIかMockかを自動選択します)。未登録の間、または登録した
キーでの呼び出しが失敗した場合は自動的にMockデータへフォールグレードされ、アプリがクラッシュしたり
画面が真っ白になったりすることはありません。

### 全般方針

- robots.txt・利用規約・レート制限を遵守し、過剰なスクレイピングは行いません
- 無料枠に制限がある場合は、その制限をこのREADMEに明記します

## ディレクトリ構成

```text
your_investment_partner/
├── app/
│   ├── src/main/java/com/investmentmonitor/app/
│   │   ├── data/
│   │   │   ├── model/       # ドメインモデル
│   │   │   ├── provider/    # NewsProvider / MarketDataProvider / CompanyProvider など(+Mock実装)
│   │   │   ├── local/       # Room / DataStore
│   │   │   └── repository/  # 重複排除・スコアリングなどのビジネスロジック
│   │   ├── ui/               # 画面ごとのCompose UI + ViewModel
│   │   └── MainActivity.kt / InvestmentMonitorApp.kt / ServiceLocator.kt
│   └── src/test/             # ユニットテスト
├── .github/workflows/build-apk.yml
├── gradle/libs.versions.toml
└── README.md
```

## 免責事項

本アプリは投資情報の収集・整理・分析支援を目的としたツールであり、金融商品取引法上の投資助言・
代理業には該当しません。表示される情報の正確性・完全性・適時性を保証するものではありません。
投資に関する最終判断は、必ずご自身の責任で行ってください。
