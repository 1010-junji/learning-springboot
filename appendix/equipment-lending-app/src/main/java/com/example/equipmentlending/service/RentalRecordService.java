package com.example.equipmentlending.service;

import com.example.equipmentlending.dto.RentalRecordResponse;
import com.example.equipmentlending.entity.Equipment;
import com.example.equipmentlending.entity.RentalRecord;
import com.example.equipmentlending.mapper.RentalRecordMapper;
import com.example.equipmentlending.repository.RentalRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RentalRecordService {

    private final RentalRecordRepository rentalRecordRepository;
    private final EquipmentService equipmentService;
    private final RentalRecordMapper rentalRecordMapper;

    public RentalRecordService(
            RentalRecordRepository rentalRecordRepository,
            EquipmentService equipmentService,
            RentalRecordMapper rentalRecordMapper
    ) {
        this.rentalRecordRepository = rentalRecordRepository;
        this.equipmentService = equipmentService;
        this.rentalRecordMapper = rentalRecordMapper;
    }

    public List<RentalRecordResponse> getAllRentalRecords() {
        return rentalRecordRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private RentalRecordResponse toResponse(RentalRecord rentalRecord) {
        Equipment equipment = equipmentService.getEquipmentOrThrow(rentalRecord.getEquipmentId());
        return rentalRecordMapper.toResponse(rentalRecord, equipment.getName());
    }
}
