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
 * Tests for HealthMonitor: polymorphic reading addition, automatic critical alert dispatch,
 * 7/30-day averages, trend trajectory detection, and abnormal filtering.
 */
public class HealthMonitorTest {

    private static final String TEST_DB = "test_healthmonitor.db";
    private static final String TEST_JDBC = "jdbc:sqlite:" + TEST_DB;

    private DatabaseManager dbManager;
    private HealthMonitor healthMonitor;
    private EmergencyService emergencyService;
    private HealthRecordDAO healthRecordDAO;
    private EmergencyAlertDAO alertDAO;
    private ElderDAO elderDAO;

    private int elderId;

    @BeforeEach
    public void setUp() throws SQLException {
        DatabaseManager.resetInstance();
        new File(TEST_DB).delete();

        dbManager = DatabaseManager.getInstance(TEST_JDBC);
        healthRecordDAO = new HealthRecordDAO(dbManager);
        alertDAO = new EmergencyAlertDAO(dbManager);
        elderDAO = new ElderDAO(dbManager);
        EmergencyContactDAO contactDAO = new EmergencyContactDAO(dbManager);
        DoctorDAO doctorDAO = new DoctorDAO(dbManager);
        AppointmentDAO appointmentDAO = new AppointmentDAO(dbManager);

        emergencyService = new EmergencyService(alertDAO, elderDAO, contactDAO, doctorDAO, appointmentDAO);
        healthMonitor = new HealthMonitor(healthRecordDAO, emergencyService);

        elderId = elderDAO.add(new Elder(0, "Test Elder", "9876543210", "Address", 75, "Male", "A+", "None"));
    }

    @AfterEach
    public void tearDown() {
        DatabaseManager.resetInstance();
        new File(TEST_DB).delete();
    }

    @Test
    @DisplayName("Polymorphic reading addition persists correctly into database")
    public void testAddReadingPolymorphic() throws SQLException {
        LocalDateTime now = LocalDateTime.now();

        HealthRecord bp = new BloodPressureRecord(0, elderId, now, "BP Check", 120, 80);
        HealthRecord savedBp = healthMonitor.addReading(bp);
        assertTrue(savedBp.getRecordId() > 0);

        HealthRecord sugar = new BloodSugarRecord(0, elderId, now, "Sugar Check", 95.0, ReadingType.FASTING);
        HealthRecord savedSugar = healthMonitor.addReading(sugar);
        assertTrue(savedSugar.getRecordId() > 0);

        HealthRecord hr = new HeartRateRecord(0, elderId, now, "Pulse Check", 72);
        HealthRecord savedHr = healthMonitor.addReading(hr);
        assertTrue(savedHr.getRecordId() > 0);

        List<HealthRecord> all = healthMonitor.getAllReadings(elderId);
        assertEquals(3, all.size());
    }

    @Test
    @DisplayName("Adding a CRITICAL health reading automatically triggers an emergency alert")
    public void testCriticalReadingAutoAlert() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        // Hypertensive crisis BP reading (190/125 is CRITICAL)
        HealthRecord criticalBp = new BloodPressureRecord(0, elderId, now, "Emergency measurement", 190, 125);
        assertEquals(HealthStatus.CRITICAL, criticalBp.evaluate());

        healthMonitor.addReading(criticalBp);

