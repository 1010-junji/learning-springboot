# 第10章：インターフェイスの深掘り（使いどころの判定と設計の勘所）

第2章や第4章で、インターフェイスは「約束事」「差し替えを可能にする仕組み」として登場しました。
ここまで学んだ段階だと、多くの学習者が次の状態で止まります。

- 言葉としての意味は分かる
- DIと相性が良いことも分かる
- しかし、実際の設計で「ここはインターフェイスを切るべきか」を自信を持って判断できない

本章の目的は、まさにこの最後の壁を越えることです。
単に「インターフェイスは良いもの」と覚えるのではなく、**どんな時に効き、どんな時には過剰設計になり、何と使い分けるべきか** を具体例で整理します。

## この章でできるようになること

- インターフェイスを使うべきケースを、変更可能性と責務分離の観点で説明できる
- インターフェイスを使うべきではないケースを、過剰設計の観点で説明できる
- 抽象クラス、具象クラス、移譲、関数化などの代替手段と使い分けできる
- Spring Bootの現場コードで、どこにインターフェイスを置くと効果が高いか判断できる

---

## 1. まず結論: インターフェイスは「とりあえず切るもの」ではない

初心者が誤解しやすいのは、以下のような極端な理解です。

- オブジェクト指向っぽいから全部インターフェイスにする
- Serviceには必ずインターフェイスが必要
- 実装クラスを直接使うのは悪い設計

これは正しくありません。

インターフェイスは、**将来の変更可能性や役割分離をコード上に明示するための道具** です。
したがって、変更や差し替えの可能性がほとんどなく、呼び出し側と実装側を分ける意味も薄いなら、インターフェイスは不要です。

逆に、次のような状況では強く効きます。

- 実装が複数ありうる
- 利用側に「中身」ではなく「できること」だけを意識させたい
- テスト時に差し替えたい
- チーム開発で先に契約だけ決めたい
- フレームワークやDIで実装切り替えをしたい

つまり判断軸は、**インターフェイスがオブジェクト指向的かどうかではなく、「境界を作る価値があるかどうか」** です。

---

## 2. インターフェイスの本質: 「型」ではなく「境界」で考える

インターフェイスを「メソッドの一覧」とだけ覚えると、実務で使いどころを判断しにくくなります。
実務では、次のように理解した方が役に立ちます。

> インターフェイスとは、呼び出す側に対して「これだけ知っていればよい」という境界線を引くためのもの。

たとえば、通知機能を考えます。

```java
// NotificationSender.java
public interface NotificationSender {
    void send(String message);
}
```

この時、呼び出し側は「コンソールに出すのか」「メールを送るのか」「Slackに投稿するのか」を知る必要がありません。
必要なのは「通知を送れる」という事実だけです。

この状態が作れると、利用側のコードは次のように安定します。

```java
// EquipmentService.java
public class EquipmentService {
    private final NotificationSender notificationSender;

    public EquipmentService(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    public void lendCompleted(String userName) {
        notificationSender.send(userName + "さんへの貸出が完了しました。");
    }
}
```

ここで `EquipmentService` が依存しているのは「通知の具体方式」ではなく、「通知できること」という契約です。
このように、**依存先を具体実装から契約へ引き上げる** のがインターフェイスの本質です。

---

## 3. インターフェイスを使うべきケース

ここでは「どういう時に切ると得をするか」を、ケースごとに見ていきます。

### 3-1. 実装が複数ありうる時

最も分かりやすいケースです。

- 通知方法が複数ある
- 支払い方法が複数ある
- 外部APIの接続先が複数ある
- 保存先がファイル、DB、クラウドで切り替わる

#### ケーススタディ: 通知方式の追加

最初はコンソール通知しかないとしても、将来こうなりがちです。

- 開発環境ではコンソール通知
- 本番環境ではメール通知
- 将来はSlack通知も追加

ここでは、まず「最初は自然に見える書き方」から、どこで痛みが出るかを見ます。

##### Before: 最初の要件だけなら素直に書けてしまう

```java
// EquipmentService.java
public class EquipmentService {
    private final ConsoleNotificationSender notificationSender = new ConsoleNotificationSender();

    public void lendCompleted(String userName, String equipmentName) {
        String message = userName + "さんへの" + equipmentName + "の貸出が完了しました。";
        notificationSender.send(message);
    }
}
```

