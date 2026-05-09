# Carrot Market API - Core Java Clone

> SpringとORMを使わず、**素のJava**で実装するCarrot Market（당근마켓）バックエンドAPI

## プロジェクトを始めた理由

### 解決したかった問題
1. Javaをどんな場面で使うべきか、その強みが分からない
2. Spring Frameworkがなぜ強力なのか、内部の動作原理が分からない
3. MyBatis/JPAといったORMフレームワークを使い続けることで原理の理解が浅くなっている

### アプローチ
**フレームワークが担っている複雑な領域を自分で実装し、原理を理解する**

## 期待できる効果

- **Java言語への理解向上**: Javaの特徴と強みを意識しながら使う
- **HTTPプロトコルの理解**: Method、Header、Session/Cookieなどのウェブ通信の原理
- **DB通信の理解**: JDBCのみでデータアクセス層を実装
- **フレームワークの価値を実感**: なぜ必要か、どこまで隠してくれているかを体感

## 技術スタック

| 項目 | 選択 | 理由 |
|------|------|------|
| **言語** | Java 17 | Java 8の経験をベースに最新機能と比較 |
| **ビルド** | Maven | JDBC、Servlet API依存関係の管理 |
| **サーバー** | Tomcat 10 | Jakarta EEサポート |
| **DB** | PostgreSQL | 慣れたDBで学習に集中 |
| **IDE** | VS Code + Java Extension | （任意） |

## プロジェクト状況

> 現在、他の活動のため開発を一時停止しています。

## ドキュメント

| ドキュメント | 説明 |
|-------------|------|
| [OCIサーバーセットアップガイド](docs/oci_web_server_setup_guide_ja.md) | UbuntuベースのOCIサーバーにTomcat + PostgreSQL環境を構築 |
| [CI/CDパイプライン構築ガイド](docs/cicd_pipeline_setup_guide_ja.md) | GitHub Actionsを使った自動デプロイパイプラインの構成 |
