package com.smartparking.dto;

public class MonthlyPassQuoteResponse {

    private final double dailyHours;
    private final double hourlyRate;
    private final double normalMonthlyPrice;
    private final double discountPercent;
    private final double discountAmount;
    private final double finalPrice;

    public MonthlyPassQuoteResponse(
            double dailyHours,
            double hourlyRate,
            double normalMonthlyPrice,
            double discountPercent,
            double discountAmount,
            double finalPrice) {

        this.dailyHours = dailyHours;
        this.hourlyRate = hourlyRate;
        this.normalMonthlyPrice = normalMonthlyPrice;
        this.discountPercent = discountPercent;
        this.discountAmount = discountAmount;
        this.finalPrice = finalPrice;
    }

    public double getDailyHours() {
        return dailyHours;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public double getNormalMonthlyPrice() {
        return normalMonthlyPrice;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getFinalPrice() {
        return finalPrice;
    }
}