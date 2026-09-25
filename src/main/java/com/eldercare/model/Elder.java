package com.eldercare.model;

import com.eldercare.util.ValidationUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Elder extends {@link Person} to represent an elderly individual receiving care.
 * Demonstrates the OOP principles of Inheritance and Encapsulation.
 */
public class Elder extends Person {

    private int age;
    private String gender;
    private String bloodGroup;
    private String medicalConditions;
    private List<EmergencyContact> emergencyContacts = new ArrayList<>();

    public Elder() {
        super();
    }

    public Elder(int id, String name, String phone, String address,
                 int age, String gender, String bloodGroup, String medicalConditions) {
        super(id, name, phone, address);
        setAge(age);
        setGender(gender);
        setBloodGroup(bloodGroup);
        setMedicalConditions(medicalConditions);
    }

    @Override
    public String getRole() {
        return "Elder";
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = ValidationUtil.validateAge(age);
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = (gender != null) ? gender.trim() : "Unspecified";
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = (bloodGroup != null) ? bloodGroup.trim().toUpperCase() : "Unknown";
    }

    public String getMedicalConditions() {
        return medicalConditions;
    }

    public void setMedicalConditions(String medicalConditions) {
        this.medicalConditions = (medicalConditions != null) ? medicalConditions.trim() : "None";
    }

    public List<EmergencyContact> getEmergencyContacts() {
        // Return an unmodifiable view to preserve encapsulation
        return Collections.unmodifiableList(emergencyContacts);
    }

    public void setEmergencyContacts(List<EmergencyContact> contacts) {
        this.emergencyContacts = (contacts != null) ? new ArrayList<>(contacts) : new ArrayList<>();
        Collections.sort(this.emergencyContacts);
    }

    public void addEmergencyContact(EmergencyContact contact) {
        if (contact != null) {
            this.emergencyContacts.add(contact);
            Collections.sort(this.emergencyContacts);
        }
    }

    public void removeEmergencyContact(int contactId) {
        this.emergencyContacts.removeIf(c -> c.getContactId() == contactId);
    }

    @Override
    public String toString() {
        return String.format("Elder [ID: %d] %s | Age: %d | Gender: %s | Blood: %s | Conditions: %s | Phone: %s",
                getId(), getName(), age, gender, bloodGroup, medicalConditions, getPhone());
    }
}
