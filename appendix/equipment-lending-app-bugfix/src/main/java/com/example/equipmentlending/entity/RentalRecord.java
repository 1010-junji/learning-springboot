package com.example.equipmentlending.entity;

import java.time.LocalDateTime;

public class RentalRecord {

    private Long id;
    private Long equipmentId;
    private String userName;
    private LocalDateTime rentedAt;
    private LocalDateTime returnedAt;

    public RentalRecord(Long id, Long equipmentId, String userName, LocalDateTime rentedAt, LocalDateTime returnedAt) {
        this.id = id;
        this.equipmentId = equipmentId;
        this.userName = userName;
        this.rentedAt = rentedAt;
        this.returnedAt = returnedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getEquipmentId() {
        return equipmentId;
    }

    public String getUserName() {
        return userName;
    }

    public LocalDateTime getRentedAt() {
        return rentedAt;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }
}
