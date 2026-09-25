package com.eldercare.service;

import com.eldercare.model.DoseStatus;
import com.eldercare.util.DateUtil;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Data transfer object representing a specific scheduled medication dose event.
 */
public class DoseScheduleItem {

    private final int medicationId;
    private final String medicationName;
    private final String dosage;
    private final LocalTime doseTime;
    private final LocalDateTime scheduledDateTime;
    private final String instructions;
    private DoseStatus status; // null if pending / not yet logged

    public DoseScheduleItem(int medicationId, String medicationName, String dosage,
                            LocalTime doseTime, LocalDateTime scheduledDateTime,
                            String instructions, DoseStatus status) {
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.doseTime = doseTime;
        this.scheduledDateTime = scheduledDateTime;
        this.instructions = instructions;
        this.status = status;
    }

    public int getMedicationId() {
        return medicationId;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public LocalTime getDoseTime() {
        return doseTime;
    }

    public LocalDateTime getScheduledDateTime() {
        return scheduledDateTime;
    }

    public String getInstructions() {
        return instructions;
    }

    public DoseStatus getStatus() {
        return status;
    }

    public void setStatus(DoseStatus status) {
        this.status = status;
    }

    public boolean isLogged() {
        return status != null;
    }

    public boolean isDueOrOverdue(LocalDateTime now) {
        if (status == DoseStatus.TAKEN || status == DoseStatus.SKIPPED) {
            return false;
        }
        return !scheduledDateTime.isAfter(now);
    }

    @Override
    public String toString() {
        String statusStr = (status != null) ? status.getDisplayName() : "PENDING";
        return String.format("[%s] %s (%s) - %s | Status: %s",
                DateUtil.formatTime(doseTime), medicationName, dosage, instructions, statusStr);
    }
}