```java
// ConsoleNotificationSender.java
public class ConsoleNotificationSender {
    public void send(String message) {
        System.out.println("[Console] " + message);
    }
}
```

この段階では、無理にインターフェイスを切らない方が読みやすく見えます。

##### Pain: 要件追加で利用側が通知方式を背負い始める

本番環境ではメール通知にしたい、という要求が入ると、利用側の `EquipmentService` を触りたくなります。

```java
// EquipmentService.java
public class EquipmentService {
    public void lendCompleted(String userName, String equipmentName, boolean production) {
        String message = userName + "さんへの" + equipmentName + "の貸出が完了しました。";

        if (production) {
            new EmailNotificationSender().send(message);
        } else {
            new ConsoleNotificationSender().send(message);
        }
    }
}
```

ここで起きている問題は次です。

- 貸出処理が通知方式の詳細まで知っている
- 環境切り替えの条件分岐が業務ロジックに混ざる
- テスト時に本物の通知を止めにくい

##### After: 通知できることだけを契約にする

```java
// NotificationSender.java
public interface NotificationSender {
    void send(String message);
}
```

```java
// EquipmentService.java
public class EquipmentService {
    private final NotificationSender notificationSender;

    public EquipmentService(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    public void lendCompleted(String userName, String equipmentName) {
        String message = userName + "さんへの" + equipmentName + "の貸出が完了しました。";
        notificationSender.send(message);
    }
}
```

```java
// EmailNotificationSender.java
public class EmailNotificationSender implements NotificationSender {
    @Override
    public void send(String message) {
        System.out.println("[Email] " + message);
    }
}
```

この形にすると、`EquipmentService` は「通知方式」ではなく「通知できること」だけ知れば済みます。
つまり、**変更が起きやすい側を境界の向こうへ押し込められる** ようになります。

この時、最初から `ConsoleNotificationSender` に直接依存していると、利用側が具象クラス名に引きずられます。
一方、`NotificationSender` を切っておけば、利用側は変えずに実装だけ差し替えられます。

#### こういう時に向いている理由

- 実装の増加を吸収しやすい
- `@Primary` や `@Qualifier` でSpringのDIと自然に組み合わせられる
- 「使う側」は増えても、変更箇所を実装側に閉じ込めやすい

### 3-2. 外部サービスやインフラを隠したい時

DB、メール、S3、決済APIなど、外部と通信する処理は変更リスクが高いです。
アプリ本体から見ると、「何ができるか」だけ分かれば十分で、「どう接続するか」は詳細です。

#### ケーススタディ: 決済APIの切り替え

```java
// PaymentGateway.java
public interface PaymentGateway {
    PaymentResult charge(int amount);
}
```

実装例:

- `StripePaymentGateway`
- `MockPaymentGateway`
- `InternalTestPaymentGateway`

本番ではStripe、ローカル検証ではMock、結合試験では社内の検証環境、といった切り替えが発生しやすい領域です。
こういう場所は、インターフェイスで境界を切る価値が高いです。

### 3-3. テストで差し替えたい時

インターフェイスがあると、テストダブルを作りやすくなります。

```java
// FakeNotificationSender.java
public class FakeNotificationSender implements NotificationSender {
    private final List<String> messages = new ArrayList<>();

    @Override
    public void send(String message) {
        messages.add(message);
    }

    public List<String> getMessages() {
        return messages;
    }
}
```

このような偽物実装を使えば、実際にメール送信せず「何が送られたか」だけ検証できます。

#### 向いている場面

- 外部通信を伴う
- 実行に時間がかかる
- 副作用が大きい
- エラー系の再現をコントロールしたい

### 3-4. チームで分業する時

インターフェイスは「契約書」としても機能します。

たとえば、注文処理チームが `CouponPolicy` の仕様だけ先に確定し、実装は別メンバーが後で作ることができます。

```java
// CouponPolicy.java
public interface CouponPolicy {
    int discount(int price);
}
```

利用側はこの契約を前提に開発を進められます。
これは大規模開発ほど効きます。

### 3-5. 役割の名前を明確にしたい時

インターフェイスを切ること自体が、設計意図の明示になることがあります。

たとえば、`UserRepository` という名前を見ると、呼び出し側は「ユーザーを永続化・取得する窓口なのだな」と理解できます。
実装がJPAなのか、MyBatisなのか、メモリ上なのかは重要ではありません。

つまり、**インターフェイス名が“役割の名前”として機能する** 場合があります。

---

