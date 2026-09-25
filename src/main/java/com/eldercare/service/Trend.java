package com.eldercare.service;

/**
 * Indicates directional trajectory in consecutive health readings over time.
 */
public enum Trend {
    RISING("Rising (^)"),
    FALLING("Falling (v)"),
    STABLE("Stable (=)"),
    INSUFFICIENT_DATA("Insufficient Data");

    private final String description;

    Trend(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
