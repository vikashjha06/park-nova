package com.smartparking.dto;

public class AdminSettingsRequest {

    private String systemName;
    private String supportEmail;

    private Boolean maintenanceMode;
    private Boolean bookingEnabled;

    public AdminSettingsRequest() {
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public Boolean getMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(
            Boolean maintenanceMode) {

        this.maintenanceMode =
                maintenanceMode;
    }

    public Boolean getBookingEnabled() {
        return bookingEnabled;
    }

    public void setBookingEnabled(
            Boolean bookingEnabled) {

        this.bookingEnabled =
                bookingEnabled;
    }
}