package com.smartparking.dto;

import com.smartparking.model.VehicleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ParkingSlotRequest {

    @NotBlank(message = "Parking area ID is required")
    private String parkingAreaId;

    @NotBlank(message = "Slot number is required")
    private String slotNumber;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    private String floor;

    private String zone;

    private String sensorId;

    public ParkingSlotRequest() {
    }

    public String getParkingAreaId() {
        return parkingAreaId;
    }

    public void setParkingAreaId(String parkingAreaId) {
        this.parkingAreaId = parkingAreaId;
    }

    public String getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(String slotNumber) {
        this.slotNumber = slotNumber;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }
}