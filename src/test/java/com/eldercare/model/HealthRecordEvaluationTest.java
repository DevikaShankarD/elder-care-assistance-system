package com.eldercare.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying dynamic polymorphic clinical evaluation and range boundary behavior
 * for BloodPressureRecord, BloodSugarRecord, and HeartRateRecord.
 */
public class HealthRecordEvaluationTest {

    private final LocalDateTime now = LocalDateTime.now();

    // =========================================================================
    // BLOOD PRESSURE TESTS
    // =========================================================================

    @Test
    @DisplayName("BP: Normal range (systolic 90-119, diastolic 60-79)")
    public void testBloodPressureNormal() {
        HealthRecord record = new BloodPressureRecord(1, 10, now, "Routine check", 115, 75);
        assertEquals(HealthStatus.NORMAL, record.evaluate());
        assertEquals("Blood Pressure", record.getType());
        assertTrue(record.getSummary().contains("Normal"));
    }

    @Test
    @DisplayName("BP: Low boundary (systolic < 90 or diastolic < 60)")
    public void testBloodPressureLow() {
        HealthRecord lowSys = new BloodPressureRecord(2, 10, now, "Dizziness", 85, 58);
        assertEquals(HealthStatus.LOW, lowSys.evaluate());

        HealthRecord lowDia = new BloodPressureRecord(3, 10, now, "Low diastolic", 100, 55);
        assertEquals(HealthStatus.LOW, lowDia.evaluate());
    }

    @Test
    @DisplayName("BP: Elevated range (systolic 120-129 and diastolic < 80)")
    public void testBloodPressureElevated() {
        HealthRecord elevated = new BloodPressureRecord(4, 10, now, "Borderline check", 125, 76);
        assertEquals(HealthStatus.ELEVATED, elevated.evaluate());
    }

    @Test
    @DisplayName("BP: High / Hypertension Stage 1 & 2 (systolic >= 130 or diastolic >= 80)")
    public void testBloodPressureHigh() {
        HealthRecord highSys = new BloodPressureRecord(5, 10, now, "High systolic", 135, 78);
        assertEquals(HealthStatus.HIGH, highSys.evaluate());

        HealthRecord highDia = new BloodPressureRecord(6, 10, now, "High diastolic", 124, 84);
        assertEquals(HealthStatus.HIGH, highDia.evaluate());
    }

    @Test
    @DisplayName("BP: Critical / Hypertensive Crisis (systolic >= 180 or diastolic >= 120)")
    public void testBloodPressureCritical() {
        HealthRecord critSys = new BloodPressureRecord(7, 10, now, "Severe crisis", 185, 95);
        assertEquals(HealthStatus.CRITICAL, critSys.evaluate());

        HealthRecord critDia = new BloodPressureRecord(8, 10, now, "Severe crisis dia", 160, 125);
        assertEquals(HealthStatus.CRITICAL, critDia.evaluate());
    }

    @Test
    @DisplayName("BP: Invalid pressure validation (systolic <= diastolic or out of bounds)")
    public void testBloodPressureValidationErrors() {
        // Systolic less than diastolic must throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                new BloodPressureRecord(9, 10, now, "Invalid", 80, 120));

        // Out of realistic clinical bounds
        assertThrows(IllegalArgumentException.class, () ->
                new BloodPressureRecord(10, 10, now, "Invalid", 280, 80));
    }

    // =========================================================================
    // BLOOD SUGAR TESTS
    // =========================================================================

    @Test
    @DisplayName("Blood Sugar: Fasting Normal (70 - 99 mg/dL)")
    public void testBloodSugarFastingNormal() {
        HealthRecord sugar = new BloodSugarRecord(11, 10, now, "Fasting", 88.0, ReadingType.FASTING);
        assertEquals(HealthStatus.NORMAL, sugar.evaluate());
        assertEquals("Blood Sugar", sugar.getType());
    }

    @Test
    @DisplayName("Blood Sugar: Fasting Low (< 70 mg/dL)")
    public void testBloodSugarFastingLow() {
        HealthRecord sugar = new BloodSugarRecord(12, 10, now, "Fasting", 65.0, ReadingType.FASTING);
        assertEquals(HealthStatus.LOW, sugar.evaluate());
    }

    @Test
    @DisplayName("Blood Sugar: Fasting Elevated (100 - 125 mg/dL prediabetes)")
    public void testBloodSugarFastingElevated() {
        HealthRecord sugar = new BloodSugarRecord(13, 10, now, "Fasting", 115.0, ReadingType.FASTING);
        assertEquals(HealthStatus.ELEVATED, sugar.evaluate());
    }