## 4. インターフェイスを使うべきではないケース

ここは非常に重要です。
現場では「使わない判断」ができる方が、むしろ設計力があります。

### 4-1. 実装が1つで、今後も増える見込みが薄い時

たとえば、アプリ内部だけで使う単純な計算クラスです。

ここは逆に、**インターフェイスを導入しない方が自然なケース** です。

##### Before: 具象クラスだけで十分な実装

```java
// PriceCalculator.java
public class PriceCalculator {
    public int calculateTotal(int price, int tax) {
        return price + tax;
    }
}
```

この時点では、責務も読みやすさも十分です。

このクラスに対して、次のようにするのは過剰になりがちです。

##### Pain: 抽象化の利益より、読みづらさが先に増える

```java
// PriceCalculator.java
public interface PriceCalculator {
    int calculateTotal(int price, int tax);
}

```

```java
// DefaultPriceCalculator.java
public class DefaultPriceCalculator implements PriceCalculator {
    @Override
    public int calculateTotal(int price, int tax) {
        return price + tax;
    }
}
```

この変更で起きるのは、主に次のような状態です。

- 実装が1つしかないのに、追うファイルが2つに増える
- 差し替え予定がないのに、抽象化の説明コストだけが増える
- 初学者には「なぜ分けたのか」が見えにくい

##### After: 具象クラスに戻した方が、むしろ意図が明確

```java
// PriceCalculator.java
public class PriceCalculator {
    public int calculateTotal(int price, int tax) {
        return price + tax;
    }
}
```

このケースで守るべきなのは、「抽象化すること」ではなく「必要以上に抽象化しないこと」です。
つまり、**インターフェイスを使わない判断も設計の一部** です。

コード量は増えますが、設計上の利益がほとんどありません。

#### 判断ポイント

- 差し替え要求が現実的にあるか
- 契約と実装を分ける意味があるか
- テストや分業で実益があるか

この3つにほぼ全部「いいえ」なら、具象クラスのままで十分です。

### 4-2. ドメインの中心的な実体そのものを無理に抽象化する時

例えば、`User` や `Order` といったEntityやDTOです。

```java
// User.java
public interface User {
    String getName();
}
```

こうした抽象化は、よほど特殊な理由がない限り不自然です。
なぜなら、`User` は「役割」ではなく「具体的な業務上の概念」だからです。

通常の業務システムでは、以下のようにそのままクラスで表現する方が自然です。

- `User`
- `Order`
- `Equipment`
- `CreateEquipmentRequest`

### 4-3. 単なる「将来の保険」として作る時

「将来何かあるかもしれないから、とりあえずInterfaceを作っておこう」は危険です。
なぜなら、将来の変更はたいてい予想通りには来ないからです。

たとえば、

- 1実装しかないのに `FooService` と `FooServiceImpl` を作る
- その後ずっと実装が増えない
- クラス名だけが増えて、読むコストだけが上がる

という状態はよくあります。

**本当に避けたいのは変更そのものではなく、変更が広範囲に波及すること** です。
波及しないなら、最初から抽象化しなくてよい場合も多いです。

### 4-4. 小さな個人開発・学習用コードで、意図がぼやける時

学習初期のコードでは、まず「処理の流れ」を掴む方が重要な場面があります。
その段階で抽象化を増やしすぎると、かえって読みづらくなります。

特に初心者が読み解く教材では、以下の弊害が出ます。

- どのクラスが実際に動いているのか分かりにくい
- 画面遷移やAPI処理の本筋より、設計パターンの理解に注意が奪われる
- デバッグ時の追跡が難しくなる

教材や小規模ツールでは、まず具象クラスで素直に書き、必要が出たところで抽象化する方が学習効果が高いことがあります。

---

## 5. よくある誤解

### 誤解1: Serviceには必ずインターフェイスが必要

必ずではありません。
Spring Bootの実務でも、Serviceインターフェイスを作るプロジェクトもあれば、具象クラスだけで進めるプロジェクトもあります。

#### インターフェイスを作ることが多い場面

- 外部連携が多い
- 実装差し替えが現実的
- テスト戦略としてモック差し替えを重視している
- ドメイン境界やレイヤ境界を強く意識している

#### 作らないことが多い場面

- 小規模な業務ロジック中心
- 実装が単一で安定している
- 直接クラスを読んだ方が理解しやすい

### 誤解2: インターフェイスがあるだけで疎結合になる

