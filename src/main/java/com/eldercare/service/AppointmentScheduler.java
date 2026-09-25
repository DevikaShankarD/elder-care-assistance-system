package com.eldercare.service;

import com.eldercare.dao.AppointmentDAO;
import com.eldercare.dao.DoctorDAO;
import com.eldercare.dao.ElderDAO;
import com.eldercare.model.Appointment;
import com.eldercare.model.AppointmentStatus;
import com.eldercare.util.DateUtil;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing appointment bookings, rescheduling, cancellations, and doctor schedules.
 * Enforces conflict detection rules:
 * <ul>
 *   <li>Rejects booking in the past.</li>
 *   <li>Doctor conflict: Cannot book within 30 minutes of another scheduled appointment for the same doctor.</li>
 *   <li>Elder conflict: Cannot book within 30 minutes of another scheduled appointment for the same elder.</li>
 * </ul>
 */
public class AppointmentScheduler {

    public static final int CONFLICT_BUFFER_MINUTES = 30;

    private final AppointmentDAO appointmentDAO;
    private final DoctorDAO doctorDAO;
    private final ElderDAO elderDAO;

    public AppointmentScheduler() {
        this(new AppointmentDAO(), new DoctorDAO(), new ElderDAO());
    }

    public AppointmentScheduler(AppointmentDAO appointmentDAO, DoctorDAO doctorDAO, ElderDAO elderDAO) {
        this.appointmentDAO = appointmentDAO;
        this.doctorDAO = doctorDAO;
        this.elderDAO = elderDAO;
    }

    /**
     * Books a new appointment after verifying elder existence, doctor existence,
     * past-date check, and 30-minute conflict constraints.
     *
     * @param elderId elder ID
     * @param doctorId doctor ID
     * @param dateTime appointment date and time
     * @param purpose purpose of visit
     * @return created Appointment object
     * @throws IllegalArgumentException on conflict or invalid parameters
     * @throws SQLException on database error
     */
    public Appointment bookAppointment(int elderId, int doctorId, LocalDateTime dateTime, String purpose) throws SQLException {
        validateAppointmentTime(dateTime);

        if (elderDAO.getById(elderId).isEmpty()) {
            throw new IllegalArgumentException("Elder with ID " + elderId + " does not exist.");
        }
        if (doctorDAO.getById(doctorId).isEmpty()) {
            throw new IllegalArgumentException("Doctor with ID " + doctorId + " does not exist.");
        }

        checkConflicts(elderId, doctorId, dateTime, -1);

        Appointment appt = new Appointment(0, elderId, doctorId, dateTime, purpose, AppointmentStatus.SCHEDULED);
        int generatedId = appointmentDAO.add(appt);
        appt.setAppointmentId(generatedId);
        return appt;
    }

    /**
     * Reschedules an existing appointment to a new date and time.
     *
     * @param appointmentId appointment ID
     * @param newDateTime new target date and time
     * @return updated Appointment object
     * @throws IllegalArgumentException on conflict or not found
     * @throws SQLException on database error
     */
    public Appointment rescheduleAppointment(int appointmentId, LocalDateTime newDateTime) throws SQLException {
        validateAppointmentTime(newDateTime);

        Optional<Appointment> existingOpt = appointmentDAO.getById(appointmentId);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment with ID " + appointmentId + " not found.");
        }

        Appointment appt = existingOpt.get();
        checkConflicts(appt.getElderId(), appt.getDoctorId(), newDateTime, appointmentId);

