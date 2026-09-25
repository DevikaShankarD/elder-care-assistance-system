package com.eldercare.dao;

import com.eldercare.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying complete Create, Read, Update, and Delete (CRUD) operations
 * across all entity DAOs against an isolated temporary SQLite database.
 */
public class DAOCrudTest {

    private static final String TEST_DB = "test_daos.db";
    private static final String TEST_JDBC = "jdbc:sqlite:" + TEST_DB;

    private DatabaseManager dbManager;
    private ElderDAO elderDAO;
    private CaregiverDAO caregiverDAO;
    private DoctorDAO doctorDAO;
    private EmergencyContactDAO contactDAO;
    private MedicationDAO medicationDAO;
    private MedicationLogDAO medicationLogDAO;
    private AppointmentDAO appointmentDAO;
    private HealthRecordDAO healthRecordDAO;
    private EmergencyAlertDAO alertDAO;

    @BeforeEach
    public void setUp() {
        DatabaseManager.resetInstance();
        new File(TEST_DB).delete();

        dbManager = DatabaseManager.getInstance(TEST_JDBC);
        elderDAO = new ElderDAO(dbManager);
        caregiverDAO = new CaregiverDAO(dbManager);
        doctorDAO = new DoctorDAO(dbManager);
        contactDAO = new EmergencyContactDAO(dbManager);
        medicationDAO = new MedicationDAO(dbManager);
        medicationLogDAO = new MedicationLogDAO(dbManager);
        appointmentDAO = new AppointmentDAO(dbManager);
        healthRecordDAO = new HealthRecordDAO(dbManager);
        alertDAO = new EmergencyAlertDAO(dbManager);
    }

    @AfterEach
    public void tearDown() {
        DatabaseManager.resetInstance();
        new File(TEST_DB).delete();
    }

    @Test
    @DisplayName("ElderDAO: Full CRUD and searchByName")
    public void testElderDAOCrud() throws SQLException {
        Elder elder = new Elder(0, "Samuel Green", "9876543210", "15 Elm St", 72, "Male", "O+", "Hypertension");
        int id = elderDAO.add(elder);
        assertTrue(id > 0);

        // Get by ID
        Optional<Elder> fetched = elderDAO.getById(id);
        assertTrue(fetched.isPresent());
        assertEquals("Samuel Green", fetched.get().getName());
        assertEquals("Elder", fetched.get().getRole());

        // Update
        Elder toUpdate = fetched.get();
        toUpdate.setName("Samuel J. Green");
        toUpdate.setAge(73);
        assertTrue(elderDAO.update(toUpdate));

        Elder updated = elderDAO.getById(id).orElseThrow();
        assertEquals("Samuel J. Green", updated.getName());
        assertEquals(73, updated.getAge());

        // Search by name
        List<Elder> searchResults = elderDAO.searchByName("green");
        assertEquals(1, searchResults.size());

        // Delete
        assertTrue(elderDAO.delete(id));
        assertTrue(elderDAO.getById(id).isEmpty());
    }

    @Test
    @DisplayName("CaregiverDAO: Full CRUD")
    public void testCaregiverDAOCrud() throws SQLException {
        Caregiver caregiver = new Caregiver(0, "Maria Gomez", "9876543212", "18 Maple Ave", "Professional Nurse");
        int id = caregiverDAO.add(caregiver);
        assertTrue(id > 0);

        Optional<Caregiver> fetched = caregiverDAO.getById(id);
        assertTrue(fetched.isPresent());
        assertEquals("Caregiver", fetched.get().getRole());

        Caregiver toUpdate = fetched.get();
        toUpdate.setRelationshipToElder("Senior Care Specialist");
        assertTrue(caregiverDAO.update(toUpdate));

        assertEquals("Senior Care Specialist", caregiverDAO.getById(id).orElseThrow().getRelationshipToElder());
        assertTrue(caregiverDAO.delete(id));
    }

