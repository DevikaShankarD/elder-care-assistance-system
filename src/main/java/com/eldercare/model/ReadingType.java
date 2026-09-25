package com.eldercare.model;

/**
 * ReadingType represents the context in which a blood sugar reading was taken.
 * Thresholds for evaluating blood sugar status depend on this reading type.
 */
public enum ReadingType {
    FASTING("Fasting (Before Meal)"),
    POST_MEAL("Post Meal (2 hours after eating)"),
    RANDOM("Random");

    private final String description;

    ReadingType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
