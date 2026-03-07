package com.example.equipmentlending.controller;

import com.example.equipmentlending.dto.CreateEquipmentRequest;
import com.example.equipmentlending.dto.EquipmentResponse;
import com.example.equipmentlending.dto.LendEquipmentRequest;
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
    public EquipmentResponse createEquipment(@RequestBody CreateEquipmentRequest request) {
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
}
