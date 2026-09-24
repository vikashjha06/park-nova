package com.smartparking.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.smartparking.model.VehicleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MonthlyPassRequest {

    @NotBlank
    private String parkingAreaId;

    @NotBlank
    private String parkingSlotId;

    @NotNull
    private VehicleType vehicleType;

    @NotBlank
    private String vehicleNumber;

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

    public String getParkingSlotId() {
        return parkingSlotId;
    }

    public void setParkingSlotId(String parkingSlotId) {
        this.parkingSlotId = parkingSlotId;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
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