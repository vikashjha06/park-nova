package com.smartparking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartparking.model.MonthlyPass;
import com.smartparking.service.AdminActivityLogService;
import com.smartparking.service.MonthlyPassService;

@RestController
@RequestMapping("/api/admin/monthly-passes")
public class AdminMonthlyPassCancellationController {

    private final MonthlyPassService monthlyPassService;
    private final AdminActivityLogService activityLogService;

    public AdminMonthlyPassCancellationController(
            MonthlyPassService monthlyPassService,
            AdminActivityLogService activityLogService) {
        this.monthlyPassService = monthlyPassService;
        this.activityLogService = activityLogService;
    }

    @PatchMapping("/{passId}/cancel")
    public ResponseEntity<MonthlyPass> cancelActivePass(
            @PathVariable String passId,
            Authentication authentication) {

        MonthlyPass cancelled =
                monthlyPassService
                        .cancelActiveMonthlyPassAsAdmin(passId);

        activityLogService.log(
                null,
                authentication == null
                        ? null
                        : authentication.getName(),
                "MONTHLY_PASS_CANCELLED",
                "MONTHLY_PASS",
                cancelled.getId(),
                "Active monthly pass cancelled by administrator"
        );

        return ResponseEntity.ok(cancelled);
    }
}