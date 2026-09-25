package com.eldercare.service;

import com.eldercare.dao.AppointmentDAO;
import com.eldercare.dao.DoctorDAO;
import com.eldercare.dao.ElderDAO;
import com.eldercare.dao.EmergencyAlertDAO;
import com.eldercare.dao.HealthRecordDAO;
import com.eldercare.dao.MedicationDAO;
import com.eldercare.dao.MedicationLogDAO;
import com.eldercare.model.Appointment;
import com.eldercare.model.Doctor;
import com.eldercare.model.Elder;
import com.eldercare.model.EmergencyAlert;
import com.eldercare.model.HealthRecord;
import com.eldercare.model.Medication;
import com.eldercare.model.MedicationLog;
import com.eldercare.util.DateUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates neatly formatted ASCII text table reports for:
 * <ul>
 *   <li>Health Summary (including 7/30-day averages, trends, and abnormal logs)</li>
 *   <li>Medication Adherence (compliance percentages and dose logs)</li>
 *   <li>Appointment History</li>
 *   <li>Emergency Alert Log</li>
 * </ul>
 */
public class ReportGenerator {

    private final ElderDAO elderDAO;
    private final HealthRecordDAO healthRecordDAO;
    private final MedicationDAO medicationDAO;
    private final MedicationLogDAO medicationLogDAO;
    private final AppointmentDAO appointmentDAO;
    private final DoctorDAO doctorDAO;
    private final EmergencyAlertDAO alertDAO;
    private final HealthMonitor healthMonitor;
    private final ReminderService reminderService;

    public ReportGenerator() {
        this.elderDAO = new ElderDAO();
        this.healthRecordDAO = new HealthRecordDAO();
        this.medicationDAO = new MedicationDAO();
        this.medicationLogDAO = new MedicationLogDAO();
        this.appointmentDAO = new AppointmentDAO();
        this.doctorDAO = new DoctorDAO();
        this.alertDAO = new EmergencyAlertDAO();
        this.healthMonitor = new HealthMonitor(this.healthRecordDAO, new EmergencyService());
        this.reminderService = new ReminderService(this.medicationDAO, this.medicationLogDAO);
    }

    public ReportGenerator(ElderDAO elderDAO, HealthRecordDAO healthRecordDAO,
                           MedicationDAO medicationDAO, MedicationLogDAO medicationLogDAO,
                           AppointmentDAO appointmentDAO, DoctorDAO doctorDAO,
                           EmergencyAlertDAO alertDAO, HealthMonitor healthMonitor,
                           ReminderService reminderService) {
        this.elderDAO = elderDAO;
        this.healthRecordDAO = healthRecordDAO;
        this.medicationDAO = medicationDAO;
        this.medicationLogDAO = medicationLogDAO;
        this.appointmentDAO = appointmentDAO;
        this.doctorDAO = doctorDAO;
        this.alertDAO = alertDAO;
        this.healthMonitor = healthMonitor;
        this.reminderService = reminderService;
    }

    /**
     * Generates a comprehensive health summary report with statistical averages and trajectory trends.
     *
     * @param elderId elder ID
     * @return formatted report string
     * @throws SQLException on database error
     */
    public String generateHealthSummaryReport(int elderId) throws SQLException {
        Elder elder = getElderOrThrow(elderId);
        StringBuilder sb = new StringBuilder();

        sb.append(createReportHeader("HEALTH DIAGNOSTIC & MONITORING REPORT", elder));

        sb.append("\n--- MOVING AVERAGES & CLINICAL TRAJECTORY TRENDS ---\n");
        appendAverageAndTrend(sb, "Blood Pressure", HealthRecordDAO.TYPE_BP, elderId);
        appendAverageAndTrend(sb, "Blood Sugar", HealthRecordDAO.TYPE_SUGAR, elderId);
        appendAverageAndTrend(sb, "Heart Rate", HealthRecordDAO.TYPE_HEART, elderId);

        sb.append("\n--- RECENT HEALTH READINGS ---\n");
        List<HealthRecord> readings = healthRecordDAO.getByElderId(elderId);
        if (readings.isEmpty()) {
            sb.append("No health readings recorded.\n");
        } else {
            sb.append("+-----+------------------+----------------+--------------------+------------+----------------------+\n");
            sb.append("| ID  | Date & Time      | Type           | Measurement        | Status     | Notes                |\n");
            sb.append("+-----+------------------+----------------+--------------------+------------+----------------------+\n");
            for (HealthRecord r : readings) {
                sb.append(String.format("| %-3d | %-16s | %-14s | %-18s | %-10s | %-20s |\n",
                        r.getRecordId(),
                        DateUtil.formatDateTime(r.getRecordedAt()),
                        r.getType(),
                        r.getSummary(),
                        r.evaluate().name(),
                        truncate(r.getNotes(), 20)));
            }
            sb.append("+-----+------------------+----------------+--------------------+------------+----------------------+\n");
        }

        List<HealthRecord> abnormal = healthMonitor.getAbnormalReadings(elderId);
        sb.append(String.format("\n--- ABNORMAL READINGS FLAGGED (%d found) ---\n", abnormal.size()));
        if (abnormal.isEmpty()) {
            sb.append("None! All recorded readings are within normal parameters.\n");
        } else {
            for (HealthRecord r : abnormal) {
                sb.append(String.format("  [!] %s: %s | %s | Status: %s\n",
                        DateUtil.formatDateTime(r.getRecordedAt()), r.getType(), r.getSummary(), r.evaluate()));
            }
        }

        sb.append("\n=========================================================================================\n");
        return sb.toString();
    }

