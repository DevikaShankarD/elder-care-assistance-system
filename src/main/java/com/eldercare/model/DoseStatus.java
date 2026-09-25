package com.eldercare.model;

/**
 * DoseStatus indicates whether a scheduled medication dose was taken, missed, or skipped.
 */
public enum DoseStatus {
    TAKEN("Taken"),
    MISSED("Missed"),
    SKIPPED("Skipped");

    private final String displayName;

    DoseStatus(String displayName) {
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
