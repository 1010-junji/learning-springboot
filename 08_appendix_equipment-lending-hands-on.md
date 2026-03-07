# 付録：Spring Bootハンズオン実践編（備品貸出管理アプリ）

この付録では、第1章から第6章までで学んだ内容を、**1本の小さなSpring Bootアプリ**としてつなげて確認します。  
読み物として眺めるだけではなく、実際に起動し、APIを叩き、コードを追いながら「どのクラスが何をしているのか」を体で覚えることが目的です。

## 1. 付録の目的と本編との対応表

今回のテーマは **備品貸出管理アプリ** です。社員が備品一覧を見て、貸出し、返却し、その履歴を確認する流れを扱います。

| 付録の操作 | 復習できる章 | 着目ポイント |
|---|---|---|
| アプリを起動する | 第1章 / 第5章 | `build.gradle`、`package`、起動クラス、フォルダ構造 |
| 備品一覧を取得する | 第3章 / 第5章 | Controller が窓口、Service が処理、Repository がDB |
| 備品を登録する | 第2章 / 第5章 | DTO と Entity の役割分担、メソッドの責務 |
| 備品を貸し出す | 第4章 / 第5章 | DI、業務ルール、レイヤーの流れ |
| 備品を返却する | 第4章 / 第6章 | 状態遷移、例外時の調査起点 |
| エラーを意図的に出す | 第6章 | スタックトレース、エラーレスポンスの読み方 |

## 2. セットアップ手順

サンプルコードは [appendix/equipment-lending-app](appendix/equipment-lending-app) にあります。

前提:
- Java 21（Private JDK）
- Gradle Wrapper（同梱済み）

起動手順:

```bash
cd appendix/equipment-lending-app
# Windows PowerShell
.\gradlew.bat bootRun

# macOS / Linux
./gradlew bootRun
```

起動後の確認先:
- 備品一覧: `http://localhost:8080/api/equipments`
- 貸出履歴: `http://localhost:8080/api/rental-records`
- H2コンソール: `http://localhost:8080/h2-console`

H2コンソールの接続情報:
- JDBC URL: `jdbc:h2:mem:equipmentdb`
- User Name: `sa`
- Password: 空欄

## 3. ハンズオン手順

### Step 1. 起動確認

目的:
アプリ全体の入口とフォルダ構成を確認する。

触るクラス:
- `build.gradle`
- `src/main/java/com/example/equipmentlending/EquipmentLendingAppApplication.java`

確認ポイント:
- Spring Boot は `main` メソッドから起動する
- `@SpringBootApplication` を起点に Spring がコンポーネントを探す
- `controller`、`service`、`repository`、`entity` などのパッケージが役割ごとに分かれている

### Step 2. 備品一覧を取得する

目的:
1つの GET リクエストがどの層を通って戻るかを追う。

触るクラス:
- `controller/EquipmentController.java`
- `service/EquipmentService.java`
- `repository/EquipmentRepository.java`
- `mapper/EquipmentMapper.java`

リクエスト例:

```http
GET /api/equipments
```

期待レスポンス例:

```json
[
  {
    "id": 1,
    "name": "WindowsノートPC",
    "category": "PC",
    "status": "AVAILABLE"
  }
]
```

どの章の復習になるか:
- 第3章: Controller は窓口に徹する
- 第5章: Service と Repository のバケツリレー

### Step 3. 備品を登録する

目的:
入力用DTOと返却用DTOを分ける意味を確認する。

触るクラス:
- `dto/CreateEquipmentRequest.java`
- `dto/EquipmentResponse.java`
- `service/EquipmentService.java`
- `repository/EquipmentRepository.java`

リクエスト例:

```http
POST /api/equipments
Content-Type: application/json

{
  "name": "Webカメラ",
  "category": "周辺機器"
}
```

期待レスポンス例:

```json
{
  "id": 4,
  "name": "Webカメラ",
  "category": "周辺機器",
  "status": "AVAILABLE"
}
```

どの章の復習になるか:
- 第2章: クラスの責務を分ける
- 第5章補足: DTO と Entity を分離する

