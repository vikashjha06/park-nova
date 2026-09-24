package com.smartparking.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "bookings")
public class Booking {

    @Id
    private String id;

    private String userId;
    private String parkingAreaId;
    private String parkingSlotId;

    private String vehicleNumber;
    private VehicleType vehicleType;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private double totalAmount;

    private BookingStatus status;

    /*
     * Normal paid bookings get a limited payment hold.
     * Monthly-pass bookings do not require this value.
     */
    private LocalDateTime paymentExpiresAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Booking() {
    }

    public Booking(
            String userId,
            String parkingAreaId,
            String parkingSlotId,
            String vehicleNumber,
            VehicleType vehicleType,
            LocalDateTime startTime,
            LocalDateTime endTime,
            double totalAmount) {

        this.userId = userId;
        this.parkingAreaId = parkingAreaId;
        this.parkingSlotId = parkingSlotId;
        this.vehicleNumber = vehicleNumber;
        this.vehicleType = vehicleType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalAmount = totalAmount;

        this.status = BookingStatus.PENDING;

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

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

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getPaymentExpiresAt() {
        return paymentExpiresAt;
    }

    public void setPaymentExpiresAt(
            LocalDateTime paymentExpiresAt) {

        this.paymentExpiresAt = paymentExpiresAt;
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