    @Test
    @DisplayName("DoctorDAO: Full CRUD and search")
    public void testDoctorDAOCrud() throws SQLException {
        Doctor doc = new Doctor(0, "Dr. Harold Finch", "Neurology", "Mount Sinai", "9876543213");
        int id = doctorDAO.add(doc);
        assertTrue(id > 0);

        Optional<Doctor> fetched = doctorDAO.getById(id);
        assertTrue(fetched.isPresent());
        assertEquals("Dr. Harold Finch", fetched.get().getName());

        List<Doctor> found = doctorDAO.searchByName("finch");
        assertEquals(1, found.size());

        fetched.get().setHospital("Metro Hospital");
        assertTrue(doctorDAO.update(fetched.get()));
        assertEquals("Metro Hospital", doctorDAO.getById(id).orElseThrow().getHospital());

        assertTrue(doctorDAO.delete(id));
    }

    @Test
    @DisplayName("EmergencyContactDAO: CRUD and getByElderId sorted by priority")
    public void testEmergencyContactDAOCrud() throws SQLException {
        int eId = elderDAO.add(new Elder(0, "Test Elder", "9876543210", "", 70, "M", "A+", ""));

        // Add 2 contacts with priority 2 and 1
        contactDAO.add(new EmergencyContact(0, eId, "Second Contact", "Friend", "9871111112", 2));
        contactDAO.add(new EmergencyContact(0, eId, "First Contact", "Daughter", "9871111111", 1));

        List<EmergencyContact> contacts = contactDAO.getByElderId(eId);
        assertEquals(2, contacts.size());
        assertEquals(1, contacts.get(0).getPriority());
        assertEquals("First Contact", contacts.get(0).getName());
        assertEquals(2, contacts.get(1).getPriority());

        // Update contact
        EmergencyContact c1 = contacts.get(0);
        c1.setName("First Contact Updated");
        assertTrue(contactDAO.update(c1));
        assertEquals("First Contact Updated", contactDAO.getById(c1.getContactId()).orElseThrow().getName());

        assertTrue(contactDAO.delete(c1.getContactId()));
    }

    @Test
    @DisplayName("MedicationDAO & MedicationLogDAO: CRUD operations")
    public void testMedicationAndLogDAOCrud() throws SQLException {
        int eId = elderDAO.add(new Elder(0, "Elder Med", "9876543210", "", 75, "F", "O-", ""));

        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(1);
        Medication med = new Medication(0, eId, "Metoprolol", "25mg", 1, List.of(LocalTime.of(8, 0)), start, end, "Take with breakfast");
        int medId = medicationDAO.add(med);
        assertTrue(medId > 0);

        Optional<Medication> fetchedMed = medicationDAO.getById(medId);
        assertTrue(fetchedMed.isPresent());
        assertEquals(1, fetchedMed.get().getDoseTimes().size());
        assertEquals(LocalTime.of(8, 0), fetchedMed.get().getDoseTimes().get(0));

        // Add MedicationLog
        LocalDateTime scheduledTime = start.atTime(8, 0);
        MedicationLog log = new MedicationLog(0, medId, scheduledTime, DoseStatus.TAKEN, LocalDateTime.now());
        int logId = medicationLogDAO.add(log);
        assertTrue(logId > 0);

        Optional<MedicationLog> fetchedLog = medicationLogDAO.getById(logId);
        assertTrue(fetchedLog.isPresent());
        assertEquals(DoseStatus.TAKEN, fetchedLog.get().getStatus());

        fetchedLog.get().setStatus(DoseStatus.SKIPPED);
        assertTrue(medicationLogDAO.update(fetchedLog.get()));
        assertEquals(DoseStatus.SKIPPED, medicationLogDAO.getById(logId).orElseThrow().getStatus());

        assertTrue(medicationLogDAO.delete(logId));
        assertTrue(medicationDAO.delete(medId));
    }

