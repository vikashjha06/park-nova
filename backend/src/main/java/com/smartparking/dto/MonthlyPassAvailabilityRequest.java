package com.smartparking.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.smartparking.model.VehicleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MonthlyPassAvailabilityRequest {

    @NotBlank
    private String parkingAreaId;

    @NotNull
    private VehicleType vehicleType;

    @NotNull
    private LocalDate passStartDate;

    @NotNull
    private LocalTime dailyStartTime;

    @NotNull
    private LocalTime dailyEndTime;

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

    public LocalDate getPassStartDate() {
        return passStartDate;
    }

    public void setPassStartDate(LocalDate passStartDate) {
        this.passStartDate = passStartDate;
    }

    public LocalTime getDailyStartTime() {
        return dailyStartTime;
    }

    public void setDailyStartTime(LocalTime dailyStartTime) {
        this.dailyStartTime = dailyStartTime;
    }

    public LocalTime getDailyEndTime() {
        return dailyEndTime;
    }

    public void setDailyEndTime(LocalTime dailyEndTime) {
        this.dailyEndTime = dailyEndTime;
    }
}