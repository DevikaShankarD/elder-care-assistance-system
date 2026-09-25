package com.eldercare.model;

import com.eldercare.util.ValidationUtil;

import java.time.LocalDateTime;

/**
 * BloodSugarRecord tracks blood glucose levels in mg/dL.
 * Extends {@link HealthRecord} and implements dynamic polymorphic evaluation based on
 * American Diabetes Association (ADA) guidelines, tailoring thresholds to reading type
 * (FASTING, POST_MEAL, or RANDOM).
 */
public class BloodSugarRecord extends HealthRecord {

    private double valueMgDl;
    private ReadingType readingType;

    public BloodSugarRecord() {
        super();
    }

    public BloodSugarRecord(int recordId, int elderId, LocalDateTime recordedAt, String notes,
                            double valueMgDl, ReadingType readingType) {
        super(recordId, elderId, recordedAt, notes);
        setValueMgDl(valueMgDl);
        setReadingType(readingType);
    }

    public double getValueMgDl() {
        return valueMgDl;
    }

    public void setValueMgDl(double valueMgDl) {
        this.valueMgDl = ValidationUtil.validateBloodSugar(valueMgDl);
    }

    public ReadingType getReadingType() {
        return readingType;
    }

    public void setReadingType(ReadingType readingType) {
        if (readingType == null) {
            throw new IllegalArgumentException("Reading type cannot be null.");
        }
        this.readingType = readingType;
    }

    @Override
    public HealthStatus evaluate() {
        if (readingType == null) {
            readingType = ReadingType.RANDOM;
        }

        switch (readingType) {
            case FASTING:
                // Fasting guidelines
                if (valueMgDl < 50.0 || valueMgDl >= 250.0) {
                    return HealthStatus.CRITICAL;
                } else if (valueMgDl < 70.0) {
                    return HealthStatus.LOW;
                } else if (valueMgDl <= 99.0) {
                    return HealthStatus.NORMAL;
                } else if (valueMgDl <= 125.0) {
                    return HealthStatus.ELEVATED;
                } else {
                    return HealthStatus.HIGH;
                }

            case POST_MEAL:
            case RANDOM:
            default:
                // Post-Meal and Random guidelines
                if (valueMgDl < 50.0 || valueMgDl >= 300.0) {
                    return HealthStatus.CRITICAL;
                } else if (valueMgDl < 70.0) {
                    return HealthStatus.LOW;
                } else if (valueMgDl <= 139.0) {
                    return HealthStatus.NORMAL;
                } else if (valueMgDl <= 199.0) {
                    return HealthStatus.ELEVATED;
                } else {
                    return HealthStatus.HIGH;
                }
        }
    }

    @Override
    public String getSummary() {
        return String.format("%.1f mg/dL [%s] (%s)",
                valueMgDl, (readingType != null ? readingType.name() : "RANDOM"), evaluate().getDisplayName());
    }

    @Override
    public String getType() {
        return "Blood Sugar";
    }
}