半分正しく、半分誤りです。

インターフェイスを置いても、呼び出し側が実装の事情を知りすぎていたら疎結合ではありません。

##### Before: いちおうインターフェイスはある

```java
// PaymentGateway.java
public interface PaymentGateway {
    PaymentResult charge(int amount);
}
```

一見すると、これだけで抽象化できているように見えます。

##### Pain: 利用側が実装専用の都合を知ってしまう

しかし、利用側が特定実装前提の使い方を始めると、契約があっても疎結合ではなくなります。

```java
// OrderService.java
public class OrderService {
    private final PaymentGateway paymentGateway;

    public OrderService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public void checkout(int amount) {
        PaymentResult result = paymentGateway.charge(amount);

        if (result.getStripeResponseCode().startsWith("S-")) {
            System.out.println("Stripe向けの後続処理");
        }
    }
}
```

このコードの問題は、`PaymentGateway` という契約を使っているように見えて、実際には `Stripe` の事情を利用側が知ってしまっていることです。

##### After: 契約に含めるのは、利用側が本当に必要な情報だけ

```java
// PaymentResult.java
public class PaymentResult {
    private final boolean success;

    public PaymentResult(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
```

```java
// OrderService.java
public class OrderService {
    private final PaymentGateway paymentGateway;

    public OrderService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public void checkout(int amount) {
        PaymentResult result = paymentGateway.charge(amount);

        if (result.isSuccess()) {
            System.out.println("決済成功");
        }
    }
}
```

このように、**利用側に必要な事実だけを契約に含める** ことで、はじめて抽象化が効きます。

例:

- `EmailNotificationSender` 専用の前提で引数を組み立てている
- `StripePaymentGateway` でしか意味のない戻り値を契約に含めている

このように、**契約が実装詳細に引きずられていると、見た目だけ抽象化しても効果が薄い** のです。

### 誤解3: インターフェイスを使えば変更に強い

これも条件付きです。
変更に強くなるのは、**変わりやすい部分を境界の向こう側に押し込められた時だけ** です。

境界の切り方が悪ければ、インターフェイスがあっても利用側の修正は減りません。

---

## 6. 類似手法との使い分け

ここが実務で最も重要な比較ポイントです。
「インターフェイス以外でも解ける問題か」を見極められるようにしましょう。

### 6-1. 抽象クラスとの違い

抽象クラスは、共通処理を持ちながら一部だけ差し替えたい時に向いています。

ここも、インターフェイスを使うか抽象クラスを使うかで迷いやすいところです。

##### Before: 役割だけ共通で、中身はかなり違うならインターフェイス向き

```java
// ReportFormatter.java
public interface ReportFormatter {
    String format(String rawData);
}
```

CSV と PDF で中身が大きく違い、共通処理もほとんどないなら、まずはこの程度の契約だけで十分です。

##### Pain: 共通手順があるのに、全部を各実装へ重複して書き始める

```java
// CsvReportGenerator.java
public class CsvReportGenerator {
    public void generate() {
        loadData();
        formatCsv();
        output();
    }

    private void loadData() {
        System.out.println("load");
    }

    private void formatCsv() {
        System.out.println("format csv");
    }

    private void output() {
        System.out.println("output");
    }
}
```

```java
// PdfReportGenerator.java
public class PdfReportGenerator {
    public void generate() {
        loadData();
        formatPdf();
        output();
    }

    private void loadData() {
        System.out.println("load");
    }

    private void formatPdf() {
        System.out.println("format pdf");
    }

    private void output() {
        System.out.println("output");
    }
}
```

この段階では、役割の違いよりも「手順の共通性」の方が支配的です。
それなのにインターフェイスだけで整理しようとすると、共通部分が重複しやすくなります。

##### After: 手順の骨格を共通化したいなら抽象クラスが向く

```java
// BaseReportGenerator.java
public abstract class BaseReportGenerator {
    public void generate() {
        loadData();
        format();
        output();
    }

    protected abstract void format();

    protected void loadData() {
        System.out.println("load");
    }

    protected void output() {
        System.out.println("output");
    }
}
```

#### 使い分けの基本

- インターフェイス: 何ができるかを定義したい
- 抽象クラス: 共通処理を持たせつつ差分だけ変えたい

#### インターフェイスが向く場面

- 実装方式が大きく異なる
- 共通処理を無理に持たせると不自然
- 複数の役割を柔軟に組み合わせたい

