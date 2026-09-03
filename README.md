# 投資情報・企業監視アプリ (Investment Monitor)

Android専用の投資情報・企業監視アプリです。監視したい企業を登録すると、その企業に関する
ニュース・株価・決算情報などを一つのアプリで確認できます。

> **重要な注意事項**
> 本アプリは投資助言サービスではありません。「注目企業」「おすすめ企業」等の表示も投資推奨ではなく、
> 客観的な指標から抽出した参考情報です。投資判断は必ずご自身の責任で行ってください。

## 現在のステータス: Phase 1 (MVP)

このリポジトリは仕様書の Phase 1〜5 のうち、**Phase 1 (Mockデータによる MVP)** を実装済みです。

- [x] Jetpack Compose + Material3 によるUI一式
- [x] 企業検索・企業登録(法人番号は複数候補から選択する方式)
- [x] 監視企業一覧・企業詳細(株価チャート・PER/PBR/ROEなど)
- [x] ニュース一覧・重複排除・重要度表示
- [x] 注目企業(急上昇・出来高急増・ニュース急増・IPO・急落・中長期注目)
- [x] 設定画面(通知・テーマ・更新頻度)
- [x] Mockデータでの動作(NewsProvider / MarketDataProvider / CompanyProvider / CorporateNumberProvider を抽象化済み)
- [x] GitHub ActionsでのAPKビルド
- [ ] Phase 2: 実際の企業・株価・ニュースAPI連携
- [ ] Phase 3: 通知・バックグラウンド更新・重要度判定の高度化
- [ ] Phase 4: スコアリングの精緻化
- [ ] Phase 5: AIによるニュース分析

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

## データソースとAPIの制限について(Phase 2以降の実装方針)

Phase 1は全てMockデータで動作します。Phase 2で実データに接続する際は、以下の優先順位・制限を
考慮して実装します(詳細はコード内コメントも参照してください)。

- **企業情報・法人番号**: 国税庁 法人番号公表サイト Web-API(無料・登録不要で利用可能)
- **株価情報**: 無料/有料の金融データAPIを比較検討し、利用規約・レート制限をこのREADMEに明記します
- **ニュース**: 企業公式サイト・IR・適時開示(TDnet等)・官公庁情報を優先し、RSS/公開APIを使用します。
  robots.txt・利用規約・レート制限を遵守し、過剰なスクレイピングは行いません
- 上記いずれも無料枠に制限がある場合、その制限を明記した上で運用します

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
