package com.eldercare.service;

import com.eldercare.dao.AppointmentDAO;
import com.eldercare.dao.DoctorDAO;
import com.eldercare.dao.ElderDAO;
import com.eldercare.dao.EmergencyAlertDAO;
import com.eldercare.dao.EmergencyContactDAO;
import com.eldercare.model.Appointment;
import com.eldercare.model.Doctor;
import com.eldercare.model.Elder;
import com.eldercare.model.EmergencyAlert;
import com.eldercare.model.EmergencyContact;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing emergency responses, manual SOS triggers, and automatic critical health alerts.
 */
public class EmergencyService {

    private final EmergencyAlertDAO alertDAO;
    private final ElderDAO elderDAO;
    private final EmergencyContactDAO contactDAO;
    private final DoctorDAO doctorDAO;
    private final AppointmentDAO appointmentDAO;

    public EmergencyService() {
        this(new EmergencyAlertDAO(), new ElderDAO(), new EmergencyContactDAO(), new DoctorDAO(), new AppointmentDAO());
    }

    public EmergencyService(EmergencyAlertDAO alertDAO, ElderDAO elderDAO,
                            EmergencyContactDAO contactDAO, DoctorDAO doctorDAO,
                            AppointmentDAO appointmentDAO) {
        this.alertDAO = alertDAO;
        this.elderDAO = elderDAO;
        this.contactDAO = contactDAO;
        this.doctorDAO = doctorDAO;
        this.appointmentDAO = appointmentDAO;
    }

    /**
     * Triggers a manual SOS emergency broadcast for an elder.
     * Compiles prioritized emergency contacts, medical summary, and primary doctor,
     * then permanently logs an EmergencyAlert.
     *
     * @param elderId elder ID
     * @param reason reason or description of emergency
     * @return SOSResult containing complete dispatch details
     * @throws SQLException on database error
     */
    public SOSResult triggerSOS(int elderId, String reason) throws SQLException {
        Elder elder = elderDAO.getById(elderId)
                .orElseThrow(() -> new IllegalArgumentException("Elder with ID " + elderId + " not found."));

        List<EmergencyContact> contacts = contactDAO.getByElderId(elderId);
        // Contacts are sorted by priority (1 = call first)
        Collections.sort(contacts);

        Doctor attendingDoctor = findAttendingDoctor(elderId);

        String contactsSummary = contacts.isEmpty()
                ? "No contacts registered"
                : contacts.stream()
                        .map(c -> String.format("P%d: %s (%s)", c.getPriority(), c.getName(), c.getPhone()))
                        .collect(Collectors.joining("; "));

        EmergencyAlert alert = new EmergencyAlert(
                0,
                elderId,
                LocalDateTime.now(),
                (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Manual SOS Pressed",
                contactsSummary
        );
        int alertId = alertDAO.add(alert);
        alert.setAlertId(alertId);

        return new SOSResult(alertId, elder, alert.getTimestamp(), alert.getReason(), contacts, attendingDoctor);
    }

    /**
     * Triggers an automated emergency alert when a critical health reading is recorded.
     *
     * @param elderId elder ID
     * @param criticalReadingDetails summary of the critical reading
     * @return persisted EmergencyAlert
     * @throws SQLException on database error
     */
    public EmergencyAlert triggerAutoCriticalAlert(int elderId, String criticalReadingDetails) throws SQLException {
        Elder elder = elderDAO.getById(elderId)
                .orElseThrow(() -> new IllegalArgumentException("Elder with ID " + elderId + " not found."));

        List<EmergencyContact> contacts = contactDAO.getByElderId(elderId);
        Collections.sort(contacts);

        String contactsSummary = contacts.isEmpty()
                ? "No contacts registered"
                : contacts.stream()
                        .map(c -> String.format("P%d: %s (%s)", c.getPriority(), c.getName(), c.getPhone()))
                        .collect(Collectors.joining("; "));

        String alertReason = "AUTOMATIC ALERT - Critical reading: " + criticalReadingDetails;
        EmergencyAlert alert = new EmergencyAlert(0, elderId, LocalDateTime.now(), alertReason, contactsSummary);
        int alertId = alertDAO.add(alert);
        alert.setAlertId(alertId);
        return alert;
    }

    /**
     * Retrieves the alert history for an elder, ordered latest first.
     *
     * @param elderId elder ID
     * @return list of EmergencyAlert records
     * @throws SQLException on database error
     */
    public List<EmergencyAlert> getAlertHistory(int elderId) throws SQLException {
        return alertDAO.getByElderId(elderId);
    }

    private Doctor findAttendingDoctor(int elderId) throws SQLException {
        // Attempt to find doctor from the most recent or upcoming appointment
        List<Appointment> appts = appointmentDAO.getByElderId(elderId);
        if (!appts.isEmpty()) {
            int doctorId = appts.get(appts.size() - 1).getDoctorId();
            Optional<Doctor> doc = doctorDAO.getById(doctorId);
            if (doc.isPresent()) {
                return doc.get();
            }
        }
        // Fallback to first doctor in the system if available
        List<Doctor> allDocs = doctorDAO.getAll();
        return allDocs.isEmpty() ? null : allDocs.get(0);
    }
}