### Step 4. 備品を貸し出す

目的:
業務ルールは Service に置く、という原則を体感する。

触るクラス:
- `dto/LendEquipmentRequest.java`
- `service/EquipmentService.java`
- `repository/RentalRecordRepository.java`

リクエスト例:

```http
POST /api/equipments/1/lend
Content-Type: application/json

{
  "userName": "山田太郎"
}
```

期待レスポンス例:

```json
{
  "id": 1,
  "name": "WindowsノートPC",
  "category": "PC",
  "status": "LENT"
}
```

どの章の復習になるか:
- 第4章: DI で Service と Repository がつながる
- 第5章: 複数Repositoryをまたぐ処理を Service がまとめる

### Step 5. 備品を返却する

目的:
状態を戻す処理と、貸出履歴の更新が連動していることを確認する。

触るクラス:
- `service/EquipmentService.java`
- `repository/EquipmentRepository.java`
- `repository/RentalRecordRepository.java`

リクエスト例:

```http
POST /api/equipments/1/return
```

期待レスポンス例:

```json
{
  "id": 1,
  "name": "WindowsノートPC",
  "category": "PC",
  "status": "AVAILABLE"
}
```

どの章の復習になるか:
- 第4章: 依存先を自分で new しない設計
- 第6章: 状態不整合エラー時の見方

### Step 6. エラーを意図的に出して読む

目的:
失敗時のレスポンスと原因箇所の追い方に慣れる。

触るクラス:
- `exception/GlobalExceptionHandler.java`
- `exception/InvalidRequestException.java`
- `exception/BusinessRuleViolationException.java`
- `exception/ResourceNotFoundException.java`

試すリクエスト例:

```http
POST /api/equipments/999/lend
Content-Type: application/json

{
  "userName": "山田太郎"
}
```

```http
POST /api/equipments/2/lend
Content-Type: application/json

{
  "userName": "山田太郎"
}
```

期待レスポンスの見方:
- `status`: HTTPの結果
- `message`: どの業務ルールに引っかかったか
- `path`: どのAPIで起きたか

どの章の復習になるか:
- 第6章: エラー本文だけでなく、発生場所と再現条件を整理する

## 4. つまずきやすいエラーと見方

### `404 Not Found`

見る場所:
- URL の打ち間違い
- Controller の `@RequestMapping` と `@GetMapping` / `@PostMapping`

### `400 Bad Request`

見る場所:
- リクエストJSONの項目名
- `CreateEquipmentRequest` や `LendEquipmentRequest` の中身
- Service の入力チェック

### `409 Conflict`

見る場所:
- Service の業務ルール
- 備品の `status`
- `rental_records` に未返却レコードが残っていないか

### `500 Internal Server Error`

見る場所:
- コンソールのスタックトレース先頭
- SQL やテーブル定義
- `schema.sql` と `data.sql`

## 5. 次の一歩

この付録を理解できたら、次は次の順で発展させると実務に近づきます。

1. `JdbcTemplate` を Spring Data JPA に置き換える
2. 手書きの入力チェックを Bean Validation に置き換える
3. Unit Test と Integration Test を分けて書く
4. 認証認可を足して「誰が借りたか」をログインユーザーと結び付ける

## 6. 付録で使う主なファイル

- [EquipmentLendingAppApplication.java](appendix/equipment-lending-app/src/main/java/com/example/equipmentlending/EquipmentLendingAppApplication.java)
- [EquipmentController.java](appendix/equipment-lending-app/src/main/java/com/example/equipmentlending/controller/EquipmentController.java)
- [EquipmentService.java](appendix/equipment-lending-app/src/main/java/com/example/equipmentlending/service/EquipmentService.java)
- [EquipmentRepository.java](appendix/equipment-lending-app/src/main/java/com/example/equipmentlending/repository/EquipmentRepository.java)
- [GlobalExceptionHandler.java](appendix/equipment-lending-app/src/main/java/com/example/equipmentlending/exception/GlobalExceptionHandler.java)





