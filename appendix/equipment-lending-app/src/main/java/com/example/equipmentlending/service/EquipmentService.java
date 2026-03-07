package com.example.equipmentlending.service;

import com.example.equipmentlending.dto.CreateEquipmentRequest;
import com.example.equipmentlending.dto.EquipmentResponse;
import com.example.equipmentlending.dto.LendEquipmentRequest;
import com.example.equipmentlending.entity.Equipment;
import com.example.equipmentlending.entity.EquipmentStatus;
import com.example.equipmentlending.entity.RentalRecord;
import com.example.equipmentlending.exception.BusinessRuleViolationException;
import com.example.equipmentlending.exception.InvalidRequestException;
import com.example.equipmentlending.exception.ResourceNotFoundException;
import com.example.equipmentlending.mapper.EquipmentMapper;
import com.example.equipmentlending.repository.EquipmentRepository;
import com.example.equipmentlending.repository.RentalRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final RentalRecordRepository rentalRecordRepository;
    private final EquipmentMapper equipmentMapper;

    public EquipmentService(
            EquipmentRepository equipmentRepository,
            RentalRecordRepository rentalRecordRepository,
            EquipmentMapper equipmentMapper
    ) {
        this.equipmentRepository = equipmentRepository;
        this.rentalRecordRepository = rentalRecordRepository;
        this.equipmentMapper = equipmentMapper;
    }

    public List<EquipmentResponse> getAllEquipments() {
        return equipmentRepository.findAll().stream()
                .map(equipmentMapper::toResponse)
                .toList();
    }

    @Transactional
    public EquipmentResponse createEquipment(CreateEquipmentRequest request) {
        validateRequest(request, "備品登録リクエストが空です。");
        validateText(request.getName(), "備品名は必須です。");
        validateText(request.getCategory(), "カテゴリは必須です。");

        Equipment createdEquipment = equipmentRepository.save(
                request.getName().trim(),
                request.getCategory().trim()
        );
        return equipmentMapper.toResponse(createdEquipment);
    }

    @Transactional
    public EquipmentResponse lendEquipment(Long equipmentId, LendEquipmentRequest request) {
        validateRequest(request, "貸出リクエストが空です。");
        validateText(request.getUserName(), "貸出者名は必須です。");

        Equipment equipment = getEquipmentOrThrow(equipmentId);
        if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
            throw new BusinessRuleViolationException("この備品はすでに貸出中です。");
        }

        rentalRecordRepository.save(equipmentId, request.getUserName().trim(), LocalDateTime.now());
        equipmentRepository.updateStatus(equipmentId, EquipmentStatus.LENT);
        return equipmentMapper.toResponse(getEquipmentOrThrow(equipmentId));
    }

    @Transactional
    public EquipmentResponse returnEquipment(Long equipmentId) {
        Equipment equipment = getEquipmentOrThrow(equipmentId);
        if (equipment.getStatus() != EquipmentStatus.LENT) {
            throw new BusinessRuleViolationException("この備品は貸出中ではないため返却できません。");
        }

        RentalRecord activeRentalRecord = rentalRecordRepository.findActiveByEquipmentId(equipmentId)
                .orElseThrow(() -> new BusinessRuleViolationException("貸出履歴が見つからないため返却できません。"));

        rentalRecordRepository.markReturned(activeRentalRecord.getId(), LocalDateTime.now());
        equipmentRepository.updateStatus(equipmentId, EquipmentStatus.AVAILABLE);
        return equipmentMapper.toResponse(getEquipmentOrThrow(equipmentId));
    }

    public Equipment getEquipmentOrThrow(Long equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("ID " + equipmentId + " の備品は存在しません。"));
    }

    private void validateRequest(Object request, String message) {
        if (request == null) {
            throw new InvalidRequestException(message);
        }
    }

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(message);
        }
    }
}
