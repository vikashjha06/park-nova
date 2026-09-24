package com.smartparking.dto;

import com.smartparking.model.VehicleType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PricingPlanRequest {

    @NotBlank(message = "Parking area ID is required")
    private String parkingAreaId;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @DecimalMin(value = "0.0", message = "Hourly rate cannot be negative")
    private double hourlyRate;

    @DecimalMin(value = "0.0", message = "Peak hourly rate cannot be negative")
    private double peakHourlyRate;

    // Legacy field retained for compatibility.
    @DecimalMin(value = "0.0", message = "Monthly pass price cannot be negative")
    private double monthlyPassPrice;

    @DecimalMin(value = "0.0", message = "Monthly pass discount cannot be negative")
    @DecimalMax(value = "100.0", message = "Monthly pass discount cannot exceed 100")
    private double monthlyPassDiscountPercent;

    public PricingPlanRequest() {
    }

    public String getParkingAreaId() {
        return parkingAreaId;
    }

    public void setParkingAreaId(String parkingAreaId) {
        this.parkingAreaId = parkingAreaId;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public double getPeakHourlyRate() {
        return peakHourlyRate;
    }

    public void setPeakHourlyRate(double peakHourlyRate) {
        this.peakHourlyRate = peakHourlyRate;
    }

    public double getMonthlyPassPrice() {
        return monthlyPassPrice;
    }

    public void setMonthlyPassPrice(double monthlyPassPrice) {
        this.monthlyPassPrice = monthlyPassPrice;
    }

    public double getMonthlyPassDiscountPercent() {
        return monthlyPassDiscountPercent;
    }

    public void setMonthlyPassDiscountPercent(
            double monthlyPassDiscountPercent) {

        this.monthlyPassDiscountPercent =
                monthlyPassDiscountPercent;
    }
}