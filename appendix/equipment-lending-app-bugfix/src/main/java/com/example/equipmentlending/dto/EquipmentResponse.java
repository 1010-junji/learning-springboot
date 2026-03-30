package com.example.equipmentlending.dto;

public class EquipmentResponse {

    private Long id;
    private String name;
    private String category;
    private String status;

    public EquipmentResponse(Long id, String name, String category, String status) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getStatus() {
        return status;
    }
}