    @Test
    @DisplayName("HealthRecordDAO: Polymorphic CRUD for BloodPressure, BloodSugar, and HeartRate")
    public void testHealthRecordDAOPolymorphicCrud() throws SQLException {
        int eId = elderDAO.add(new Elder(0, "Elder Health", "9876543210", "", 80, "M", "AB+", ""));
        LocalDateTime now = LocalDateTime.now();

        // 1. Blood Pressure Record
        BloodPressureRecord bp = new BloodPressureRecord(0, eId, now, "BP Log", 120, 80);
        int bpId = healthRecordDAO.add(bp);
        HealthRecord fetchedBp = healthRecordDAO.getById(bpId).orElseThrow();
        assertInstanceOf(BloodPressureRecord.class, fetchedBp);
        assertEquals(120, ((BloodPressureRecord) fetchedBp).getSystolic());
        assertEquals(80, ((BloodPressureRecord) fetchedBp).getDiastolic());

        // 2. Blood Sugar Record
        BloodSugarRecord sugar = new BloodSugarRecord(0, eId, now, "Sugar Log", 95.5, ReadingType.FASTING);
        int sugarId = healthRecordDAO.add(sugar);
        HealthRecord fetchedSugar = healthRecordDAO.getById(sugarId).orElseThrow();
        assertInstanceOf(BloodSugarRecord.class, fetchedSugar);
        assertEquals(95.5, ((BloodSugarRecord) fetchedSugar).getValueMgDl(), 0.01);
        assertEquals(ReadingType.FASTING, ((BloodSugarRecord) fetchedSugar).getReadingType());

        // 3. Heart Rate Record
        HeartRateRecord hr = new HeartRateRecord(0, eId, now, "Pulse Log", 72);
        int hrId = healthRecordDAO.add(hr);
        HealthRecord fetchedHr = healthRecordDAO.getById(hrId).orElseThrow();
        assertInstanceOf(HeartRateRecord.class, fetchedHr);
        assertEquals(72, ((HeartRateRecord) fetchedHr).getBpm());

        // Delete records
        assertTrue(healthRecordDAO.delete(bpId));
        assertTrue(healthRecordDAO.delete(sugarId));
        assertTrue(healthRecordDAO.delete(hrId));
    }

    @Test
    @DisplayName("AppointmentDAO & EmergencyAlertDAO: CRUD operations")
    public void testAppointmentAndAlertDAOCrud() throws SQLException {
        int eId = elderDAO.add(new Elder(0, "Elder Appt", "9876543210", "", 70, "F", "O+", ""));
        int dId = doctorDAO.add(new Doctor(0, "Dr. Banner", "Cardiology", "General Hospital", "9876543220"));

        // Appointment CRUD
        LocalDateTime apptTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        Appointment appt = new Appointment(0, eId, dId, apptTime, "Checkup", AppointmentStatus.SCHEDULED);
        int aId = appointmentDAO.add(appt);
        assertTrue(aId > 0);

        Appointment fetchedAppt = appointmentDAO.getById(aId).orElseThrow();
        assertEquals("Checkup", fetchedAppt.getPurpose());
        fetchedAppt.setStatus(AppointmentStatus.COMPLETED);
        assertTrue(appointmentDAO.update(fetchedAppt));
        assertEquals(AppointmentStatus.COMPLETED, appointmentDAO.getById(aId).orElseThrow().getStatus());
        assertTrue(appointmentDAO.delete(aId));

        // EmergencyAlert CRUD
        EmergencyAlert alert = new EmergencyAlert(0, eId, LocalDateTime.now(), "Test Alert", "P1: Alice (9876543210)");
        int alertId = alertDAO.add(alert);
        assertTrue(alertId > 0);

        EmergencyAlert fetchedAlert = alertDAO.getById(alertId).orElseThrow();
        assertEquals("Test Alert", fetchedAlert.getReason());
        assertTrue(alertDAO.delete(alertId));
    }
}
