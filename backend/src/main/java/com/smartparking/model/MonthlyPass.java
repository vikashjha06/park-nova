package com.smartparking.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "monthly_passes")
public class MonthlyPass {

    @Id
    private String id;

    private String userId;
    private String parkingAreaId;
    private String parkingSlotId;

    private VehicleType vehicleType;
    private String vehicleNumber;

    private LocalTime dailyStartTime;
    private LocalTime dailyEndTime;

    private double dailyHours;

    // Pricing snapshot at purchase time.
    private double hourlyRateAtPurchase;
    private double discountPercent;
    private double normalMonthlyPrice;
    private double pricePaid;

    private LocalDateTime startDate;
    private LocalDateTime expiryDate;

    private MonthlyPassStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MonthlyPass() {
    }

    public MonthlyPass(
            String userId,
            String parkingAreaId,
            String parkingSlotId,
            VehicleType vehicleType,
            String vehicleNumber,
            LocalTime dailyStartTime,
            LocalTime dailyEndTime,
            double dailyHours,
            double hourlyRateAtPurchase,
            double discountPercent,
            double normalMonthlyPrice,
            double pricePaid,
            LocalDateTime startDate,
            LocalDateTime expiryDate) {

        this.userId = userId;
        this.parkingAreaId = parkingAreaId;
        this.parkingSlotId = parkingSlotId;
        this.vehicleType = vehicleType;
        this.vehicleNumber = vehicleNumber;
        this.dailyStartTime = dailyStartTime;
        this.dailyEndTime = dailyEndTime;
        this.dailyHours = dailyHours;
        this.hourlyRateAtPurchase = hourlyRateAtPurchase;
        this.discountPercent = discountPercent;
        this.normalMonthlyPrice = normalMonthlyPrice;
        this.pricePaid = pricePaid;
        this.startDate = startDate;
        this.expiryDate = expiryDate;
        this.status = MonthlyPassStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getParkingAreaId() { return parkingAreaId; }
    public void setParkingAreaId(String parkingAreaId) { this.parkingAreaId = parkingAreaId; }

    public String getParkingSlotId() { return parkingSlotId; }
    public void setParkingSlotId(String parkingSlotId) { this.parkingSlotId = parkingSlotId; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public LocalTime getDailyStartTime() { return dailyStartTime; }
    public void setDailyStartTime(LocalTime dailyStartTime) { this.dailyStartTime = dailyStartTime; }

    public LocalTime getDailyEndTime() { return dailyEndTime; }
    public void setDailyEndTime(LocalTime dailyEndTime) { this.dailyEndTime = dailyEndTime; }

    public double getDailyHours() { return dailyHours; }
    public void setDailyHours(double dailyHours) { this.dailyHours = dailyHours; }

    public double getHourlyRateAtPurchase() { return hourlyRateAtPurchase; }
    public void setHourlyRateAtPurchase(double hourlyRateAtPurchase) { this.hourlyRateAtPurchase = hourlyRateAtPurchase; }

    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }

    public double getNormalMonthlyPrice() { return normalMonthlyPrice; }
    public void setNormalMonthlyPrice(double normalMonthlyPrice) { this.normalMonthlyPrice = normalMonthlyPrice; }

    public double getPricePaid() { return pricePaid; }
    public void setPricePaid(double pricePaid) { this.pricePaid = pricePaid; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public MonthlyPassStatus getStatus() { return status; }
    public void setStatus(MonthlyPassStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}