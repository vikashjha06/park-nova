package com.smartparking.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "parking_slots")
@CompoundIndex(
        name = "parking_area_slot_unique",
        def = "{'parkingAreaId': 1, 'slotNumber': 1}",
        unique = true
)
public class ParkingSlot {

    @Id
    private String id;

    private String parkingAreaId;

    private String slotNumber;

    private VehicleType vehicleType;

    private ParkingSlotStatus status;

    private String floor;

    private String zone;

    private String sensorId;

    private boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public ParkingSlot() {
    }

    public ParkingSlot(
            String parkingAreaId,
            String slotNumber,
            VehicleType vehicleType,
            String floor,
            String zone) {

        this.parkingAreaId = parkingAreaId;
        this.slotNumber = slotNumber;
        this.vehicleType = vehicleType;

        this.floor = floor;
        this.zone = zone;

        this.status = ParkingSlotStatus.AVAILABLE;
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

    public ParkingSlotStatus getStatus() {
        return status;
    }

    public void setStatus(ParkingSlotStatus status) {
        this.status = status;
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