#### 抽象クラスが向く場面

- 手順の大枠は共通
- 一部の手順だけ子クラスに任せたい
- 共通ロジックを重複なく持ちたい

### 6-2. 具象クラス1つで十分なケース

最も見落とされがちですが、これが正解のことは多いです。

もし次の条件なら、具象クラスのみで問題ない可能性が高いです。

- 実装は1つ
- 差し替え予定がない
- テストもそのまま書ける
- 呼び出し側に実装詳細が漏れても困らない

「まず具象クラスで書き、必要になったら抽象化する」は、十分まともな判断です。

### 6-3. 移譲（Delegation）との違い

移譲は、あるオブジェクトが自分の処理を別オブジェクトに任せることです。
インターフェイスとは競合関係ではなく、組み合わせて使うことが多いです。

```java
// OrderService.java
public class OrderService {
    private final DiscountPolicy discountPolicy;

    public OrderService(DiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }

    public int calculatePrice(int price) {
        return discountPolicy.apply(price);
    }
}
```

この例では、

- 移譲: `OrderService` が割引計算を `discountPolicy` に任せている
- インターフェイス: `DiscountPolicy` を差し替え可能にしている

つまり、**移譲は責務の分担の話、インターフェイスは境界と契約の話** です。

### 6-4. 関数・ユーティリティメソッドで十分なケース

状態を持たず、差し替えも不要で、ただの計算だけなら、クラス分割やインターフェイス化は不要なことがあります。

たとえば、日付整形や単純な税率計算などです。

```java
// PriceUtils.java
public class PriceUtils {
    public static int addTax(int price) {
        return (int) (price * 1.1);
    }
}
```

このような処理まで無理にインターフェイス化すると、設計の重さだけが増えます。

### 6-5. Enumで表現した方が自然なケース

「種類の違い」を表したいだけなら、インターフェイスより `enum` が自然なことがあります。

たとえば備品の状態です。

```java
// EquipmentStatus.java
public enum EquipmentStatus {
    AVAILABLE,
    LENT,
    BROKEN
}
```

これは「複数実装を差し替えたい」のではなく、「取りうる値が決まっている」問題です。
こういう時はインターフェイスより列挙型の方が適切です。

---

## 7. ケーススタディで見る「切るべきか」の判断

### ケース1: 貸出完了通知

#### 状況

- 今はコンソール通知のみ
- 将来メールやSlack通知の可能性あり
- テストでは実送信したくない

#### 判断

インターフェイスを使う価値が高いです。

#### 理由

- 実装が複数になりやすい
- 外部通信で副作用がある
- テストで偽物に差し替えたい

#### 例

- `NotificationSender`
- `ConsoleNotificationSender`
- `EmailNotificationSender`
- `SlackNotificationSender`

### ケース2: 金額の単純計算

#### 状況

- `price + tax` 程度の単純計算
- 外部依存なし
- 差し替え予定なし

#### 判断

インターフェイスは不要です。

#### 理由

- 抽象化の利益よりコード増加のコストが大きい
- 変更が起きても局所的

### ケース3: ユーザー検索Repository

#### 状況

- DBアクセスを隠したい
- テストではメモリ上実装を使いたい
- 将来ORMやアクセス方式が変わる可能性がある

#### 判断

インターフェイスを使う価値があります。

#### 理由

- インフラ境界を隠せる
- テスト実装を差し込みやすい
- ドメイン側がDB技術に引っ張られにくくなる

### ケース4: `User` Entity

#### 状況

- 業務上の実体そのもの
- DBテーブルとも対応する
- 値や状態を持つ

#### 判断

通常はインターフェイス不要です。

#### 理由

- 役割ではなく具体的な業務概念だから
- 抽象化より実体表現の方が自然だから

### ケース5: CSV出力とPDF出力

#### 状況

- 出力という共通目的はある
- ただし処理内容はかなり異なる
- 共通前処理も少しある

#### 判断

次の2案を比較します。

- 共通前処理が薄いならインターフェイス
- 共通手順が強いなら抽象クラス

#### 見るべき点

- 「出力する」という役割を揃えたいのか
- 「共通手順」を再利用したいのか

---

## 8. Spring Bootで特にインターフェイスが効きやすい場所

### 8-1. 外部連携の窓口

- メール送信
- ストレージアクセス
- 決済API
- 他システム連携

これらは環境差異、障害、テスト差し替えの都合が大きいため、境界として切る価値があります。

