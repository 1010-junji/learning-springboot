package com.example.equipmentlending.dto;

public class CreateEquipmentRequest {

    private String name;
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