    /**
     * Generates a medication adherence report showing compliance percentages and dose logs.
     *
     * @param elderId elder ID
     * @return formatted report string
     * @throws SQLException on database error
     */
    public String generateMedicationAdherenceReport(int elderId) throws SQLException {
        Elder elder = getElderOrThrow(elderId);
        StringBuilder sb = new StringBuilder();

        sb.append(createReportHeader("MEDICATION ADHERENCE & COMPLIANCE REPORT", elder));

        double overallAdherence = reminderService.calculateElderOverallAdherence(elderId);
        sb.append(String.format("\nOVERALL ADHERENCE RATE: %.1f%%\n\n", overallAdherence));

        sb.append("--- ACTIVE MEDICATIONS & INDIVIDUAL ADHERENCE ---\n");
        Map<Medication, Double> breakdown = reminderService.getMedicationAdherenceBreakdown(elderId);
        if (breakdown.isEmpty()) {
            sb.append("No medications prescribed.\n");
        } else {
            sb.append("+-----+----------------------+------------+------------+--------------------+------------+\n");
            sb.append("| ID  | Medication Name      | Dosage     | Times/Day  | Active Period      | Adherence  |\n");
            sb.append("+-----+----------------------+------------+------------+--------------------+------------+\n");
            for (Map.Entry<Medication, Double> entry : breakdown.entrySet()) {
                Medication m = entry.getKey();
                String period = DateUtil.formatDate(m.getStartDate()) + " to " + DateUtil.formatDate(m.getEndDate());
                sb.append(String.format("| %-3d | %-20s | %-10s | %-10d | %-18s | %6.1f%%    |\n",
                        m.getMedicationId(),
                        truncate(m.getName(), 20),
                        truncate(m.getDosage(), 10),
                        m.getTimesPerDay(),
                        period,
                        entry.getValue()));
            }
            sb.append("+-----+----------------------+------------+------------+--------------------+------------+\n");
        }

        sb.append("\n--- RECENT DOSE LOGS ---\n");
        List<MedicationLog> logs = medicationLogDAO.getByElderId(elderId);
        if (logs.isEmpty()) {
            sb.append("No dose logs recorded.\n");
        } else {
            sb.append("+-----+------------------+------------------+------------+------------------+\n");
            sb.append("| ID  | Scheduled Time   | Medication ID    | Status     | Logged At        |\n");
            sb.append("+-----+------------------+------------------+------------+------------------+\n");
            for (MedicationLog l : logs) {
                sb.append(String.format("| %-3d | %-16s | %-16d | %-10s | %-16s |\n",
                        l.getLogId(),
                        DateUtil.formatDateTime(l.getScheduledTime()),
                        l.getMedicationId(),
                        l.getStatus().name(),
                        DateUtil.formatDateTime(l.getLoggedAt())));
            }
            sb.append("+-----+------------------+------------------+------------+------------------+\n");
        }

        sb.append("\n=========================================================================================\n");
        return sb.toString();
    }

    /**
     * Generates an appointment history report listing upcoming, completed, and cancelled visits.
     *
     * @param elderId elder ID
     * @return formatted report string
     * @throws SQLException on database error
     */
    public String generateAppointmentHistoryReport(int elderId) throws SQLException {
        Elder elder = getElderOrThrow(elderId);
        StringBuilder sb = new StringBuilder();

        sb.append(createReportHeader("DOCTOR APPOINTMENT HISTORY REPORT", elder));

        List<Appointment> appts = appointmentDAO.getByElderId(elderId);
        if (appts.isEmpty()) {
            sb.append("No appointments recorded for this elder.\n");
        } else {
            sb.append("+-----+------------------+----------------------+----------------------+------------+----------------------+\n");
            sb.append("| ID  | Date & Time      | Doctor               | Hospital             | Status     | Purpose              |\n");
            sb.append("+-----+------------------+----------------------+----------------------+------------+----------------------+\n");
            for (Appointment a : appts) {
                String docName = "Unknown";
                String docHospital = "N/A";
                Optional<Doctor> d = doctorDAO.getById(a.getDoctorId());
                if (d.isPresent()) {
                    docName = "Dr. " + d.get().getName();
                    docHospital = d.get().getHospital();
                }

                sb.append(String.format("| %-3d | %-16s | %-20s | %-20s | %-10s | %-20s |\n",
                        a.getAppointmentId(),
                        DateUtil.formatDateTime(a.getDateTime()),
                        truncate(docName, 20),
                        truncate(docHospital, 20),
                        a.getStatus().name(),
                        truncate(a.getPurpose(), 20)));
            }
            sb.append("+-----+------------------+----------------------+----------------------+------------+----------------------+\n");
        }

        sb.append("\n=========================================================================================\n");
        return sb.toString();
    }