### 8-2. ポリシーやルールの差し替えポイント

- 割引ルール
- 料金計算ルール
- 承認ルール
- 採番ルール

「何をするか」は同じでも、「どう決めるか」が変わる場所です。
このようなビジネスルールは、インターフェイス化と相性が良いです。

### 8-3. Repository層

チームの設計方針によりますが、永続化の詳細をドメインロジックから隠したい時に有効です。

ただし、Spring Data JPAではRepository自体がインターフェイスになることも多く、これはフレームワークの仕組みとセットで理解する必要があります。

### 8-4. AOPやプロキシの恩恵を受ける設計

SpringはDIやAOPの都合上、インターフェイスベースのプロキシを使うことがあります。
ただし、これを理由に「何でもインターフェイス化する」のは本末転倒です。

まず設計上の意味があるかを優先してください。

---

## 9. 迷った時の判定フロー

インターフェイスを切るべきか迷ったら、次の順で考えてください。

1. そのクラスは「役割」か、「実体」か。
2. 実装が複数になりうるか。
3. テストで差し替えたいか。
4. 外部連携や変わりやすい技術詳細を隠したいか。
5. 呼び出し側に「できること」だけ見せれば十分か。
6. 抽象化によって、読むコストより利益が上回るか。

### 判定の目安

- 1つでも強く「はい」がある: インターフェイスを検討する価値あり
- 全部が弱い: 具象クラスのままでよい可能性が高い

### 現場向けの短い言い換え

- 変わりやすい側に境界を置く
- 呼び出し側が詳細を知らなくて済むなら切る価値がある
- 将来不明だけを理由に増やさない

---

## 10. 設計レビューで見たい観点

チームでコードレビューする時は、次の観点でインターフェイスの妥当性を見ます。

### 良いインターフェイスの兆候

- 名前が役割を表している
- メソッドが少なく、責務が絞られている
- 実装詳細が契約に漏れていない
- 利用側が自然に読める
- 差し替え理由が説明できる

### 危ないインターフェイスの兆候

- 実装が1つで、増える理由も説明できない
- `Impl` を作ること自体が目的になっている
- メソッドが多すぎて何の役割か分からない
- 契約に具体技術の都合が漏れている
- 利用側が結局実装クラス前提で書かれている

---

## 11. まとめ

インターフェイスは、「オブジェクト指向っぽく見せるための飾り」ではありません。
**変わりやすいものと、安定させたい利用側の間に境界を作るための実用品** です。

そのため、重要なのは「使うこと」ではなく「使う理由があること」です。

- 実装差し替えがある
- 外部依存を隠したい
- テストで偽物に置き換えたい
- チームで契約を先に決めたい

このような条件があるなら、インターフェイスは強い味方になります。
逆に、単純で安定した処理にまで機械的に導入すると、設計はむしろ重くなります。

現場では、**「インターフェイスにするか」ではなく、「どこに境界を置くと変更の波及を減らせるか」** を考えるようにしてください。

---

## 12. Painから見る補助教材

ここまでの本文は、判断基準を整理するために「どう考えるべきか」を中心に説明してきました。
一方で、初学者にとっては **「避けなかった結果、コードがどう傷むのか」** を見た方が理解しやすいことがあります。

そのため、付録として **悪い例と改善後を見比べる比較サンプル** を追加しています。

- [比較サンプル：通知機能で学ぶ「インターフェイスが必要になる痛み」](appendix/interface-notification-comparison/README.md)

このサンプルでは、次の流れをコードで観察できます。

- 最初は具体クラスに直接依存していても自然に見える
- 要件追加で利用側まで修正が波及する
- テストで本物の通知処理を止めにくくなる
- インターフェイス導入後、変更が通知実装側に閉じやすくなる

本文では判断軸を学び、付録では「なぜその判断が必要になったのか」を痛みから確認してください。

---

## 確証をとるための確認問題（第10章）

**【問題1】**  
次のコードでは、`EquipmentService` が `ConsoleNotificationSender` を直接 `new` しています。  
この書き方は最初の要件だけを見ると自然ですが、後からどのような変更に弱くなるでしょうか。2つ以上挙げて説明してください。

```java
public class EquipmentService {
    private final ConsoleNotificationSender notificationSender = new ConsoleNotificationSender();

    public void lendCompleted(String userName, String equipmentName) {
        String message = userName + "さんへの" + equipmentName + "の貸出が完了しました。";
        notificationSender.send(message);
    }
}
```

