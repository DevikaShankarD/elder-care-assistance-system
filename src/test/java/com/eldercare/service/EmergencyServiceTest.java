package com.eldercare.service;

import com.eldercare.dao.*;
import com.eldercare.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EmergencyService: SOS dispatch, prioritized contact ordering,
 * automated critical alerts, and alert history logging.
 */
public class EmergencyServiceTest {

    private static final String TEST_DB = "test_emergency.db";
    private static final String TEST_JDBC = "jdbc:sqlite:" + TEST_DB;

    private DatabaseManager dbManager;
    private EmergencyService emergencyService;
    private ElderDAO elderDAO;
    private EmergencyContactDAO contactDAO;
    private DoctorDAO doctorDAO;
    private AppointmentDAO appointmentDAO;
    private EmergencyAlertDAO alertDAO;

    private int elderId;
    private int doctorId;

    @BeforeEach
    public void setUp() throws SQLException {
        DatabaseManager.resetInstance();
        new File(TEST_DB).delete();

        dbManager = DatabaseManager.getInstance(TEST_JDBC);
        elderDAO = new ElderDAO(dbManager);
        contactDAO = new EmergencyContactDAO(dbManager);
        doctorDAO = new DoctorDAO(dbManager);
        appointmentDAO = new AppointmentDAO(dbManager);
        alertDAO = new EmergencyAlertDAO(dbManager);

        emergencyService = new EmergencyService(alertDAO, elderDAO, contactDAO, doctorDAO, appointmentDAO);

        elderId = elderDAO.add(new Elder(0, "Eleanor Vance", "9876543210", "12 Maple St", 80, "Female", "O+", "Hypertension"));
        doctorId = doctorDAO.add(new Doctor(0, "Dr. Evans", "Cardiology", "Memorial Hospital", "9876543220"));

        // Insert contacts with scrambled priority order (3, 1, 2)
        contactDAO.add(new EmergencyContact(0, elderId, "Charlie (Neighbor)", "Neighbor", "9871111113", 3));
        contactDAO.add(new EmergencyContact(0, elderId, "Alice (Daughter)", "Daughter", "9871111111", 1));
        contactDAO.add(new EmergencyContact(0, elderId, "Bob (Son)", "Son", "9871111112", 2));

        // Create an appointment so attending doctor is linked
        appointmentDAO.add(new Appointment(0, elderId, doctorId, LocalDateTime.now().plusDays(2), "Review", AppointmentStatus.SCHEDULED));
    }

    @AfterEach
    public void tearDown() {
        DatabaseManager.resetInstance();
        new File(TEST_DB).delete();
    }

    @Test
    @DisplayName("SOS dispatch lists emergency contacts in strict ascending priority order (1, 2, 3)")
    public void testSOSContactPriorityOrdering() throws SQLException {
        SOSResult result = emergencyService.triggerSOS(elderId, "Severe chest tightness");

        assertNotNull(result);
        List<EmergencyContact> contacts = result.getContactsNotified();
        assertEquals(3, contacts.size());

        // Verify priorities are ordered 1, 2, 3
        assertEquals(1, contacts.get(0).getPriority());
        assertEquals("Alice (Daughter)", contacts.get(0).getName());

        assertEquals(2, contacts.get(1).getPriority());
        assertEquals("Bob (Son)", contacts.get(1).getName());

        assertEquals(3, contacts.get(2).getPriority());
        assertEquals("Charlie (Neighbor)", contacts.get(2).getName());
    }

    @Test
    @DisplayName("SOS includes attending doctor information and elder health details")
    public void testSOSDoctorAndMedicalInfo() throws SQLException {
        SOSResult result = emergencyService.triggerSOS(elderId, "Fall detected");

        assertEquals("Eleanor Vance", result.getElder().getName());
        assertEquals("Hypertension", result.getElder().getMedicalConditions());
        assertEquals("O+", result.getElder().getBloodGroup());

        assertNotNull(result.getAssignedDoctor());
        assertEquals("Dr. Evans", result.getAssignedDoctor().getName());
        assertEquals("Cardiology", result.getAssignedDoctor().getSpecialization());

        String banner = result.toFormattedAlertBanner();
        assertTrue(banner.contains("EMERGENCY SOS TRIGGERED"));
        assertTrue(banner.contains("Alice (Daughter)"));
        assertTrue(banner.contains("Dr. Evans"));
    }

    @Test
    @DisplayName("Automatic critical alert is triggered and saved to database")
    public void testAutoCriticalAlert() throws SQLException {
        EmergencyAlert alert = emergencyService.triggerAutoCriticalAlert(elderId, "BP 190/125 mmHg (Hypertensive Crisis)");

        assertNotNull(alert);
        assertTrue(alert.getAlertId() > 0);
        assertTrue(alert.getReason().contains("AUTOMATIC ALERT"));
        assertTrue(alert.getReason().contains("190/125"));

        // Verify alert is in database
        List<EmergencyAlert> history = emergencyService.getAlertHistory(elderId);
        assertFalse(history.isEmpty());
        assertEquals(alert.getAlertId(), history.get(0).getAlertId());
    }

    @Test
    @DisplayName("Alert history retrieval returns records latest-first")
    public void testAlertHistory() throws SQLException {
        emergencyService.triggerSOS(elderId, "Incident 1");
        emergencyService.triggerSOS(elderId, "Incident 2");

        List<EmergencyAlert> history = emergencyService.getAlertHistory(elderId);
        assertEquals(2, history.size());
        assertTrue(history.get(0).getAlertId() > history.get(1).getAlertId());
    }
}