    /**
     * Generates an emergency alert log report.
     *
     * @param elderId elder ID
     * @return formatted report string
     * @throws SQLException on database error
     */
    public String generateEmergencyAlertLogReport(int elderId) throws SQLException {
        Elder elder = getElderOrThrow(elderId);
        StringBuilder sb = new StringBuilder();

        sb.append(createReportHeader("EMERGENCY ALERT & DISPATCH LOG", elder));

        List<EmergencyAlert> alerts = alertDAO.getByElderId(elderId);
        if (alerts.isEmpty()) {
            sb.append("No emergency alerts recorded for this elder.\n");
        } else {
            sb.append("+-----+------------------+--------------------------------+-------------------------------------+\n");
            sb.append("| ID  | Date & Time      | Reason                         | Contacts Notified                   |\n");
            sb.append("+-----+------------------+--------------------------------+-------------------------------------+\n");
            for (EmergencyAlert a : alerts) {
                sb.append(String.format("| %-3d | %-16s | %-30s | %-35s |\n",
                        a.getAlertId(),
                        DateUtil.formatDateTime(a.getTimestamp()),
                        truncate(a.getReason(), 30),
                        truncate(a.getContactsNotified(), 35)));
            }
            sb.append("+-----+------------------+--------------------------------+-------------------------------------+\n");
        }

        sb.append("\n=========================================================================================\n");
        return sb.toString();
    }

    /**
     * Combines all 4 diagnostic reports into one unified master document.
     *
     * @param elderId elder ID
     * @return complete formatted elder dossier
     * @throws SQLException on database error
     */
    public String generateFullElderReport(int elderId) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("#########################################################################################\n");
        sb.append("                     ELDER CARE SYSTEM: COMPREHENSIVE DOSSIER                            \n");
        sb.append("#########################################################################################\n\n");

        sb.append(generateHealthSummaryReport(elderId)).append("\n\n");
        sb.append(generateMedicationAdherenceReport(elderId)).append("\n\n");
        sb.append(generateAppointmentHistoryReport(elderId)).append("\n\n");
        sb.append(generateEmergencyAlertLogReport(elderId));

        return sb.toString();
    }

    private void appendAverageAndTrend(StringBuilder sb, String title, String recordType, int elderId) throws SQLException {
        HealthAverage avg7 = healthMonitor.get7DayAverage(elderId, recordType);
        HealthAverage avg30 = healthMonitor.get30DayAverage(elderId, recordType);
        Trend trend = healthMonitor.detectTrend(elderId, recordType);

        sb.append(String.format("* %-16s: 7-Day: %-25s | 30-Day: %-25s | Trend: %s\n",
                title,
                avg7.hasData() ? avg7.toString() : "No data",
                avg30.hasData() ? avg30.toString() : "No data",
                trend.getDescription()));
    }

    private String createReportHeader(String title, Elder elder) {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================================================================\n");
        sb.append(String.format(" %s \n", centerText(title, 87)));
        sb.append("=========================================================================================\n");
        sb.append(String.format(" Elder: %s (ID: %d) | Age: %d | Gender: %s | Blood: %s\n",
                elder.getName(), elder.getId(), elder.getAge(), elder.getGender(), elder.getBloodGroup()));
        sb.append(String.format(" Contact: %s | Address: %s\n", elder.getPhone(), elder.getAddress()));
        sb.append(String.format(" Conditions: %s\n", elder.getMedicalConditions()));
        sb.append("=========================================================================================\n");
        return sb.toString();
    }

    private Elder getElderOrThrow(int elderId) throws SQLException {
        return elderDAO.getById(elderId)
                .orElseThrow(() -> new IllegalArgumentException("Elder with ID " + elderId + " not found."));
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private String centerText(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        int leftPadding = (width - text.length()) / 2;
        int rightPadding = width - text.length() - leftPadding;
        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }
}
