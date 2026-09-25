package com.eldercare.model;

import com.eldercare.util.ValidationUtil;

import java.time.LocalDateTime;

/**
 * HeartRateRecord tracks resting heart rate in beats per minute (bpm).
 * Extends {@link HealthRecord} and evaluates readings using standard cardiology guidelines:
 * <ul>
 *   <li><b>CRITICAL:</b> &lt; 40 bpm (severe bradycardia) or &ge; 140 bpm (severe tachycardia)</li>
 *   <li><b>LOW:</b> 40-59 bpm (bradycardia)</li>
 *   <li><b>NORMAL:</b> 60-100 bpm (normal resting adult pulse)</li>
 *   <li><b>ELEVATED:</b> 101-120 bpm (mild tachycardia)</li>
 *   <li><b>HIGH:</b> 121-139 bpm (tachycardia)</li>
 * </ul>
 */
public class HeartRateRecord extends HealthRecord {

    private int bpm;

    public HeartRateRecord() {
        super();
    }

    public HeartRateRecord(int recordId, int elderId, LocalDateTime recordedAt, String notes, int bpm) {
        super(recordId, elderId, recordedAt, notes);
        setBpm(bpm);
    }

    public int getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = ValidationUtil.validateHeartRate(bpm);
    }

    @Override
    public HealthStatus evaluate() {
        if (bpm < 40 || bpm >= 140) {
            return HealthStatus.CRITICAL;
        } else if (bpm < 60) {
            return HealthStatus.LOW;
        } else if (bpm <= 100) {
            return HealthStatus.NORMAL;
        } else if (bpm <= 120) {
            return HealthStatus.ELEVATED;
        } else {
            return HealthStatus.HIGH;
        }
    }

    @Override
    public String getSummary() {
        return String.format("%d bpm (%s)", bpm, evaluate().getDisplayName());
    }

    @Override
    public String getType() {
        return "Heart Rate";
    }
}
