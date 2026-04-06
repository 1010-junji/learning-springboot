# 09 ハンズオン: 不具合修正を通じて学ぶDIとSpring Bootの実践

本章では、前章までで作成した「備品貸出アプリ（Equipment Lending App）」のコードベースを用いて、実際に現場でよく起こる「不具合（バグ）」の修正に挑戦します。

ゼロからの開発とは異なり、実務では **「既存のコードを読み解き、原因を特定して安全に修正する」** スキルが最も重要になります。
ここでは、DIやSpring Bootの重要な仕組み（AOP、データバインディング）に関連した3つの例題を通じて、実践的なデバッグプロセスを学びましょう。

## 準備

本ハンズオン用の破損した（わざとバグを含ませた）プロジェクトが用意されています。
エディタで `appendix/equipment-lending-app-bugfix` フォルダを開き、このプロジェクト上で作業を行います。

このプロジェクトでは、予期しない例外が発生した場合に `GlobalExceptionHandler` が 500 エラーのレスポンスを返しつつ、コンソールにスタックトレースを出力するようになっています。
デバッグ時はレスポンスのメッセージだけで判断せず、**コンソールログもあわせて確認しながら原因を追跡する** ようにしてください。

---

## 例題1: 起動しないアプリケーション（DIの競合エラー）

### 状況

開発チームのメンバーが、備品の貸出完了時に「通知」を行うための `NotificationService` インターフェースを追加しました。
コンソール出力用の `ConsoleNotificationService` を実装していましたが、別のメンバーが将来機能として「メール通知用」の `EmailNotificationService` も作成し、両方に `@Service` をつけてコミットしてしまいました。

プロジェクトを起動（VS Codeなら F5 キー または `./bootrun-jdk21.ps1`）してみてください。起動時に以下のような長いエラーが出てアプリケーションが強制終了してしまいます。

```log
***************************
APPLICATION FAILED TO START
***************************

Description:
Parameter 3 of constructor in com.example.equipmentlending.service.EquipmentService required a single bean, but 2 were found:
	- consoleNotificationService: defined in file [...]
	- emailNotificationService: defined in file [...]

Action:
Consider marking one of the beans as @Primary, updating the consumer to accept multiple beans, or using @Qualifier to identify the bean that should be consumed
```

### 課題

1. なぜこのエラーが発生したのか、SpringのDIコンテナの仕組みから理由を説明してください。
2. 今回は当面コンソール通知（`ConsoleNotificationService`）を利用することとします。エラーログの `Action` とヒントをもとに、起動できるように修正してください。

### ヒント

- Springは `EquipmentService` を生成するとき、引数の `NotificationService` インターフェースの実装クラスを探します。
- 複数の候補がある場合、どれを注入（DI）すればいいかわからずエラーになります。
- 優先すべきクラスに付与するアノテーションは **「@Primary」** です。

---

## 例題2: 入力したはずのデータが消える？（データバインディングの不具合）

### 状況

例題1を解決してアプリが起動するようになりました。
テストのために、新しい備品「プロジェクター」を登録するAPIを叩いてみたところ、なぜかリクエストが失敗します。

`appendix/equipment-lending-app-bugfix/sample-requests.http` を利用して、以下のリクエストを送信してみてください。

```http
POST http://localhost:8080/api/equipments
Content-Type: application/json

{
  "name": "プロジェクター",
  "category": "電子機器"
}
```

**[レスポンス]**

```json
{
  "message": "備品名は必須です。",
  "status": 400
}
```

ちゃんと `"name"` を送信しているのに、サーバー側は「必須です（nullまたは空っぽです）」とエラーを返してきます。

### 課題

Controllerクラスの受け取り方に原因があります。設定が一つ漏れているため、送信したJSONデータがJavaのオブジェクトにうまく変換（バインディング）されていません。該当の不具合を特定し、修正してください。

### ヒント

- `com.example.equipmentlending.controller.EquipmentController` の `createEquipment` メソッドを確認しましょう。
- 引数の `CreateEquipmentRequest` の前に、**リクエストのBodyをJSONとして受け取るためのアノテーション**（`@RequestBody`）が設定されているでしょうか？

---

## 例題3: 貸出記録がないのに「貸出中」になった備品（トランザクション管理の欠落）

### 状況

備品の登録は無事にできるようになりました。続いて、貸出のテストをします。
しかし、特定のユーザー（`BUG_USER`）が貸出リクエストを送ると、システム側でエラーが発生してしまいます（今回はわざと例外が起きるようにコーディングされています）。

問題は、「エラーが起きた」こと自体ではなく、**エラーが発生したあとのデータベースの状態** です。スタックトレースで例外が発生した位置を確認しつつ、更新処理が途中まで反映されていないかも観察してください。

1. まだ貸し出されていない備品のID（例: 1）を使って、`BUG_USER` で貸出APIを叩いてみましょう（わざとエラーにします）。

