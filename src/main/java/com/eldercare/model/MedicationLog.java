package com.eldercare.model;

import com.eldercare.util.DateUtil;

import java.time.LocalDateTime;

/**
 * Tracks the historical adherence of a medication dose, recording whether it was TAKEN, MISSED, or SKIPPED.
 */
public class MedicationLog {

    private int logId;
    private int medicationId;
    private LocalDateTime scheduledTime;
    private DoseStatus status;
    private LocalDateTime loggedAt;

    public MedicationLog() {
    }

    public MedicationLog(int logId, int medicationId, LocalDateTime scheduledTime, DoseStatus status, LocalDateTime loggedAt) {
        this.logId = logId;
        this.medicationId = medicationId;
        setScheduledTime(scheduledTime);
        setStatus(status);
        setLoggedAt(loggedAt);
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public int getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(int medicationId) {
        this.medicationId = medicationId;
    }

    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalDateTime scheduledTime) {
        if (scheduledTime == null) {
            throw new IllegalArgumentException("Scheduled time cannot be null.");
        }
        this.scheduledTime = scheduledTime;
    }

    public DoseStatus getStatus() {
        return status;
    }

    public void setStatus(DoseStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Dose status cannot be null.");
        }
        this.status = status;
    }

    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(LocalDateTime loggedAt) {
        if (loggedAt == null) {
            this.loggedAt = LocalDateTime.now();
        } else {
            this.loggedAt = loggedAt;
        }
    }

    @Override
    public String toString() {
        return String.format("Log #%d [Medication ID %d] - Scheduled: %s | Status: %s | Logged: %s",
                logId, medicationId, DateUtil.formatDateTime(scheduledTime), status, DateUtil.formatDateTime(loggedAt));
    }
}
