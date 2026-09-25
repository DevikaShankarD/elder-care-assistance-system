package com.eldercare.service;

import com.eldercare.dao.DatabaseManager;
import com.eldercare.dao.ElderDAO;
import com.eldercare.dao.MedicationDAO;
import com.eldercare.dao.MedicationLogDAO;
import com.eldercare.model.DoseStatus;
import com.eldercare.model.Elder;
import com.eldercare.model.Medication;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ReminderService: today's schedule, due/overdue calculation, dose logging,
 * and adherence calculations.
 */
public class ReminderServiceTest {

    private static final String TEST_DB = "test_reminders.db";
    private static final String TEST_JDBC = "jdbc:sqlite:" + TEST_DB;

    private DatabaseManager dbManager;
    private ReminderService reminderService;
    private MedicationDAO medicationDAO;
    private MedicationLogDAO medicationLogDAO;
    private ElderDAO elderDAO;

    private int elderId;

    @BeforeEach
    public void setUp() throws SQLException {
        DatabaseManager.resetInstance();
        new File(TEST_DB).delete();

        dbManager = DatabaseManager.getInstance(TEST_JDBC);
        medicationDAO = new MedicationDAO(dbManager);
        medicationLogDAO = new MedicationLogDAO(dbManager);
        elderDAO = new ElderDAO(dbManager);
        reminderService = new ReminderService(medicationDAO, medicationLogDAO);

        elderId = elderDAO.add(new Elder(0, "Elder Test", "9876543210", "123 St", 70, "Male", "B+", "Diabetes"));
    }

    @AfterEach
    public void tearDown() {
        DatabaseManager.resetInstance();
        new File(TEST_DB).delete();
    }

    @Test
    @DisplayName("Today's schedule lists active medication doses ordered chronologically")
    public void testTodayScheduleActiveMedication() throws SQLException {
        LocalDate today = LocalDate.now();
        Medication med = new Medication(0, elderId, "Metformin", "500mg", 2,
                List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                today.minusDays(5), today.plusDays(10), "With water");
        medicationDAO.add(med);

        List<DoseScheduleItem> schedule = reminderService.getTodaySchedule(elderId, today);
        assertEquals(2, schedule.size());
        assertEquals(LocalTime.of(8, 0), schedule.get(0).getDoseTime());
        assertEquals(LocalTime.of(20, 0), schedule.get(1).getDoseTime());
        assertNull(schedule.get(0).getStatus(), "Status should be null/pending before logging");
    }

    @Test
    @DisplayName("Inactive medications (expired) are excluded from today's schedule")
    public void testTodayScheduleExcludesExpiredMedication() throws SQLException {
        LocalDate today = LocalDate.now();
        // Expired medication
        Medication expired = new Medication(0, elderId, "Amoxicillin", "250mg", 1,
                List.of(LocalTime.of(12, 0)),
                today.minusDays(20), today.minusDays(5), "Antibiotic course");
        medicationDAO.add(expired);

        List<DoseScheduleItem> schedule = reminderService.getTodaySchedule(elderId, today);
        assertTrue(schedule.isEmpty(), "Expired medication should not be scheduled today");
    }

    @Test
    @DisplayName("Due or overdue dose detection based on current time")
    public void testDueOrOverdueDoses() throws SQLException {
        LocalDate today = LocalDate.now();
        Medication med = new Medication(0, elderId, "Aspirin", "100mg", 2,
                List.of(LocalTime.of(8, 0), LocalTime.of(18, 0)),
                today.minusDays(1), today.plusDays(10), "Daily");
        medicationDAO.add(med);

        // Simulate evaluation at 12:00 PM (8:00 AM dose is overdue, 18:00 dose is not yet due)
        LocalDateTime evalTime = today.atTime(12, 0);
        List<DoseScheduleItem> overdue = reminderService.getDueOrOverdueDoses(elderId, evalTime);

        assertEquals(1, overdue.size());
        assertEquals(LocalTime.of(8, 0), overdue.get(0).getDoseTime());
    }

    @Test
    @DisplayName("Record dose status and update existing log")
    public void testRecordDoseAndAdherence() throws SQLException {
        LocalDate today = LocalDate.now();
        Medication med = new Medication(0, elderId, "Atorvastatin", "20mg", 1,
                List.of(LocalTime.of(21, 0)),
                today.minusDays(5), today.plusDays(5), "Nightly");
        int medId = medicationDAO.add(med);

        LocalDateTime scheduledTime = today.atTime(21, 0);

        // Record as TAKEN
        int logId1 = reminderService.recordDose(medId, scheduledTime, DoseStatus.TAKEN);
        assertTrue(logId1 > 0);

        // Updating same scheduled time to MISSED
        int logId2 = reminderService.recordDose(medId, scheduledTime, DoseStatus.MISSED);
        assertEquals(logId1, logId2, "Should update the same log entry rather than duplicate");

        assertEquals(DoseStatus.MISSED, medicationLogDAO.getById(logId1).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Calculate medication adherence percentage accurately")
    public void testCalculateMedicationAdherence() throws SQLException {
        LocalDate today = LocalDate.now();
        Medication med = new Medication(0, elderId, "Insulin", "10 units", 1,
                List.of(LocalTime.of(7, 30)),
                today.minusDays(4), today.plusDays(4), "Daily");
        int medId = medicationDAO.add(med);

        // Log 4 days: 3 TAKEN, 1 MISSED -> 75% adherence
        reminderService.recordDose(medId, today.minusDays(4).atTime(7, 30), DoseStatus.TAKEN);
        reminderService.recordDose(medId, today.minusDays(3).atTime(7, 30), DoseStatus.TAKEN);
        reminderService.recordDose(medId, today.minusDays(2).atTime(7, 30), DoseStatus.TAKEN);
        reminderService.recordDose(medId, today.minusDays(1).atTime(7, 30), DoseStatus.MISSED);

        double adherence = reminderService.calculateMedicationAdherence(medId);
        assertEquals(75.0, adherence, 0.01);
    }

    @Test
    @DisplayName("Calculate overall adherence across multiple medications")
    public void testCalculateElderOverallAdherence() throws SQLException {
        LocalDate today = LocalDate.now();

        // Med 1: 2 doses taken out of 2 = 100%
        Medication med1 = new Medication(0, elderId, "Med A", "10mg", 1,
                List.of(LocalTime.of(8, 0)), today.minusDays(2), today.plusDays(2), "");
        int m1 = medicationDAO.add(med1);
        reminderService.recordDose(m1, today.minusDays(2).atTime(8, 0), DoseStatus.TAKEN);
        reminderService.recordDose(m1, today.minusDays(1).atTime(8, 0), DoseStatus.TAKEN);

        // Med 2: 0 doses taken out of 2 = 0%
        Medication med2 = new Medication(0, elderId, "Med B", "20mg", 1,
                List.of(LocalTime.of(9, 0)), today.minusDays(2), today.plusDays(2), "");
        int m2 = medicationDAO.add(med2);
        reminderService.recordDose(m2, today.minusDays(2).atTime(9, 0), DoseStatus.MISSED);
        reminderService.recordDose(m2, today.minusDays(1).atTime(9, 0), DoseStatus.MISSED);

        // Total: 2 taken out of 4 = 50.0%
        double overall = reminderService.calculateElderOverallAdherence(elderId);
        assertEquals(50.0, overall, 0.01);
    }
}
