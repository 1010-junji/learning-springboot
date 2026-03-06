# 第4章 補足：DIの種明かし（どのクラスが注入されるのか？）

Controllerのコンストラクタ（初期設定メソッド）に、以下のように書かれていました。

```java
// Controllerの中のコード
public UserController(TaxService taxService) {
    this.taxService = taxService;
}
```

ご推察の通り、**Springはコンストラクタの「引数の型（ここでは `TaxService`）」をヒントにして、自分が管理しているコンテナ（保管庫）の中から「これを渡せばいいんだな！」と探し出してDI（注入）を行います。**

しかし、ここで第2章で学んだ **「インターフェース（約束事・メニュー表）」** が超重要な役割を果たします。

## 1. 準備：インターフェースと2つの実装クラス

実は、`TaxService` は具体的な処理が書かれていない「インターフェース」です。
そして、そのルールに従って作られた（`implements`された）「8%版」と「10%版」の2つの具体的なクラスが存在しています。

**【大元のルール（インターフェース）】**

```java
// 処理の中身はない。「calculateという機能を持て」という約束だけ。
public interface TaxService {
    int calculate(int amount);
}
```

**【8%版のクラス】**

```java
import org.springframework.stereotype.Service;

@Service // Springさん、これを管理（new）しておいて！
public class TaxService8Percent implements TaxService {
    @Override
    public int calculate(int amount) {
        return (int)(amount * 1.08); // 8%の計算
    }
}
```

**【10%版のクラス（後から追加した）】**

```java
// まだアノテーションはつけていない状態
public class TaxService10Percent implements TaxService {
    @Override
    public int calculate(int amount) {
        return (int)(amount * 1.10); // 10%の計算
    }
}
```

## 2. 開発者はどこを修正するのか？（切り替えの瞬間）

現在、Springのコンテナには `@Service` のふせんが貼られた `TaxService8Percent` だけが保管されています。
そのため、SpringはControllerの `TaxService` という引数を見た時、 **「お、TaxServiceのルールを守っている（implementsしている）クラスは、コンテナの中に8%版が1つだけあるな。これを渡そう！」** と迷わず注入してくれます。

では、**「明日から10%に切り替えて！」** と言われた時、開発者は具体的にどこをどう修正するのでしょうか。
方法は主に2つあります。どちらも **「Controllerのコードは一切触らず、Serviceのアノテーション（ふせん）だけを貼り替える」** という魔法です。

### 切り替えパターンA：古いふせんを剥がし、新しいクラスに貼る（一番シンプル）

開発者は、8%版のファイルを開いて `@Service` を消し、10%版のファイルを開いて `@Service` を書き込みます。

**【修正後】**

```java
// 8%版（@Service を消した）
// これでSpringの管理対象から外れる（コンテナに入らない）
public class TaxService8Percent implements TaxService { ... }

// ----------------------------------------------------

// 10%版（@Service を新しく書いた！）
@Service // Springさん、明日からこっちを管理（new）して！
public class TaxService10Percent implements TaxService { ... }
```

たったこれだけです。
システムを再起動すると、Springのコンテナには「10%版」だけが入ります。Controllerは相変わらず「`TaxService`をください」と言っているので、Springは「今は10%版しかないので、これを渡しますね」と自動で注入します。
**50個のControllerがあっても、この2ファイルのふせん（アノテーション）を貼り替えるだけで、システム全体の計算が10%に切り替わります。**

### 切り替えパターンB：「こっちを優先して！」というふせん（`@Primary`）を貼る

「8%版の処理は、過去のデータの計算用に残しておきたいので `@Service` は消したくない」という場合があります。
しかし、両方に `@Service` をつけてしまうと、Springは「あれ？8%版と10%版、2つあるぞ？どっちをControllerに渡せばいいか分からない！」とパニックになり、起動エラーを起こします。

そこで、新しい10%版に **「迷ったらこっちを優先して（`@Primary`）」** という特別なアノテーションを追記します。

**【修正後】**

```java
// 8%版（@Service は残したまま）
@Service
public class TaxService8Percent implements TaxService { ... }

// ----------------------------------------------------

// 10%版（@Service と @Primary をつけた！）
@Service
@Primary // Springさん、複数の候補があったら絶対にこっちを優先して渡して！
public class TaxService10Percent implements TaxService { ... }
```

これだけで切り替え完了です。
SpringはControllerに注入する際、「2つあるけど、10%版に `@Primary` がついてるからこっちを渡そう」と判断します。

---

## 3. `@Primary` が優先されないケースはあるのか？

あります。`@Primary` は「候補が複数ある時のデフォルト優先ルール」であって、常に絶対ではありません。

### 優先されない代表ケース

1. **`@Qualifier` を注入側に書いた場合**
    - 例：`@Qualifier("taxService8Percent")` を指定すると、その指定が最優先になります。
2. **名前指定の注入を使う場合（`@Resource(name = "...")` など）**
    - 型よりも名前で解決されるため、`@Primary` は効きません。
3. **`List<TaxService>` や `Map<String, TaxService>` で受ける場合**
    - 1つに絞らず複数候補を受け取るため、`@Primary` の出番がありません。
4. **`@Primary` を複数クラスに付けた場合**
    - 優先候補が1つに定まらず、起動時エラーになります。

### 「あえて優先させない」書き方

```java
@RestController
public class UserController {

     private final TaxService taxService;

     // @Primary が付いていても、Qualifier があればこちらを優先
     public UserController(@Qualifier("taxService8Percent") TaxService taxService) {
          this.taxService = taxService;
     }
}
```

つまり実務では、**普段は `@Primary` でデフォルトを決め、必要な箇所だけ `@Qualifier` で明示指定する** という使い分けがよく採用されます。

---

## 4. 結論：DI最大のメリット

「開発者はどこを設定しなおすのか？」の答えは、 **「新しいクラスを作り、そこに `@Service`（や `@Primary`）というアノテーションをつけるだけ」** です。

もしDIを使っていなければ、あなたは50個のControllerのファイルを開き、50箇所の `new TaxService8Percent()` を `new TaxService10Percent()` に書き換えるという、バグを生み出しやすい手作業を強いられていました。

DIとアノテーションを使うことで、**「使う側（Controller）」と「作られる側（Service）」の結びつきが、直接の指名（new）から、Springを介した「インターフェース（条件）によるマッチング」に変わる** のです。これが変更に極めて強いシステムの正体です。

---

← [第4章に戻る](04_di-and-annotations.md) &nbsp;|&nbsp; → [第5章へ進む](05_springboot-structure.md)
