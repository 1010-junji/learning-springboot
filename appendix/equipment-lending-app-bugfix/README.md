[配布用クイックスタート](QUICKSTART.md)

# 備品貸出管理アプリ

Spring Boot 3 の基本構成を追体験するための、研修用サンプルアプリです。  
Controller / Service / Repository / Entity / DTO / Mapper / Exception の役割分担を、H2 データベース付きで確認できます。

## 動かし方

前提:
- Java 21（Private JDK など）
- Gradle Wrapper（このリポジトリに同梱済み）

### Windows PowerShell

```powershell
# 任意: JDK 21 の場所を明示する場合
$env:JDK21_HOME = '<your-jdk21-path>'

# 起動
.\bootrun-jdk21.ps1
```

`bootrun-jdk21.ps1` は次の順で JDK 21 を探します。
1. `JAVA_HOME`（21以上）
2. `JDK21_HOME`（21以上）
3. `PATH` 上の `java`（21以上）

### macOS / Linux

```bash
./gradlew bootRun
```

起動後の確認先:
- API一覧確認: `http://localhost:8080/api/equipments`
- H2コンソール: `http://localhost:8080/h2-console`

## API

- `GET /api/equipments`
- `POST /api/equipments`
- `POST /api/equipments/{id}/lend`
- `POST /api/equipments/{id}/return`
- `GET /api/rental-records`

## テスト

### Windows PowerShell

```powershell
# 任意: JDK 21 の場所を明示する場合
$env:JDK21_HOME = '<your-jdk21-path>'

# テスト
.\test-jdk21.ps1
```

### macOS / Linux

```bash
./gradlew test
```