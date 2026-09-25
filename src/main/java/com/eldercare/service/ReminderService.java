package com.eldercare.service;

import com.eldercare.dao.MedicationDAO;
import com.eldercare.dao.MedicationLogDAO;
import com.eldercare.model.DoseStatus;
import com.eldercare.model.Medication;
import com.eldercare.model.MedicationLog;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service managing medication schedules, dose tracking, reminders, and adherence analytics.
 * Business logic resides here rather than in SQL queries.
 */
public class ReminderService {

    private final MedicationDAO medicationDAO;
    private final MedicationLogDAO medicationLogDAO;

    public ReminderService() {
        this(new MedicationDAO(), new MedicationLogDAO());
    }

    public ReminderService(MedicationDAO medicationDAO, MedicationLogDAO medicationLogDAO) {
        this.medicationDAO = medicationDAO;
        this.medicationLogDAO = medicationLogDAO;
    }

    /**
     * Computes the complete schedule of doses for an elder on a specific date.
     *
     * @param elderId elder ID
     * @param date target date
     * @return sorted list of dose schedule items
     * @throws SQLException on database error
     */
    public List<DoseScheduleItem> getTodaySchedule(int elderId, LocalDate date) throws SQLException {
        List<DoseScheduleItem> schedule = new ArrayList<>();
        List<Medication> medications = medicationDAO.getByElderId(elderId);

        for (Medication med : medications) {
            if (med.isActiveOn(date)) {
                for (LocalTime time : med.getDoseTimes()) {
                    LocalDateTime scheduledDateTime = date.atTime(time);
                    Optional<MedicationLog> existingLog =
                            medicationLogDAO.getByMedicationAndScheduledTime(med.getMedicationId(), scheduledDateTime);

                    DoseStatus status = existingLog.map(MedicationLog::getStatus).orElse(null);
                    schedule.add(new DoseScheduleItem(
                            med.getMedicationId(),
                            med.getName(),
                            med.getDosage(),
                            time,
                            scheduledDateTime,
                            med.getInstructions(),
                            status
                    ));
                }
            }
        }

        schedule.sort(Comparator.comparing(DoseScheduleItem::getDoseTime));
        return schedule;
    }

    /**
     * Retrieves all doses for an elder that are due or past due at the given timestamp
     * and have not yet been recorded as TAKEN or SKIPPED.
     *
     * @param elderId elder ID
     * @param currentDateTime current evaluation date-time
     * @return list of due or overdue dose schedule items
     * @throws SQLException on database error
     */
    public List<DoseScheduleItem> getDueOrOverdueDoses(int elderId, LocalDateTime currentDateTime) throws SQLException {
        List<DoseScheduleItem> todaySchedule = getTodaySchedule(elderId, currentDateTime.toLocalDate());
        List<DoseScheduleItem> dueOrOverdue = new ArrayList<>();

        for (DoseScheduleItem item : todaySchedule) {
            if (item.isDueOrOverdue(currentDateTime)) {
                dueOrOverdue.add(item);
            }
        }
        return dueOrOverdue;
    }

    /**
     * Records a medication dose status (TAKEN, MISSED, or SKIPPED).
     * If a log entry already exists for that scheduled time, updates it; otherwise inserts a new log.
     *
     * @param medicationId medication ID
     * @param scheduledTime scheduled dose time
     * @param status DoseStatus
     * @return persisted log ID
     * @throws SQLException on database error
     */
    public int recordDose(int medicationId, LocalDateTime scheduledTime, DoseStatus status) throws SQLException {
        if (status == null) {
            throw new IllegalArgumentException("Dose status cannot be null.");
        }
        Optional<MedicationLog> existing =
                medicationLogDAO.getByMedicationAndScheduledTime(medicationId, scheduledTime);

        if (existing.isPresent()) {
            MedicationLog log = existing.get();
            log.setStatus(status);
            log.setLoggedAt(LocalDateTime.now());
            medicationLogDAO.update(log);
            return log.getLogId();
        } else {
            MedicationLog newLog = new MedicationLog(0, medicationId, scheduledTime, status, LocalDateTime.now());
            return medicationLogDAO.add(newLog);
        }
    }

    /**
     * Calculates the adherence percentage for a single medication course.
     * Adherence % = (TAKEN doses / total logged doses) * 100.
     *
     * @param medicationId medication ID
     * @return adherence percentage (0.0 to 100.0)
     * @throws SQLException on database error
     */
    public double calculateMedicationAdherence(int medicationId) throws SQLException {
        List<MedicationLog> logs = medicationLogDAO.getByMedicationId(medicationId);
        if (logs.isEmpty()) {
            return 100.0; // Default to 100% when no adverse logs exist
        }
        long takenCount = logs.stream().filter(l -> l.getStatus() == DoseStatus.TAKEN).count();
        return (takenCount * 100.0) / logs.size();
    }

    /**
     * Calculates the overall medication adherence percentage for an elder across all prescriptions.
     *
     * @param elderId elder ID
     * @return overall adherence percentage (0.0 to 100.0)
     * @throws SQLException on database error
     */
    public double calculateElderOverallAdherence(int elderId) throws SQLException {
        List<MedicationLog> logs = medicationLogDAO.getByElderId(elderId);
        if (logs.isEmpty()) {
            return 100.0;
        }
        long takenCount = logs.stream().filter(l -> l.getStatus() == DoseStatus.TAKEN).count();
        return (takenCount * 100.0) / logs.size();
    }

    /**
     * Computes an adherence breakdown map for all active medications belonging to an elder.
     *
     * @param elderId elder ID
     * @return map of Medication to adherence percentage
     * @throws SQLException on database error
     */
    public Map<Medication, Double> getMedicationAdherenceBreakdown(int elderId) throws SQLException {
        Map<Medication, Double> breakdown = new LinkedHashMap<>();
        List<Medication> meds = medicationDAO.getByElderId(elderId);
        for (Medication m : meds) {
            double rate = calculateMedicationAdherence(m.getMedicationId());
            breakdown.put(m, rate);
        }
        return breakdown;
    }
}
