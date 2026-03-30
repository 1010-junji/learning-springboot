package com.example.equipmentlending.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateEquipmentRequest {

    // BUG: このアノテーションは機能していますが、Controllerに @Validated (@Valid) がないため実行時に無視されます
    @NotBlank(message = "備品名は必須です")
    @Size(max = 50, message = "備品名は50文字以内で入力してください")
    private String name;

    @NotBlank(message = "カテゴリは必須です")
    private String category;

    public CreateEquipmentRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
