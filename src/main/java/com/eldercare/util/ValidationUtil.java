package com.eldercare.util;

import java.util.regex.Pattern;

/**
 * ValidationUtil provides reusable input validation logic across models and UI input.
 * Throws IllegalArgumentException on invalid values to maintain strict domain integrity.
 */
public final class ValidationUtil {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10}$");

    private ValidationUtil() {
        // Utility class; prevent instantiation
    }

    /**
     * Validates that a string is neither null nor empty/whitespace.
     *
     * @param value field value
     * @param fieldName name of the field for error message
     * @return trimmed valid string
     */
    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty.");
        }
        return value.trim();
    }

    /**
     * Validates that a phone number consists of exactly 10 digits.
     *
     * @param phone phone number string
     * @return validated 10-digit phone string
     */
    public static String validatePhone(String phone) {
        if (phone == null || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            throw new IllegalArgumentException("Phone number must be exactly 10 digits (e.g. 9876543210).");
        }
        return phone.trim();
    }

    /**
     * Validates human age between 1 and 130 years.
     *
     * @param age age in years
     * @return validated age
     */
    public static int validateAge(int age) {
        if (age < 1 || age > 130) {
            throw new IllegalArgumentException("Age must be between 1 and 130.");
        }
        return age;
    }

    /**
     * Validates contact priority (1 = call first, 2 = second, etc.).
     *
     * @param priority priority integer
     * @return validated priority
     */
    public static int validatePriority(int priority) {
        if (priority < 1) {
            throw new IllegalArgumentException("Priority must be 1 or greater.");
        }
        return priority;
    }

    /**
     * Validates blood pressure readings within realistic clinical limits.
     *
     * @param systolic systolic blood pressure (mmHg)
     * @param diastolic diastolic blood pressure (mmHg)
     */
    public static void validateBloodPressure(int systolic, int diastolic) {
        if (systolic < 50 || systolic > 260) {
            throw new IllegalArgumentException("Systolic pressure must be between 50 and 260 mmHg.");
        }
        if (diastolic < 30 || diastolic > 160) {
            throw new IllegalArgumentException("Diastolic pressure must be between 30 and 160 mmHg.");
        }
        if (systolic <= diastolic) {
            throw new IllegalArgumentException("Systolic pressure (" + systolic + ") must be higher than diastolic pressure (" + diastolic + ").");
        }
    }

    /**
     * Validates blood sugar measurement within realistic physiological limits.
     *
     * @param valueMgDl blood sugar in mg/dL
     * @return validated value
     */
    public static double validateBloodSugar(double valueMgDl) {
        if (valueMgDl < 20.0 || valueMgDl > 600.0) {
            throw new IllegalArgumentException("Blood sugar value must be between 20 and 600 mg/dL.");
        }
        return valueMgDl;
    }

    /**
     * Validates resting heart rate in bpm.
     *
     * @param bpm beats per minute
     * @return validated heart rate
     */
    public static int validateHeartRate(int bpm) {
        if (bpm < 30 || bpm > 220) {
            throw new IllegalArgumentException("Heart rate must be between 30 and 220 bpm.");
        }
        return bpm;
    }
}
