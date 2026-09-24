package com.smartparking.dto;

import com.smartparking.model.RefundDestination;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RefundRequest {

    @NotBlank(message = "Refund reason is required")
    private String reason;

    @NotNull(message = "Refund destination is required")
    private RefundDestination destination;

    public RefundRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(
            String reason) {
        this.reason = reason;
    }

    public RefundDestination getDestination() {
        return destination;
    }

    public void setDestination(
            RefundDestination destination) {
        this.destination = destination;
    }
}