        // Verify alert was automatically created in database
        List<EmergencyAlert> alerts = alertDAO.getByElderId(elderId);
        assertFalse(alerts.isEmpty(), "Emergency alert must be triggered automatically on critical reading");
        assertTrue(alerts.get(0).getReason().contains("AUTOMATIC ALERT"));
        assertTrue(alerts.get(0).getReason().contains("Blood Pressure"));
    }

    @Test
    @DisplayName("Moving averages (7-day and 30-day) computed accurately")
    public void testMovingAveragesCalculation() throws SQLException {
        LocalDateTime now = LocalDateTime.now();

        // Add 2 BP readings within the last 7 days: 120/80 and 130/84 -> avg 125.0 / 82.0
        healthMonitor.addReading(new BloodPressureRecord(0, elderId, now.minusDays(2), "", 120, 80));
        healthMonitor.addReading(new BloodPressureRecord(0, elderId, now.minusDays(1), "", 130, 84));

        // Add 1 BP reading from 20 days ago (outside 7 days, inside 30 days): 140/90
        healthMonitor.addReading(new BloodPressureRecord(0, elderId, now.minusDays(20), "", 140, 90));

        HealthAverage avg7 = healthMonitor.get7DayAverage(elderId, HealthRecordDAO.TYPE_BP);
        assertEquals(2, avg7.getReadingCount());
        assertEquals(125.0, avg7.getPrimaryAverage(), 0.01);
        assertEquals(82.0, avg7.getSecondaryAverage(), 0.01);

        HealthAverage avg30 = healthMonitor.get30DayAverage(elderId, HealthRecordDAO.TYPE_BP);
        assertEquals(3, avg30.getReadingCount());
        assertEquals(130.0, avg30.getPrimaryAverage(), 0.01);
        assertEquals(84.66, avg30.getSecondaryAverage(), 0.1);
    }

    @Test
    @DisplayName("Trend trajectory detection: Rising, Falling, Stable")
    public void testTrendDetection() throws SQLException {
        LocalDateTime now = LocalDateTime.now();

        // 1. Rising Heart Rate readings: 65, 70, 78, 85
        healthMonitor.addReading(new HeartRateRecord(0, elderId, now.minusDays(4), "", 65));
        healthMonitor.addReading(new HeartRateRecord(0, elderId, now.minusDays(3), "", 70));
        healthMonitor.addReading(new HeartRateRecord(0, elderId, now.minusDays(2), "", 78));
        healthMonitor.addReading(new HeartRateRecord(0, elderId, now.minusDays(1), "", 85));

        Trend hrTrend = healthMonitor.detectTrend(elderId, HealthRecordDAO.TYPE_HEART);
        assertEquals(Trend.RISING, hrTrend);

        // 2. Stable Blood Sugar readings: 95, 96, 95, 94
        healthMonitor.addReading(new BloodSugarRecord(0, elderId, now.minusDays(4), "", 95.0, ReadingType.FASTING));
        healthMonitor.addReading(new BloodSugarRecord(0, elderId, now.minusDays(3), "", 96.0, ReadingType.FASTING));
        healthMonitor.addReading(new BloodSugarRecord(0, elderId, now.minusDays(2), "", 95.0, ReadingType.FASTING));
        healthMonitor.addReading(new BloodSugarRecord(0, elderId, now.minusDays(1), "", 94.0, ReadingType.FASTING));

        Trend sugarTrend = healthMonitor.detectTrend(elderId, HealthRecordDAO.TYPE_SUGAR);
        assertEquals(Trend.STABLE, sugarTrend);
    }

    @Test
    @DisplayName("Filter abnormal readings flags all non-NORMAL measurements")
    public void testGetAbnormalReadings() throws SQLException {
        LocalDateTime now = LocalDateTime.now();

        // Normal BP (115/75)
        healthMonitor.addReading(new BloodPressureRecord(0, elderId, now.minusDays(3), "", 115, 75));
        // High BP (140/85) -> Abnormal
        healthMonitor.addReading(new BloodPressureRecord(0, elderId, now.minusDays(2), "", 140, 85));
        // Normal Sugar (85 fasting)
        healthMonitor.addReading(new BloodSugarRecord(0, elderId, now.minusDays(1), "", 85.0, ReadingType.FASTING));
        // Elevated Sugar (115 fasting) -> Abnormal
        healthMonitor.addReading(new BloodSugarRecord(0, elderId, now, "", 115.0, ReadingType.FASTING));

        List<HealthRecord> abnormal = healthMonitor.getAbnormalReadings(elderId);
        assertEquals(2, abnormal.size());
    }
}
