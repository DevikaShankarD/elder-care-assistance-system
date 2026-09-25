package com.eldercare.model;

import com.eldercare.util.ValidationUtil;

/**
 * Caregiver extends {@link Person} to represent family members or professional aides
 * managing an elder's care schedule and health tracking.
 * Demonstrates the OOP principle of Inheritance.
 */
public class Caregiver extends Person {

    private String relationshipToElder;

    public Caregiver() {
        super();
    }

    public Caregiver(int id, String name, String phone, String address, String relationshipToElder) {
        super(id, name, phone, address);
        setRelationshipToElder(relationshipToElder);
    }

    @Override
    public String getRole() {
        return "Caregiver";
    }

    public String getRelationshipToElder() {
        return relationshipToElder;
    }

    public void setRelationshipToElder(String relationshipToElder) {
        this.relationshipToElder = ValidationUtil.requireNonBlank(relationshipToElder, "Relationship to Elder");
    }

    @Override
    public String toString() {
        return String.format("Caregiver [ID: %d] %s | Relation: %s | Phone: %s | Address: %s",
                getId(), getName(), relationshipToElder, getPhone(), getAddress());
    }
}
