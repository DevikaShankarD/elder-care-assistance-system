package com.eldercare.service;

import com.eldercare.dao.HealthRecordDAO;
import com.eldercare.model.BloodPressureRecord;
import com.eldercare.model.BloodSugarRecord;
import com.eldercare.model.HealthRecord;
import com.eldercare.model.HealthStatus;
import com.eldercare.model.HeartRateRecord;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing health diagnostic measurements, polymorphic clinical evaluations,
 * statistical moving averages (7-day and 30-day), trend detection, and critical alert triggering.
 */
public class HealthMonitor {

    private final HealthRecordDAO healthRecordDAO;
    private final EmergencyService emergencyService;

    public HealthMonitor() {
        this(new HealthRecordDAO(), new EmergencyService());
    }

    public HealthMonitor(HealthRecordDAO healthRecordDAO, EmergencyService emergencyService) {
        this.healthRecordDAO = healthRecordDAO;
        this.emergencyService = emergencyService;
    }

    /**
     * Adds a health reading, evaluates its clinical status polymorphically, saves it to the database,
     * and automatically triggers an emergency alert if the reading evaluates to CRITICAL.
     *
     * @param record polymorphic HealthRecord instance
     * @return persisted HealthRecord with generated recordId
     * @throws SQLException on database error
     */
    public HealthRecord addReading(HealthRecord record) throws SQLException {
        if (record == null) {
            throw new IllegalArgumentException("Health record cannot be null.");
        }

        // Dynamic Polymorphism: evaluate() is executed based on the actual runtime object
        HealthStatus status = record.evaluate();

        int id = healthRecordDAO.add(record);
        record.setRecordId(id);

        // If the reading is CRITICAL, immediately dispatch an automatic emergency alert
        if (status == HealthStatus.CRITICAL && emergencyService != null) {
            emergencyService.triggerAutoCriticalAlert(
                    record.getElderId(),
                    record.getType() + " (" + record.getSummary() + ")"
            );
        }

        return record;
    }

    /**
     * Retrieves all health readings for an elder, sorted chronologically.
     *
     * @param elderId elder ID
     * @return list of health records
     * @throws SQLException on database error
     */
    public List<HealthRecord> getAllReadings(int elderId) throws SQLException {
        return healthRecordDAO.getByElderId(elderId);
    }

    /**
     * Retrieves all abnormal health readings (LOW, ELEVATED, HIGH, or CRITICAL) for an elder.
     *
     * @param elderId elder ID
     * @return list of abnormal records
     * @throws SQLException on database error
     */
    public List<HealthRecord> getAbnormalReadings(int elderId) throws SQLException {
        return healthRecordDAO.getByElderId(elderId).stream()
                .filter(r -> r.evaluate() != HealthStatus.NORMAL)
                .collect(Collectors.toList());
    }

    /**
     * Calculates 7-day average for a specific health measurement type.
     *
     * @param elderId elder ID
     * @param recordType HealthRecordDAO.TYPE_BP, TYPE_SUGAR, or TYPE_HEART
     * @return HealthAverage object
     * @throws SQLException on database error
     */
    public HealthAverage get7DayAverage(int elderId, String recordType) throws SQLException {
        return calculateAverage(elderId, recordType, 7, LocalDateTime.now());
    }

    /**
     * Calculates 30-day average for a specific health measurement type.
     *
     * @param elderId elder ID
     * @param recordType HealthRecordDAO.TYPE_BP, TYPE_SUGAR, or TYPE_HEART
     * @return HealthAverage object
     * @throws SQLException on database error
     */
    public HealthAverage get30DayAverage(int elderId, String recordType) throws SQLException {
        return calculateAverage(elderId, recordType, 30, LocalDateTime.now());
    }

    /**
     * Computes moving averages over a specified day window.
     *
     * @param elderId elder ID
     * @param recordType measurement type
     * @param daysWindow number of days (e.g. 7 or 30)
     * @param referenceTime anchor timestamp
     * @return HealthAverage
     * @throws SQLException on database error
     */
    public HealthAverage calculateAverage(int elderId, String recordType, int daysWindow, LocalDateTime referenceTime) throws SQLException {
        LocalDateTime windowStart = referenceTime.minusDays(daysWindow);

        List<HealthRecord> all = healthRecordDAO.getByElderIdAndType(elderId, recordType);
        List<HealthRecord> filtered = all.stream()
                .filter(r -> !r.getRecordedAt().isBefore(windowStart) && !r.getRecordedAt().isAfter(referenceTime))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return new HealthAverage(recordType, daysWindow, 0, null, null);
        }

        if (HealthRecordDAO.TYPE_BP.equalsIgnoreCase(recordType)) {
            double avgSys = filtered.stream()
                    .filter(r -> r instanceof BloodPressureRecord)
                    .mapToInt(r -> ((BloodPressureRecord) r).getSystolic())
                    .average()
                    .orElse(0.0);
            double avgDia = filtered.stream()
                    .filter(r -> r instanceof BloodPressureRecord)
                    .mapToInt(r -> ((BloodPressureRecord) r).getDiastolic())
                    .average()
                    .orElse(0.0);
            return new HealthAverage(recordType, daysWindow, filtered.size(), avgSys, avgDia);

        } else if (HealthRecordDAO.TYPE_SUGAR.equalsIgnoreCase(recordType)) {
            double avgSugar = filtered.stream()
                    .filter(r -> r instanceof BloodSugarRecord)
                    .mapToDouble(r -> ((BloodSugarRecord) r).getValueMgDl())
                    .average()
                    .orElse(0.0);
            return new HealthAverage(recordType, daysWindow, filtered.size(), avgSugar, null);

        } else if (HealthRecordDAO.TYPE_HEART.equalsIgnoreCase(recordType)) {
            double avgBpm = filtered.stream()
                    .filter(r -> r instanceof HeartRateRecord)
                    .mapToInt(r -> ((HeartRateRecord) r).getBpm())
                    .average()
                    .orElse(0.0);
            return new HealthAverage(recordType, daysWindow, filtered.size(), avgBpm, null);
        }

        return new HealthAverage(recordType, daysWindow, 0, null, null);
    }

    /**
     * Detects trajectory trend (Rising, Falling, or Stable) across historical readings.
     * Compares the average of the older half of readings against the newer half.
     *
     * @param elderId elder ID
     * @param recordType measurement type
     * @return Trend enum
     * @throws SQLException on database error
     */
    public Trend detectTrend(int elderId, String recordType) throws SQLException {
        List<HealthRecord> records = healthRecordDAO.getByElderIdAndType(elderId, recordType);
        if (records.size() < 3) {
            return Trend.INSUFFICIENT_DATA;
        }

        records.sort(Comparator.comparing(HealthRecord::getRecordedAt));

        List<Double> values = new ArrayList<>();
        for (HealthRecord r : records) {
            if (r instanceof BloodPressureRecord bp) {
                values.add((double) bp.getSystolic());
            } else if (r instanceof BloodSugarRecord bs) {
                values.add(bs.getValueMgDl());
            } else if (r instanceof HeartRateRecord hr) {
                values.add((double) hr.getBpm());
            }
        }

        int mid = values.size() / 2;
        double firstHalfAvg = values.subList(0, mid).stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double secondHalfAvg = values.subList(mid, values.size()).stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double diff = secondHalfAvg - firstHalfAvg;
        // Sensitivity threshold: 3 units
        if (diff > 3.0) {
            return Trend.RISING;
        } else if (diff < -3.0) {
            return Trend.FALLING;
        } else {
            return Trend.STABLE;
        }
    }
}
