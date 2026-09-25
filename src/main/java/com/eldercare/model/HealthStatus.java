package com.eldercare.model;

/**
 * HealthStatus represents the clinical evaluation level of a health measurement.
 * Used polymorphically across all HealthRecord subclasses.
 */
public enum HealthStatus {
    LOW("Low"),
    NORMAL("Normal"),
    ELEVATED("Elevated"),
    HIGH("High"),
    CRITICAL("Critical");

    private final String displayName;

    HealthStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