    @Test
    @DisplayName("Blood Sugar: Fasting High (126 - 249 mg/dL diabetes)")
    public void testBloodSugarFastingHigh() {
        HealthRecord sugar = new BloodSugarRecord(14, 10, now, "Fasting", 160.0, ReadingType.FASTING);
        assertEquals(HealthStatus.HIGH, sugar.evaluate());
    }

    @Test
    @DisplayName("Blood Sugar: Fasting Critical (< 50 or >= 250 mg/dL)")
    public void testBloodSugarFastingCritical() {
        HealthRecord lowCrit = new BloodSugarRecord(15, 10, now, "Severe hypo", 42.0, ReadingType.FASTING);
        assertEquals(HealthStatus.CRITICAL, lowCrit.evaluate());

        HealthRecord highCrit = new BloodSugarRecord(16, 10, now, "Ketoacidosis risk", 265.0, ReadingType.FASTING);
        assertEquals(HealthStatus.CRITICAL, highCrit.evaluate());
    }

    @Test
    @DisplayName("Blood Sugar: Post-Meal Normal (70 - 139 mg/dL)")
    public void testBloodSugarPostMealNormal() {
        HealthRecord sugar = new BloodSugarRecord(17, 10, now, "2hr post-lunch", 125.0, ReadingType.POST_MEAL);
        assertEquals(HealthStatus.NORMAL, sugar.evaluate());
    }

    @Test
    @DisplayName("Blood Sugar: Post-Meal Elevated (140 - 199 mg/dL)")
    public void testBloodSugarPostMealElevated() {
        HealthRecord sugar = new BloodSugarRecord(18, 10, now, "2hr post-dinner", 175.0, ReadingType.POST_MEAL);
        assertEquals(HealthStatus.ELEVATED, sugar.evaluate());
    }

    @Test
    @DisplayName("Blood Sugar: Post-Meal Critical (>= 300 mg/dL)")
    public void testBloodSugarPostMealCritical() {
        HealthRecord sugar = new BloodSugarRecord(19, 10, now, "Spike", 320.0, ReadingType.POST_MEAL);
        assertEquals(HealthStatus.CRITICAL, sugar.evaluate());
    }

    @Test
    @DisplayName("Blood Sugar: Invalid reading value (< 20 or > 600)")
    public void testBloodSugarValidation() {
        assertThrows(IllegalArgumentException.class, () ->
                new BloodSugarRecord(20, 10, now, "Unrealistic", 10.0, ReadingType.RANDOM));
        assertThrows(IllegalArgumentException.class, () ->
                new BloodSugarRecord(21, 10, now, "Unrealistic", 700.0, ReadingType.RANDOM));
    }

    // =========================================================================
    // HEART RATE TESTS
    // =========================================================================

    @Test
    @DisplayName("Heart Rate: Normal adult pulse (60 - 100 bpm)")
    public void testHeartRateNormal() {
        HealthRecord hr = new HeartRateRecord(22, 10, now, "Resting pulse", 72);
        assertEquals(HealthStatus.NORMAL, hr.evaluate());
        assertEquals("Heart Rate", hr.getType());
    }

    @Test
    @DisplayName("Heart Rate: Low / Bradycardia (40 - 59 bpm)")
    public void testHeartRateLow() {
        HealthRecord hr = new HeartRateRecord(23, 10, now, "Slow pulse", 52);
        assertEquals(HealthStatus.LOW, hr.evaluate());
    }

    @Test
    @DisplayName("Heart Rate: Elevated (101 - 120 bpm)")
    public void testHeartRateElevated() {
        HealthRecord hr = new HeartRateRecord(24, 10, now, "Mild tachycardia", 110);
        assertEquals(HealthStatus.ELEVATED, hr.evaluate());
    }

    @Test
    @DisplayName("Heart Rate: High (121 - 139 bpm)")
    public void testHeartRateHigh() {
        HealthRecord hr = new HeartRateRecord(25, 10, now, "Tachycardia", 130);
        assertEquals(HealthStatus.HIGH, hr.evaluate());
    }

    @Test
    @DisplayName("Heart Rate: Critical (< 40 bpm or >= 140 bpm)")
    public void testHeartRateCritical() {
        HealthRecord severeBradycardia = new HeartRateRecord(26, 10, now, "Severe bradycardia", 36);
        assertEquals(HealthStatus.CRITICAL, severeBradycardia.evaluate());

        HealthRecord severeTachycardia = new HeartRateRecord(27, 10, now, "Severe tachycardia", 155);
        assertEquals(HealthStatus.CRITICAL, severeTachycardia.evaluate());
    }
}
