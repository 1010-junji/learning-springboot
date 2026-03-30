package com.example.equipmentlending.mapper;

import com.example.equipmentlending.dto.EquipmentResponse;
import com.example.equipmentlending.entity.Equipment;
import org.springframework.stereotype.Component;

@Component
public class EquipmentMapper {

    public EquipmentResponse toResponse(Equipment equipment) {
        return new EquipmentResponse(
                equipment.getId(),
                equipment.getName(),
                equipment.getCategory(),
                equipment.getStatus().name()
        );
    }
}
