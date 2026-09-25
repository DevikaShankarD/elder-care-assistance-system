package com.eldercare.util;

import com.eldercare.dao.*;
import com.eldercare.model.*;
import com.eldercare.service.HealthMonitor;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Seeds comprehensive, realistic sample data for demonstration and testing.
 * Seeds 2 elders, 3 doctors, 1 caregiver, emergency contacts, medications,
 * dose logs, 2 weeks of health readings, appointments, and emergency alerts.
 */
public class SampleDataLoader {

    private final ElderDAO elderDAO;
    private final CaregiverDAO caregiverDAO;
    private final DoctorDAO doctorDAO;
    private final EmergencyContactDAO contactDAO;
    private final MedicationDAO medicationDAO;
    private final MedicationLogDAO medicationLogDAO;
    private final AppointmentDAO appointmentDAO;
    private final HealthRecordDAO healthRecordDAO;
    private final EmergencyAlertDAO alertDAO;

    public SampleDataLoader() {
        this.elderDAO = new ElderDAO();
        this.caregiverDAO = new CaregiverDAO();
        this.doctorDAO = new DoctorDAO();
        this.contactDAO = new EmergencyContactDAO();
        this.medicationDAO = new MedicationDAO();
        this.medicationLogDAO = new MedicationLogDAO();
        this.appointmentDAO = new AppointmentDAO();
        this.healthRecordDAO = new HealthRecordDAO();
        this.alertDAO = new EmergencyAlertDAO();
    }

    public SampleDataLoader(DatabaseManager dbManager) {
        this.elderDAO = new ElderDAO(dbManager);
        this.caregiverDAO = new CaregiverDAO(dbManager);
        this.doctorDAO = new DoctorDAO(dbManager);
        this.contactDAO = new EmergencyContactDAO(dbManager);
        this.medicationDAO = new MedicationDAO(dbManager);
        this.medicationLogDAO = new MedicationLogDAO(dbManager);
        this.appointmentDAO = new AppointmentDAO(dbManager);
        this.healthRecordDAO = new HealthRecordDAO(dbManager);
        this.alertDAO = new EmergencyAlertDAO(dbManager);
    }

