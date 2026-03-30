package com.example.equipmentlending.service;

import com.example.equipmentlending.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EquipmentCategoryAnalyzer {

    // DIで自動注入される想定
    @Autowired
    private EquipmentRepository equipmentRepository;

    public long analyzeCategoryCount() {
        // BUG: equipmentRepositoryが注入されていない（null）状態で呼び出されるとNullPointerExceptionが発生する
        return equipmentRepository.findAll().stream()
                .map(e -> e.getCategory())
                .distinct()
                .count();
    }
}