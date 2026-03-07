package com.example.equipmentlending.controller;

import com.example.equipmentlending.dto.RentalRecordResponse;
import com.example.equipmentlending.service.RentalRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rental-records")
public class RentalRecordController {

    private final RentalRecordService rentalRecordService;

    public RentalRecordController(RentalRecordService rentalRecordService) {
        this.rentalRecordService = rentalRecordService;
    }

    @GetMapping
    public List<RentalRecordResponse> getRentalRecords() {
        return rentalRecordService.getAllRentalRecords();
    }
}
