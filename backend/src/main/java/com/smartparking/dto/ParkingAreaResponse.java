package com.smartparking.dto;

import java.time.LocalDateTime;

import com.smartparking.model.ParkingArea;

public class ParkingAreaResponse {

    private String id;
    private String name;
    private String address;
    private String city;

    private int totalSlots;
    private long availableSlots;

    private double latitude;
    private double longitude;

    private String googleMapUrl;
    private String googlePlaceId;

    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ParkingAreaResponse(
            ParkingArea area,
            long availableSlots) {

        this.id = area.getId();
        this.name = area.getName();
        this.address = area.getAddress();
        this.city = area.getCity();

        this.totalSlots = area.getTotalSlots();
        this.availableSlots = availableSlots;

        this.latitude = area.getLatitude();
        this.longitude = area.getLongitude();

        this.googleMapUrl = area.getGoogleMapUrl();
        this.googlePlaceId = area.getGooglePlaceId();

        this.active = area.isActive();

        this.createdAt = area.getCreatedAt();
        this.updatedAt = area.getUpdatedAt();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public long getAvailableSlots() {
        return availableSlots;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getGoogleMapUrl() {
        return googleMapUrl;
    }

    public String getGooglePlaceId() {
        return googlePlaceId;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}