```http
POST http://localhost:8080/api/equipments/1/lend
Content-Type: application/json

{
  "userName": "BUG_USER"
}
```

2. サーバーで「システムエラーが発生しました。コンソールログとスタックトレースを確認してください。」となったあと、コンソールに出力された例外ログを確認しましょう。続けて、備品一覧取得API（`GET http://localhost:8080/api/equipments`）を叩いてみましょう。

**「貸出履歴」の保存には失敗したはずなのに、ステータスだけが `LENT`（貸出中）に書き換わってしまっています。**
これでは、誰が借りたかわからない宙に浮いた備品になってしまい、**データ不整合** が発生しています。

### 課題

1. `com.example.equipmentlending.service.EquipmentService` の `lendEquipment` メソッドを確認してください。
2. エラーが発生しても、それまでの変更（ステータスの書き換えなど）がすべて **「なかったこと（ロールバック）」** になるように、適切なアノテーションを付与して修正してください。
3. （確認のため）一度H2コンソールでデータをリセット（または再起動）し、再度エラーを起こしてもステータスが `AVAILABLE` のまま保たれることを確認しましょう。

### ヒント

- データベースの複数の更新処理を「すべて成功するか、すべて失敗するか」の全か無にする仕組みを「トランザクション」と呼びます。
- Spring Bootでは、メソッドに **`@Transactional`** をつけるだけで、例外発生時に自動でロールバックをしてくれます。

---

## 【応用編】 例題4: はじかれるはずの不正入力（バリデーションのすり抜け）

### 状況

備品登録のリクエスト（`CreateEquipmentRequest`）に対し、別の担当者が「名前やカテゴリが空っぽだったり、長すぎたりした場合はエラーにしたい」と考えました。
彼は `build.gradle` に `spring-boot-starter-validation` を追加し、DTOにも `@NotBlank` などのアノテーションを記述しました。

しかしテストをしてみると、**アノテーションを書いたにもかかわらず、何もバリデーションが働きません**。50文字以上の極端に長いデータを送るとチェックをすり抜け、そのままデータベースにたどり着いた結果、「500 Internal Server Error（またはデータベースの制約違反エラー）」が発生してアプリケーションが異常終了してしまいます。

以下のJSONを送ってテストしてみてください。

```http
POST http://localhost:8080/api/equipments
Content-Type: application/json

{
  "name": "これは50文字を超える非常に長い不正な備品名のテストデータです。本来であればControllerの段階でバリデーションアノテーションによって入力チェックが働き、400 Bad Requestとして弾かれなければなりませんが、設定が抜けているためDBまで到達してしまいます",
  "category": "電子機器"
}
```

### 課題

1. なぜDTOに `@NotBlank` を書いたのに無視されてしまうのでしょうか。
2. Controllerクラスを修正し、Spring Bootの機能として正常にバリデーションが働き、400 Bad Request（MethodArgumentNotValidException）が適切な場所で返るように修正してください。

### ヒント

- アノテーションをDTOに書くだけでは、Springは自動でチェックしてくれません。
- 「この引数はバリデーションルールに従ってチェックしてくださいね」と、**Controllerの引数に指示するアノテーション**（`@Validated` または `@Valid`）が必要です。

---

## 【応用編】 例題5: 「new」の罠（NullPointerExceptionとDIコンテナ）

### 状況

システムに「現在のカテゴリの数」を分析して返す新しい機能が追加されました。
`EquipmentCategoryAnalyzer`（`@Component` 付き）を新しく作り、Controller（`GET /api/equipments/categories/count`）からそれを呼び出すように実装したようです。

しかし、ブラウザやAPIクライアントから `http://localhost:8080/api/equipments/categories/count` にアクセスすると、**500 Internal Server Error（NullPointerException）** が発生してしまいます。レスポンスに加えて、コンソールに出力されたスタックトレースも確認してみましょう。

### 課題

1. `EquipmentController` の `getCategoryCount` メソッドから `EquipmentCategoryAnalyzer` までの処理を追いかけ、どこで、なぜ `NullPointerException` が発生したか特定してください。
2. Spring（DIコンテナ）の力を正しく借りる設計に修正し、正常にカテゴリ数が返却されるようにしてください。

### ヒント

- `EquipmentCategoryAnalyzer` の中には、`EquipmentRepository` が `@Autowired` されています。
- しかし、Controllerの中で `new EquipmentCategoryAnalyzer()` と**自分でインスタンスを作ってしまっています**。
- SpringのDIコンテナは、「Springがインスタンス化した（管理している）クラス（Bean）」に対してのみ、`@Autowired` を解決してくれます。自分で `new` したオブジェクトにはDIは行われず、フィールドは `null` のままになります。
- Controllerのコンストラクタで、`EquipmentCategoryAnalyzer` をDIで受け取るように書き換えましょう。

---

← [付録ハンズオンに戻る](08_appendix_equipment-lending-hands-on.md) | → [第10章へ進む](10_interface-deep-dive.md)
