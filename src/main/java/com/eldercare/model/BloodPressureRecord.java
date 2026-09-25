package com.eldercare.model;

import com.eldercare.util.ValidationUtil;

import java.time.LocalDateTime;

/**
 * BloodPressureRecord tracks systolic and diastolic blood pressure readings in mmHg.
 * Extends {@link HealthRecord} and implements dynamic polymorphic evaluation based on
 * American Heart Association (AHA) clinical standards:
 * <ul>
 *   <li><b>CRITICAL:</b> Systolic &ge; 180 or Diastolic &ge; 120 (Hypertensive crisis)</li>
 *   <li><b>LOW:</b> Systolic &lt; 90 or Diastolic &lt; 60 (Hypotension)</li>
 *   <li><b>HIGH:</b> Systolic &ge; 130 or Diastolic &ge; 80 (Hypertension Stage 1/2)</li>
 *   <li><b>ELEVATED:</b> Systolic 120-129 and Diastolic &lt; 80</li>
 *   <li><b>NORMAL:</b> Systolic 90-119 and Diastolic 60-79</li>
 * </ul>
 */
public class BloodPressureRecord extends HealthRecord {

    private int systolic;
    private int diastolic;

    public BloodPressureRecord() {
        super();
    }

    public BloodPressureRecord(int recordId, int elderId, LocalDateTime recordedAt, String notes,
                               int systolic, int diastolic) {
        super(recordId, elderId, recordedAt, notes);
        setPressures(systolic, diastolic);
    }

    public int getSystolic() {
        return systolic;
    }

    public int getDiastolic() {
        return diastolic;
    }

    public void setPressures(int systolic, int diastolic) {
        ValidationUtil.validateBloodPressure(systolic, diastolic);
        this.systolic = systolic;
        this.diastolic = diastolic;
    }

    public void setSystolic(int systolic) {
        ValidationUtil.validateBloodPressure(systolic, this.diastolic > 0 ? this.diastolic : 40);
        this.systolic = systolic;
    }

    public void setDiastolic(int diastolic) {
        ValidationUtil.validateBloodPressure(this.systolic > 0 ? this.systolic : 100, diastolic);
        this.diastolic = diastolic;
    }

    @Override
    public HealthStatus evaluate() {
        // Priority 1: Hypertensive Crisis (immediate danger)
        if (systolic >= 180 || diastolic >= 120) {
            return HealthStatus.CRITICAL;
        }
        // Priority 2: Hypotension (too low)
        if (systolic < 90 || diastolic < 60) {
            return HealthStatus.LOW;
        }
        // Priority 3: Hypertension (Stage 1 or 2)
        if (systolic >= 130 || diastolic >= 80) {
            return HealthStatus.HIGH;
        }
        // Priority 4: Elevated systolic
        if (systolic >= 120 && diastolic < 80) {
            return HealthStatus.ELEVATED;
        }
        // Priority 5: Normal range
        return HealthStatus.NORMAL;
    }

    @Override
    public String getSummary() {
        return String.format("%d/%d mmHg (%s)", systolic, diastolic, evaluate().getDisplayName());
    }

    @Override
    public String getType() {
        return "Blood Pressure";
    }
}
