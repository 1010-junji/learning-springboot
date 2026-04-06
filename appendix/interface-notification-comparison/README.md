# 比較サンプル：通知機能で学ぶ「インターフェイスが必要になる痛み」

この付録は、**インターフェイスの完成形を先に見せる** のではなく、
**インターフェイスがないまま変更要求を受けた時に何がつらくなるか** を観察するための比較サンプルです。

## このサンプルで見たいこと

- 最初は「具体クラスに直接依存する実装」が自然に見えること
- 要件変更が入ると、利用側まで修正が波及すること
- テスト時に本物の通知処理が混ざりやすいこと
- インターフェイス導入後は、変更が通知実装側に閉じやすくなること

## 題材

備品貸出完了時の通知です。

### 最初の要件

- 貸出完了時にコンソールへ通知する

### 後から追加される要件

- 本番ではメール通知に切り替えたい
- 開発環境ではコンソール通知のままにしたい
- テストでは本物の通知を送らず、何が送られたかだけ確認したい

このような要件は、実務で非常によく起きます。

---

## 1. Before: 最初は自然に見える実装

まずは、最初の要件だけを満たす素直なコードです。

```java
// bad/EquipmentService.java
public class EquipmentService {
    private final ConsoleNotificationService notificationService = new ConsoleNotificationService();

    public void lendEquipment(String userName, String equipmentName) {
        System.out.println(equipmentName + " を " + userName + " に貸し出しました。");

        String message = userName + "さんへの" + equipmentName + "の貸出が完了しました。";
        notificationService.send(message);
    }
}
```

```java
// bad/ConsoleNotificationService.java
public class ConsoleNotificationService {
    public void send(String message) {
        System.out.println("[Console] " + message);
    }
}
```

### この時点では自然に見える理由

- 実装が1つしかない
- すぐ動く
- クラス数が少なく、初心者にも読みやすい

この段階では、「わざわざインターフェイスを作る必要があるのか」と感じるのが普通です。

---

## 2. Pain: 要件追加で何がつらくなるか

ここで次の要件が入ります。

- 本番ではメール通知に切り替えたい
- 開発環境ではコンソール通知のままにしたい
- テストでは本物の通知を送らず、何が送られたかだけ確認したい

この時、具体クラスに直結した設計だと、`EquipmentService` 側に変更が波及しやすくなります。

---

### Pain 1: 実装追加で利用側まで修正が波及する

通知方式をメールに変えたくなっただけなのに、通知の利用側である `EquipmentService` を書き換える必要が出ます。

```java
// bad/EquipmentService.java
public class EquipmentService {
    private final EmailNotificationService notificationService = new EmailNotificationService();

    public void lendEquipment(String userName, String equipmentName) {
        System.out.println(equipmentName + " を " + userName + " に貸し出しました。");

        String message = userName + "さんへの" + equipmentName + "の貸出が完了しました。";
        notificationService.send(message);
    }
}
```

```java
// bad/EmailNotificationService.java
public class EmailNotificationService {
    public void send(String message) {
        System.out.println("[Email] " + message);
    }
}
```

本来 `EquipmentService` が知りたいのは「通知できること」だけのはずです。
それなのに、「メールなのかコンソールなのか」という詳細まで知る必要がある状態になっています。

### Pain 2: 環境ごとの切り替えが業務ロジックに混ざる

さらに開発環境と本番環境で切り替えたいとなると、ありがちな悪化例は次です。

```java
// bad/ChangeRequestExample.java
public class EquipmentService {
    public void lendEquipment(String userName, String equipmentName, boolean production) {
        System.out.println(equipmentName + " を " + userName + " に貸し出しました。");

        String message = userName + "さんへの" + equipmentName + "の貸出が完了しました。";

        if (production) {
            new EmailNotificationService().send(message);
        } else {
            new ConsoleNotificationService().send(message);
        }
    }
}
```

こうなると、通知方式の都合が業務処理に混ざります。
`EquipmentService` の責務は「貸出処理」なのに、「どの通知方式を使うか」まで背負い始めています。

### Pain 3: テストで副作用を止めにくい

本物の通知クラスを直接使っているため、テストで「送信したことだけ確認したい」という要求に弱くなります。
テストしたいのは貸出処理なのに、通知方式まで巻き込まれやすくなります。

---

## 3. After: インターフェイスで境界を切った後

ここで発想を切り替えます。
`EquipmentService` が本当に必要としているのは、「メール送信クラス」でも「コンソール送信クラス」でもなく、**通知できること** です。

その契約だけをインターフェイスとして切り出します。

```java
// better/NotificationSender.java
public interface NotificationSender {
    void send(String message);
}
```

```java
// better/EquipmentService.java
public class EquipmentService {
    private final NotificationSender notificationSender;

    public EquipmentService(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    public void lendEquipment(String userName, String equipmentName) {
        System.out.println(equipmentName + " を " + userName + " に貸し出しました。");

        String message = userName + "さんへの" + equipmentName + "の貸出が完了しました。";
        notificationSender.send(message);
    }
}
```

```java
// better/ConsoleNotificationSender.java
public class ConsoleNotificationSender implements NotificationSender {
    @Override
    public void send(String message) {
        System.out.println("[Console] " + message);
    }
}
```

```java
// better/EmailNotificationSender.java
public class EmailNotificationSender implements NotificationSender {
    @Override
    public void send(String message) {
        System.out.println("[Email] " + message);
    }
}
```

### 改善点

- `EquipmentService` は「通知できること」だけ知ればよい
- コンソール通知、メール通知、テスト用偽物通知を差し替えられる
- 通知方式の追加時も、利用側の修正を減らせる

この変更によって、`EquipmentService` は通知の実装詳細から切り離されました。
通知方式の追加や変更があっても、貸出処理のコード自体を巻き込みにくくなります。

### 何が変わったか

リファクタリング前:

- 業務ロジックが通知方式の具体クラスを知っている
- 差し替えのたびに利用側へ影響が出る

リファクタリング後:

- 業務ロジックは契約だけ知っている
- 変更は主に通知実装側へ閉じる

---

## 4. テストでは何が楽になるか

テストでは、本物のメール送信やコンソール出力を避けて、送信内容だけ確認したいことがあります。
その時は次のような偽物実装を使えます。

```java
// better/FakeNotificationSender.java
import java.util.ArrayList;
import java.util.List;

public class FakeNotificationSender implements NotificationSender {
    private final List<String> sentMessages = new ArrayList<>();

    @Override
    public void send(String message) {
        sentMessages.add(message);
    }

    public List<String> getSentMessages() {
        return sentMessages;
    }
}
```

この偽物を `EquipmentService` に渡せば、

- 実際の通知を飛ばさずに済む
- 何件送られたか確認できる
- メッセージ内容だけを検証できる

という状態を作れます。

つまり、**テストしたい対象を通知方式の副作用から切り離せる** ようになります。

---

## 5. このサンプルで得てほしい判断基準

- 実装が1つでも、後から増えやすい境界ならインターフェイスを検討する
- 外部通知のように副作用がある処理は、差し替え可能にしておく価値が高い
- 逆に、単純計算のような安定した処理まで機械的に抽象化する必要はない

この比較サンプルは、「インターフェイスは最初から神聖な正解だから使う」のではなく、
**痛みを局所化するための道具** だと理解するために使ってください。

## 6. 補足

比較しやすいように文中へ Before / Pain / After を埋め込みましたが、補助的に `bad` と `better` のコード断片も同じフォルダに残しています。
必要ならファイル単位でも確認できますが、まずはこの README を上から順に読むのを優先してください。