        appt.setDateTime(newDateTime);
        appt.setStatus(AppointmentStatus.SCHEDULED);
        appointmentDAO.update(appt);
        return appt;
    }

    /**
     * Cancels a scheduled appointment.
     *
     * @param appointmentId appointment ID
     * @return true if cancelled
     * @throws SQLException on database error
     */
    public boolean cancelAppointment(int appointmentId) throws SQLException {
        Optional<Appointment> existingOpt = appointmentDAO.getById(appointmentId);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment with ID " + appointmentId + " not found.");
        }
        Appointment appt = existingOpt.get();
        appt.setStatus(AppointmentStatus.CANCELLED);
        return appointmentDAO.update(appt);
    }

    /**
     * Marks an appointment as completed.
     *
     * @param appointmentId appointment ID
     * @return true if marked completed
     * @throws SQLException on database error
     */
    public boolean completeAppointment(int appointmentId) throws SQLException {
        Optional<Appointment> existingOpt = appointmentDAO.getById(appointmentId);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Appointment with ID " + appointmentId + " not found.");
        }
        Appointment appt = existingOpt.get();
        appt.setStatus(AppointmentStatus.COMPLETED);
        return appointmentDAO.update(appt);
    }

    /**
     * Lists all upcoming scheduled appointments for an elder.
     *
     * @param elderId elder ID
     * @return chronologically sorted list of upcoming appointments
     * @throws SQLException on database error
     */
    public List<Appointment> getUpcomingAppointmentsForElder(int elderId) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        return appointmentDAO.getByElderId(elderId).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED)
                .filter(a -> !a.getDateTime().isBefore(now))
                .sorted(Comparator.comparing(Appointment::getDateTime))
                .collect(Collectors.toList());
    }

    /**
     * Lists a doctor's schedule for a given date.
     *
     * @param doctorId doctor ID
     * @param date target date
     * @return sorted appointments for the doctor on that date
     * @throws SQLException on database error
     */
    public List<Appointment> getDoctorSchedule(int doctorId, LocalDate date) throws SQLException {
        return appointmentDAO.getByDoctorId(doctorId).stream()
                .filter(a -> a.getDateTime().toLocalDate().equals(date))
                .sorted(Comparator.comparing(Appointment::getDateTime))
                .collect(Collectors.toList());
    }

    /**
     * Validates that an appointment is scheduled for the future.
     */
    private void validateAppointmentTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            throw new IllegalArgumentException("Appointment date-time cannot be null.");
        }
        if (dateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot schedule an appointment in the past (" +
                    DateUtil.formatDateTime(dateTime) + ").");
        }
    }

    /**
     * Checks 30-minute conflict constraints for both the doctor and the elder.
     *
     * @param elderId elder ID
     * @param doctorId doctor ID
     * @param targetTime proposed time
     * @param excludeAppointmentId appointment ID to exclude (when rescheduling)
     */
    public void checkConflicts(int elderId, int doctorId, LocalDateTime targetTime, int excludeAppointmentId) throws SQLException {
        // 1. Check doctor appointments for conflict
        List<Appointment> doctorAppts = appointmentDAO.getByDoctorId(doctorId);
        for (Appointment a : doctorAppts) {
            if (a.getAppointmentId() == excludeAppointmentId || a.getStatus() != AppointmentStatus.SCHEDULED) {
                continue;
            }
            long minutesDiff = Math.abs(Duration.between(a.getDateTime(), targetTime).toMinutes());
            if (minutesDiff < CONFLICT_BUFFER_MINUTES) {
                throw new IllegalArgumentException(String.format(
                        "Doctor conflict: Dr. (ID %d) already has an appointment at %s (within %d minutes).",
                        doctorId, DateUtil.formatDateTime(a.getDateTime()), CONFLICT_BUFFER_MINUTES));
            }
        }

        // 2. Check elder appointments for conflict
        List<Appointment> elderAppts = appointmentDAO.getByElderId(elderId);
        for (Appointment a : elderAppts) {
            if (a.getAppointmentId() == excludeAppointmentId || a.getStatus() != AppointmentStatus.SCHEDULED) {
                continue;
            }
            long minutesDiff = Math.abs(Duration.between(a.getDateTime(), targetTime).toMinutes());
            if (minutesDiff < CONFLICT_BUFFER_MINUTES) {
                throw new IllegalArgumentException(String.format(
                        "Elder conflict: Elder (ID %d) already has an appointment at %s (within %d minutes).",
                        elderId, DateUtil.formatDateTime(a.getDateTime()), CONFLICT_BUFFER_MINUTES));
            }
        }
    }
}
