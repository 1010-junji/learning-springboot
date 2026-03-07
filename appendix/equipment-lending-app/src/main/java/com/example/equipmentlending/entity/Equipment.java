package com.example.equipmentlending.entity;

public class Equipment {

    private Long id;
    private String name;
    private String category;
    private EquipmentStatus status;

    public Equipment(Long id, String name, String category, EquipmentStatus status) {
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

    public EquipmentStatus getStatus() {
        return status;
    }
}
