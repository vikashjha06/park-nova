package com.smartparking.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "parking_areas")
public class ParkingArea {

    @Id
    private String id;

    private String name;
    private String address;
    private String city;

    private int totalSlots;
    private int availableSlots;

    // Google Map location
    private double latitude;
    private double longitude;

    private String googleMapUrl;
    private String googlePlaceId;

    private boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ParkingArea() {
    }

    public ParkingArea(
            String name,
            String address,
            String city,
            int totalSlots,
            double latitude,
            double longitude,
            String googleMapUrl,
            String googlePlaceId) {

        this.name = name;
        this.address = address;
        this.city = city;

        this.totalSlots = totalSlots;
        this.availableSlots = totalSlots;

        this.latitude = latitude;
        this.longitude = longitude;

        this.googleMapUrl = googleMapUrl;
        this.googlePlaceId = googlePlaceId;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(int totalSlots) {
        this.totalSlots = totalSlots;
    }

    public int getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(int availableSlots) {
        this.availableSlots = availableSlots;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getGoogleMapUrl() {
        return googleMapUrl;
    }

    public void setGoogleMapUrl(String googleMapUrl) {
        this.googleMapUrl = googleMapUrl;
    }

    public String getGooglePlaceId() {
        return googlePlaceId;
    }

    public void setGooglePlaceId(String googlePlaceId) {
        this.googlePlaceId = googlePlaceId;
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