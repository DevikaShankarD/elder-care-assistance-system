package com.eldercare.model;

import com.eldercare.util.DateUtil;

import java.time.LocalDateTime;

/**
 * Abstract base class for all health diagnostic records.
 * Demonstrates the OOP principles of Abstraction and Dynamic Polymorphism:
 * Subclasses implement {@link #evaluate()}, {@link #getSummary()}, and {@link #getType()}
 * according to their clinical standards, allowing callers to evaluate any record through
 * a generic HealthRecord reference.
 */
public abstract class HealthRecord {

    private int recordId;
    private int elderId;
    private LocalDateTime recordedAt;
    private String notes;

    public HealthRecord() {
    }

    public HealthRecord(int recordId, int elderId, LocalDateTime recordedAt, String notes) {
        this.recordId = recordId;
        this.elderId = elderId;
        setRecordedAt(recordedAt);
        setNotes(notes);
    }

    /**
     * Polymorphic method evaluating the health status of this measurement.
     * Overridden by each record type.
     *
     * @return {@link HealthStatus} (LOW, NORMAL, ELEVATED, HIGH, CRITICAL)
     */
    public abstract HealthStatus evaluate();

    /**
     * Returns a concise human-readable summary of the measurement and its status.
     *
     * @return summary string
     */
    public abstract String getSummary();

    /**
     * Returns the name of the health record type (e.g. "Blood Pressure").
     *
     * @return type name string
     */
    public abstract String getType();

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public int getElderId() {
        return elderId;
    }

    public void setElderId(int elderId) {
        this.elderId = elderId;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        if (recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
        } else {
            this.recordedAt = recordedAt;
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = (notes != null) ? notes.trim() : "";
    }

    @Override
    public String toString() {
        return String.format("[%s #%d] Elder ID: %d | Time: %s | Status: %s | Summary: %s | Notes: %s",
                getType(), recordId, elderId, DateUtil.formatDateTime(recordedAt), evaluate(), getSummary(), notes);
    }
}
