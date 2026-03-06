# 第5章 補足：DTOとEntityの分離、Mapperの考え方

第5章では、Controller / Service / Repository / Entity の役割分担を学びました。
ここではその続きとして、実務でほぼ必須になる **「DTOとEntityの分離」** と **「Mapper（変換処理）」** を解説します。

## 1. なぜDTOとEntityを分けるのか

`Entity` は、データベースのテーブル構造に近い「永続化のためのモデル」です。
一方 `DTO` は、APIの入出力に使う「受け渡し専用のモデル」です。

この2つを分ける主な理由は次の3つです。

- **セキュリティ**：Entityの全項目（例: パスワードハッシュ、内部フラグ）をそのまま外部に出さないため
- **変更耐性**：DB構造が変わっても、APIの入出力仕様を必要以上に壊さないため
- **責務分離**：DB都合の構造と、画面/API都合の構造を分けて保守しやすくするため

## 2. アンチパターン（Entityをそのまま返す）

```java
// 悪い例: Entity をそのまま返してしまう
@GetMapping("/users/{id}")
public UserEntity getUser(@PathVariable Long id) {
    return userRepository.findById(id).orElseThrow();
}
```

この書き方だと、意図しない項目がAPIレスポンスに含まれたり、Entity変更がAPI破壊に直結したりします。

## 3. 推奨パターン（DTOに詰め替えて返す）

```java
// 返却用DTO
public class UserResponseDto {
    private Long id;
    private String name;

    public UserResponseDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
```

```java
// Entity
public class UserEntity {
    private Long id;
    private String name;
    private String passwordHash; // 外部には出したくない

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPasswordHash() { return passwordHash; }
}
```

## 4. Mapper（変換処理）をどこに置くか

DTOとEntityを分けると、必ず「詰め替え処理」が必要になります。
この変換をControllerやServiceに散らばらせると、同じコードが増えます。

そこで、変換責務を1箇所に寄せるのがMapperです。

```java
public class UserMapper {
    public static UserResponseDto toResponseDto(UserEntity entity) {
        return new UserResponseDto(entity.getId(), entity.getName());
    }
}
```

```java
// Service例
public UserResponseDto findUser(Long id) {
    UserEntity entity = userRepository.findById(id).orElseThrow();
    return UserMapper.toResponseDto(entity);
}
```

## 5. どこまで厳密に分けるべきか

最初は「とりあえず分ける」だけで十分です。以下の順番で進めると実務で使いやすくなります。

1. 返却用DTOだけ先に分ける（レスポンス漏えいを防ぐ）
2. 作成・更新用DTOも分ける（入力バリデーションを整理する）
3. Mapperをクラス化して共通化する（重複コード削減）
4. 必要なら MapStruct などの自動マッピング導入を検討する

## 6. 最低限の実務ルール（迷ったとき用）

- Controllerの入出力型はDTOを使う
- Repositoryの戻りはEntityでよい（ServiceでDTOに変換）
- `password`, `internalFlag`, `createdAt` などは公開要否を必ず明示する
- 変換コードはMapperに寄せる（Controller直書きしない）

---

← [第5章に戻る](05_springboot-structure.md) | → [第6章へ進む](06_error-reading-and-qa.md)