    /**
     * Seeds initial sample records into the database.
     *
     * @throws SQLException on database error
     */
    public void loadSampleData() throws SQLException {
        // 1. Elders
        Elder elder1 = new Elder(0, "John Smith", "9876543210",
                "14 Elm Street, City Center", 78, "Male", "O+", "Hypertension, Type 2 Diabetes");
        int elder1Id = elderDAO.add(elder1);

        Elder elder2 = new Elder(0, "Mary Davis", "9123456780",
                "42 Pine Avenue, Westside", 75, "Female", "A+", "Arrhythmia, Mild Arthritis");
        int elder2Id = elderDAO.add(elder2);

        // 2. Caregiver
        Caregiver caregiver = new Caregiver(0, "Sarah Wilson", "9899988877",
                "8 Oak Lane, City Center", "Certified Caregiver");
        caregiverDAO.add(caregiver);

        // 3. Doctors (3 doctors)
        Doctor doc1 = new Doctor(0, "Dr. James Anderson", "Cardiology", "City Hospital", "9845112233");
        int doc1Id = doctorDAO.add(doc1);

        Doctor doc2 = new Doctor(0, "Dr. Emily Clark", "General Medicine", "City Clinic", "9845223344");
        int doc2Id = doctorDAO.add(doc2);

        Doctor doc3 = new Doctor(0, "Dr. Michael Brown", "Geriatric Specialist", "Metro Hospital", "9845334455");
        int doc3Id = doctorDAO.add(doc3);

        // 4. Emergency Contacts (with priorities)
        EmergencyContact c1 = new EmergencyContact(0, elder1Id, "David Smith", "Son", "9871122334", 1);
        EmergencyContact c2 = new EmergencyContact(0, elder1Id, "Susan Smith", "Daughter", "9872233445", 2);
        contactDAO.add(c1);
        contactDAO.add(c2);

        EmergencyContact c3 = new EmergencyContact(0, elder2Id, "Thomas Davis", "Son", "9811122233", 1);
        EmergencyContact c4 = new EmergencyContact(0, elder2Id, "Nancy Davis", "Daughter", "9822233344", 2);
        contactDAO.add(c3);
        contactDAO.add(c4);

        // 5. Medications
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(14);
        LocalDate endDate = today.plusMonths(3);

        Medication med1 = new Medication(0, elder1Id, "Metformin", "500 mg", 2,
                List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                startDate, endDate, "Take with meal");
        int med1Id = medicationDAO.add(med1);

        Medication med2 = new Medication(0, elder1Id, "Lisinopril", "10 mg", 1,
                List.of(LocalTime.of(9, 0)),
                startDate, endDate, "Take in the morning with water");
        int med2Id = medicationDAO.add(med2);

        Medication med3 = new Medication(0, elder2Id, "Amiodarone", "200 mg", 1,
                List.of(LocalTime.of(8, 30)),
                startDate, endDate, "Take with breakfast");
        int med3Id = medicationDAO.add(med3);

        Medication med4 = new Medication(0, elder2Id, "Low-Dose Aspirin", "75 mg", 1,
                List.of(LocalTime.of(13, 0)),
                startDate, endDate, "Take after lunch");
        int med4Id = medicationDAO.add(med4);

        // 6. Medication Logs (simulating 14 days of compliance)
        for (int i = 14; i >= 1; i--) {
            LocalDate logDate = today.minusDays(i);
            // Metformin morning dose (mostly taken, occasional missed)
            DoseStatus statusMorning = (i == 4) ? DoseStatus.MISSED : DoseStatus.TAKEN;
            medicationLogDAO.add(new MedicationLog(0, med1Id, logDate.atTime(8, 0), statusMorning, logDate.atTime(8, 15)));

            // Metformin evening dose
            DoseStatus statusEvening = (i == 10) ? DoseStatus.SKIPPED : DoseStatus.TAKEN;
            medicationLogDAO.add(new MedicationLog(0, med1Id, logDate.atTime(20, 0), statusEvening, logDate.atTime(20, 10)));

            // Lisinopril
            medicationLogDAO.add(new MedicationLog(0, med2Id, logDate.atTime(9, 0), DoseStatus.TAKEN, logDate.atTime(9, 5)));

            // Amiodarone for Arthur
            DoseStatus arthurDose = (i == 7) ? DoseStatus.MISSED : DoseStatus.TAKEN;
            medicationLogDAO.add(new MedicationLog(0, med3Id, logDate.atTime(8, 30), arthurDose, logDate.atTime(8, 40)));
        }

        // 7. Health Readings (2 weeks of realistic readings)
        // Blood Pressure readings over 14 days for Margaret
        int[][] bpReadings = {
                {132, 84}, {130, 82}, {128, 80}, {126, 78}, {124, 78},
                {122, 76}, {120, 75}, {122, 76}, {125, 80}, {128, 82},
                {130, 84}, {134, 86}, {136, 88}, {138, 90}
        };
        for (int i = 0; i < bpReadings.length; i++) {
            LocalDateTime readingTime = today.minusDays(14 - i).atTime(8, 15);
            BloodPressureRecord bp = new BloodPressureRecord(0, elder1Id, readingTime,
                    "Routine morning measurement", bpReadings[i][0], bpReadings[i][1]);
            healthRecordDAO.add(bp);
        }

        // Blood Sugar readings over 14 days for Margaret
        double[] sugarReadings = {
                105.0, 110.0, 98.0, 95.0, 92.0,
                89.0, 94.0, 99.0, 104.0, 112.0,
                118.0, 122.0, 128.0, 135.0
        };
        for (int i = 0; i < sugarReadings.length; i++) {
            LocalDateTime readingTime = today.minusDays(14 - i).atTime(7, 30);
            ReadingType type = (i % 2 == 0) ? ReadingType.FASTING : ReadingType.POST_MEAL;
            BloodSugarRecord bs = new BloodSugarRecord(0, elder1Id, readingTime,
                    "Fasting / Post-meal check", sugarReadings[i], type);
            healthRecordDAO.add(bs);
        }

        // Heart Rate readings over 14 days for Arthur
        int[] hrReadings = {
                72, 74, 70, 68, 69,
                71, 73, 75, 78, 80,
                82, 85, 84, 88
        };
        for (int i = 0; i < hrReadings.length; i++) {
            LocalDateTime readingTime = today.minusDays(14 - i).atTime(9, 30);
            HeartRateRecord hr = new HeartRateRecord(0, elder2Id, readingTime,
                    "Morning resting pulse", hrReadings[i]);
            healthRecordDAO.add(hr);
        }

        // 8. Appointments
        // Margaret: 1 completed in the past, 1 upcoming
        Appointment pastAppt = new Appointment(0, elder1Id, doc2Id,
                today.minusDays(10).atTime(10, 0), "Routine Diabetes Checkup", AppointmentStatus.COMPLETED);
        appointmentDAO.add(pastAppt);

        Appointment upcomingAppt1 = new Appointment(0, elder1Id, doc1Id,
                today.plusDays(4).atTime(11, 0), "Blood Pressure & Cardiac Evaluation", AppointmentStatus.SCHEDULED);
        appointmentDAO.add(upcomingAppt1);

        // Arthur: 1 upcoming appointment
        Appointment upcomingAppt2 = new Appointment(0, elder2Id, doc1Id,
                today.plusDays(5).atTime(15, 30), "Arrhythmia Follow-up & ECG Review", AppointmentStatus.SCHEDULED);
        appointmentDAO.add(upcomingAppt2);

        // 9. Emergency Alert
        EmergencyAlert pastAlert = new EmergencyAlert(0, elder1Id,
                today.minusDays(5).atTime(18, 45),
                "Elevated Blood Pressure Warning",
                "P1: David Smith (9871122334); P2: Susan Smith (9872233445)");
        alertDAO.add(pastAlert);
    }
}
