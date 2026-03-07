package com.example.equipmentlending.dto;

import java.time.LocalDateTime;

public class RentalRecordResponse {

    private Long id;
    private Long equipmentId;
    private String equipmentName;
    private String userName;
    private LocalDateTime rentedAt;
    private LocalDateTime returnedAt;

    public RentalRecordResponse(
            Long id,
            Long equipmentId,
            String equipmentName,
            String userName,
            LocalDateTime rentedAt,
            LocalDateTime returnedAt
    ) {
        this.id = id;
        this.equipmentId = equipmentId;
        this.equipmentName = equipmentName;
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

    public String getEquipmentName() {
        return equipmentName;
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
