package com.smartparking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartparking.dto.PricingPlanRequest;
import com.smartparking.model.PricingPlan;
import com.smartparking.service.PricingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/pricing")
public class AdminPricingController {

    private final PricingService pricingService;

    public AdminPricingController(
            PricingService pricingService) {

        this.pricingService = pricingService;
    }

    // CREATE PRICING PLAN
    @PostMapping
    public ResponseEntity<PricingPlan> createPricingPlan(
            @Valid @RequestBody PricingPlanRequest request) {

        PricingPlan pricingPlan =
                pricingService.createPricingPlan(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pricingPlan);
    }

    // GET PRICING PLANS BY PARKING AREA
    @GetMapping("/area/{parkingAreaId}")
    public ResponseEntity<List<PricingPlan>> getPricingPlans(
            @PathVariable String parkingAreaId) {

        return ResponseEntity.ok(
                pricingService.getPricingPlans(
                        parkingAreaId
                )
        );
    }

    // UPDATE PRICING PLAN
    @PutMapping("/{id}")
    public ResponseEntity<PricingPlan> updatePricingPlan(
            @PathVariable String id,
            @Valid @RequestBody PricingPlanRequest request) {

        return ResponseEntity.ok(
                pricingService.updatePricingPlan(
                        id,
                        request
                )
        );
    }

    // ENABLE / DISABLE PRICING PLAN
    @PatchMapping("/{id}/active")
    public ResponseEntity<PricingPlan> setPricingPlanActive(
            @PathVariable String id,
            @RequestParam boolean active) {

        return ResponseEntity.ok(
                pricingService.setPricingPlanActive(
                        id,
                        active
                )
        );
    }
}