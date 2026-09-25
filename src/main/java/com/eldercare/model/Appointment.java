package com.eldercare.model;

import com.eldercare.util.DateUtil;
import com.eldercare.util.ValidationUtil;

import java.time.LocalDateTime;

/**
 * Represents a scheduled doctor consultation for an elder.
 */
public class Appointment {

    private int appointmentId;
    private int elderId;
    private int doctorId;
    private LocalDateTime dateTime;
    private String purpose;
    private AppointmentStatus status;

    public Appointment() {
    }

    public Appointment(int appointmentId, int elderId, int doctorId,
                       LocalDateTime dateTime, String purpose, AppointmentStatus status) {
        this.appointmentId = appointmentId;
        this.elderId = elderId;
        this.doctorId = doctorId;
        setDateTime(dateTime);
        setPurpose(purpose);
        setStatus(status);
    }

    public int getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(int appointmentId) {
        this.appointmentId = appointmentId;
    }

    public int getElderId() {
        return elderId;
    }

    public void setElderId(int elderId) {
        this.elderId = elderId;
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("Appointment date-time cannot be null.");
        }
        this.dateTime = dateTime;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = ValidationUtil.requireNonBlank(purpose, "Purpose");
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Appointment status cannot be null.");
        }
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("Appointment #%d | Elder ID: %d | Doctor ID: %d | Date: %s | Status: %s | Purpose: %s",
                appointmentId, elderId, doctorId, DateUtil.formatDateTime(dateTime), status, purpose);
    }
}
