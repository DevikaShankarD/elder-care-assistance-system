package com.eldercare.service;

/**
 * Encapsulates statistical averages of health readings over an evaluation window (e.g. 7 or 30 days).
 */
public class HealthAverage {

    private final String recordType;
    private final int daysWindow;
    private final int readingCount;
    private final Double primaryAverage;    // systolic, blood sugar, or heart rate
    private final Double secondaryAverage;  // diastolic (applicable for BP)

    public HealthAverage(String recordType, int daysWindow, int readingCount, Double primaryAverage, Double secondaryAverage) {
        this.recordType = recordType;
        this.daysWindow = daysWindow;
        this.readingCount = readingCount;
        this.primaryAverage = primaryAverage;
        this.secondaryAverage = secondaryAverage;
    }

    public String getRecordType() {
        return recordType;
    }

    public int getDaysWindow() {
        return daysWindow;
    }

    public int getReadingCount() {
        return readingCount;
    }

    public Double getPrimaryAverage() {
        return primaryAverage;
    }

    public Double getSecondaryAverage() {
        return secondaryAverage;
    }

    public boolean hasData() {
        return readingCount > 0 && primaryAverage != null;
    }

    public String getFormattedSummary() {
        if (!hasData()) {
            return String.format("%d-Day %s: No readings recorded", daysWindow, recordType);
        }
        if (secondaryAverage != null) {
            return String.format("%d-Day %s Avg: %.1f / %.1f mmHg (%d readings)",
                    daysWindow, recordType, primaryAverage, secondaryAverage, readingCount);
        } else if ("BLOOD_SUGAR".equalsIgnoreCase(recordType)) {
            return String.format("%d-Day %s Avg: %.1f mg/dL (%d readings)",
                    daysWindow, recordType, primaryAverage, readingCount);
        } else {
            return String.format("%d-Day %s Avg: %.1f bpm (%d readings)",
                    daysWindow, recordType, primaryAverage, readingCount);
        }
    }

    @Override
    public String toString() {
        return getFormattedSummary();
    }
}
