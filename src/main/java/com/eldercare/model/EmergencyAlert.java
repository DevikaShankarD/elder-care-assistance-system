package com.eldercare.model;

import com.eldercare.util.DateUtil;
import com.eldercare.util.ValidationUtil;

import java.time.LocalDateTime;

/**
 * EmergencyAlert records an emergency event triggered either manually (SOS button)
 * or automatically (when a critical health measurement is logged).
 */
public class EmergencyAlert {

    private int alertId;
    private int elderId;
    private LocalDateTime timestamp;
    private String reason;
    private String contactsNotified;

    public EmergencyAlert() {
    }

    public EmergencyAlert(int alertId, int elderId, LocalDateTime timestamp, String reason, String contactsNotified) {
        this.alertId = alertId;
        this.elderId = elderId;
        setTimestamp(timestamp);
        setReason(reason);
        setContactsNotified(contactsNotified);
    }

    public int getAlertId() {
        return alertId;
    }

    public void setAlertId(int alertId) {
        this.alertId = alertId;
    }

    public int getElderId() {
        return elderId;
    }

    public void setElderId(int elderId) {
        this.elderId = elderId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            this.timestamp = LocalDateTime.now();
        } else {
            this.timestamp = timestamp;
        }
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = ValidationUtil.requireNonBlank(reason, "Alert Reason");
    }

    public String getContactsNotified() {
        return contactsNotified;
    }

    public void setContactsNotified(String contactsNotified) {
        this.contactsNotified = (contactsNotified != null) ? contactsNotified.trim() : "None";
    }

    @Override
    public String toString() {
        return String.format("ALERT #%d [Elder ID: %d] at %s | Reason: %s | Notified: %s",
                alertId, elderId, DateUtil.formatDateTime(timestamp), reason, contactsNotified);
    }
}
