package com.smartparking.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pricing_plans")
public class PricingPlan {

    @Id
    private String id;

    private String parkingAreaId;

    private VehicleType vehicleType;

    private double hourlyRate;

    private double peakHourlyRate;

    // Legacy field kept for existing MongoDB documents.
    private double monthlyPassPrice;

    // New dynamic monthly-pass discount.
    private double monthlyPassDiscountPercent;

    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PricingPlan() {
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public PricingPlan(
            String parkingAreaId,
            VehicleType vehicleType,
            double hourlyRate,
            double peakHourlyRate,
            double monthlyPassPrice,
            double monthlyPassDiscountPercent) {

        this.parkingAreaId = parkingAreaId;
        this.vehicleType = vehicleType;
        this.hourlyRate = hourlyRate;
        this.peakHourlyRate = peakHourlyRate;
        this.monthlyPassPrice = monthlyPassPrice;
        this.monthlyPassDiscountPercent =
                monthlyPassDiscountPercent;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}