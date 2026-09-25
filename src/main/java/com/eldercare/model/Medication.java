package com.eldercare.model;

import com.eldercare.util.DateUtil;
import com.eldercare.util.ValidationUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a prescribed medication course for an elder.
 * Contains dosage guidelines, scheduling times, and active date range.
 */
public class Medication {

    private int medicationId;
    private int elderId;
    private String name;
    private String dosage;
    private int timesPerDay;
    private List<LocalTime> doseTimes = new ArrayList<>();
    private LocalDate startDate;
    private LocalDate endDate;
    private String instructions;

    public Medication() {
    }

    public Medication(int medicationId, int elderId, String name, String dosage,
                      int timesPerDay, List<LocalTime> doseTimes,
                      LocalDate startDate, LocalDate endDate, String instructions) {
        this.medicationId = medicationId;
        this.elderId = elderId;
        setName(name);
        setDosage(dosage);
        setTimesPerDay(timesPerDay);
        setDoseTimes(doseTimes);
        setDateRange(startDate, endDate);
        setInstructions(instructions);
    }

    public int getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(int medicationId) {
        this.medicationId = medicationId;
    }

    public int getElderId() {
        return elderId;
    }

    public void setElderId(int elderId) {
        this.elderId = elderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = ValidationUtil.requireNonBlank(name, "Medication Name");
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = ValidationUtil.requireNonBlank(dosage, "Dosage");
    }

    public int getTimesPerDay() {
        return timesPerDay;
    }

    public void setTimesPerDay(int timesPerDay) {
        if (timesPerDay < 1) {
            throw new IllegalArgumentException("Times per day must be at least 1.");
        }
        this.timesPerDay = timesPerDay;
    }

    public List<LocalTime> getDoseTimes() {
        return Collections.unmodifiableList(doseTimes);
    }

    public void setDoseTimes(List<LocalTime> doseTimes) {
        if (doseTimes == null || doseTimes.isEmpty()) {
            throw new IllegalArgumentException("At least one dose time must be specified.");
        }
        this.doseTimes = new ArrayList<>(doseTimes);
        Collections.sort(this.doseTimes);
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null.");
        }
        if (this.endDate != null && startDate.isAfter(this.endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null.");
        }
        if (this.startDate != null && endDate.isBefore(this.startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
        this.endDate = endDate;
    }

    public void setDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and End date cannot be null.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date (" + DateUtil.formatDate(endDate) +
                    ") cannot be before start date (" + DateUtil.formatDate(startDate) + ").");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = (instructions != null) ? instructions.trim() : "";
    }

    /**
     * Checks if this medication is currently active on a given date.
     *
     * @param date date to evaluate
     * @return true if date falls between startDate and endDate inclusive
     */
    public boolean isActiveOn(LocalDate date) {
        if (date == null || startDate == null || endDate == null) {
            return false;
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    @Override
    public String toString() {
        return String.format("Medication #%d: %s (%s) | %d time(s)/day at %s | Active: %s to %s | Notes: %s",
                medicationId, name, dosage, timesPerDay, doseTimes,
                DateUtil.formatDate(startDate), DateUtil.formatDate(endDate), instructions);
    }
}
