package com.example.equipmentlending.mapper;

import com.example.equipmentlending.dto.RentalRecordResponse;
import com.example.equipmentlending.entity.RentalRecord;
import org.springframework.stereotype.Component;

@Component
public class RentalRecordMapper {

    public RentalRecordResponse toResponse(RentalRecord rentalRecord, String equipmentName) {
        return new RentalRecordResponse(
                rentalRecord.getId(),
                rentalRecord.getEquipmentId(),
                equipmentName,
                rentalRecord.getUserName(),
                rentalRecord.getRentedAt(),
                rentalRecord.getReturnedAt()
        );
    }
}
