package com.example.equipmentlending.controller;

import com.example.equipmentlending.dto.CreateEquipmentRequest;
import com.example.equipmentlending.dto.EquipmentResponse;
import com.example.equipmentlending.dto.LendEquipmentRequest;
import com.example.equipmentlending.service.EquipmentCategoryAnalyzer;
import com.example.equipmentlending.service.EquipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/equipments")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public List<EquipmentResponse> getEquipments() {
        return equipmentService.getAllEquipments();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    // BUG: @RequestBody が欠落しているため、POST送信されたJSONデータがマッピングされない（nullになる）
    public EquipmentResponse createEquipment(CreateEquipmentRequest request) {
        return equipmentService.createEquipment(request);
    }

    @PostMapping("/{id}/lend")
    public EquipmentResponse lendEquipment(@PathVariable("id") Long equipmentId, @RequestBody LendEquipmentRequest request) {
        return equipmentService.lendEquipment(equipmentId, request);
    }

    @PostMapping("/{id}/return")
    public EquipmentResponse returnEquipment(@PathVariable("id") Long equipmentId) {
        return equipmentService.returnEquipment(equipmentId);
    }

    // --- 応用編用のAPI ---

    @GetMapping("/categories/count")
    public String getCategoryCount() {
        // BUG: ここでSpringのDIを使わず「new」でインスタンス化してしまっているため、
        // Analyzerの中のRepositoryが注入（Autowired）されずNullになっていて例外が発生する
        EquipmentCategoryAnalyzer analyzer = new EquipmentCategoryAnalyzer();
        long count = analyzer.analyzeCategoryCount();
        return "現在の備品のカテゴリ数は " + count + " 種類です。";
    }
}