**【問題2】**  
問題1の通知機能に対して、「本番ではメール通知、開発環境ではコンソール通知、テストでは偽物通知を使いたい」という要件が追加されました。  
この時、インターフェイスを導入すると何が改善されるか、利用側と実装側の責務分離に触れながら説明してください。

**【問題3】**  
次の `PriceCalculator` は単純な計算しかしていません。これをあえて `interface PriceCalculator` と `DefaultPriceCalculator` に分けると、どんな「痛み」が発生しますか。初学者の読みやすさも含めて説明してください。

```java
public class PriceCalculator {
    public int calculateTotal(int price, int tax) {
        return price + tax;
    }
}
```

**【問題4】**  
`User` や `Order` のようなEntityを、通常はインターフェイス化しない方が自然な理由を説明してください。  
特に「役割」と「実体」の違いに触れて答えてください。

**【問題5】**  
次の考え方はなぜ危険でしょうか。  
「将来何かあるかもしれないから、Service は全部 `Interface + Impl` にしておこう」  
変更可能性の見積もりと、コードを読むコストの観点から説明してください。

**【問題6】**  
CSV出力とPDF出力を実装しているチームで、両方とも「データを読む → 整形する → 出力する」という同じ大枠の手順を持っていました。  
この時、インターフェイスより抽象クラスの方が向いている可能性があるのはなぜですか。共通手順と重複コードの観点から説明してください。

**【問題7】**  
`PaymentGateway` というインターフェイスを使っているのに、`OrderService` が `Stripe` 固有のレスポンスコードを直接見て分岐していたとします。  
なぜこの状態は「インターフェイスがあるのに疎結合ではない」と言えるのでしょうか。

**【問題8】**  
外部決済APIを呼び出す処理では、なぜインターフェイス化の価値が高くなりやすいのでしょうか。  
環境差異、テスト、副作用の観点を含めて説明してください。

**【問題9】**  
次の3つのうち、どれにインターフェイスを使う価値が高いかを選び、理由を説明してください。  
また、使わない方がよいものについても理由を述べてください。

1. 備品の状態（AVAILABLE / LENT / BROKEN）
2. 割引ルール
3. 単純な日付文字列整形

**【問題10】**  
次のような新規クラスを作る場面を想定してください。  
「メール送信」「帳票出力」「単純な計算」「Entity」など、性質の違う候補が混在しています。  
インターフェイスを切るか迷った時の判定フローを、自分なりの言葉で3〜5項目に整理してください。

## 自己採点の観点

次の観点に触れられていれば、この章の要点はかなり押さえられています。

- 「インターフェイスは良いものだから使う」のではなく、「変更の波及を減らすために使う」と説明できている
- 使うべきケースだけでなく、使わない方がよいケースも理由付きで説明できている
- 「役割」と「実体」を区別して考えられている
- 疎結合とは、インターフェイスがあることではなく、利用側が実装詳細を知らずに済む状態だと説明できている
- 抽象クラス、具象クラス、enum、ユーティリティメソッドとの使い分けを、痛みや重複の観点で説明できている

逆に、次のような答え方に寄っていたら、もう一度本文を見直す価値があります。

- 「とりあえず全部インターフェイスにするのがよい」と考えている
- 「Serviceには必ずインターフェイスが必要」と決め打ちしている
- 実装が1つしかない単純処理でも、理由なく `Interface + Impl` に分けようとしている
- 契約と実装詳細の境界がどこかを説明できない

## <Next Step>

本章の内容をさらに深めるなら、次のテーマに進むと理解が繋がります。

- **依存性逆転の原則（DIP）**: なぜ上位モジュールが具象に依存しない方がよいのか
- **Strategyパターン**: インターフェイスによる実装切り替えを設計パターンとして捉える
- **Adapterパターン**: 外部APIを自分たちの契約に合わせる実践手法
- **テストダブル（Stub / Mock / Fake）**: 差し替え可能設計をテストでどう活かすか

関連する基礎の復習としては[第2章](02_oop-fundamentals.md)、DIとの関係を再確認するなら[第4章](04_di-and-annotations.md)と[補足：DI詳解](04-1_di-deep-dive.md)を見直してください。

---

← [第9章に戻る](09_bugfix-and-di-hands-on.md) | [目次に戻る](00_curriculum.md)
