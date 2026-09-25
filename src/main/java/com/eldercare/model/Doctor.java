package com.eldercare.model;

import com.eldercare.util.ValidationUtil;

/**
 * Represents a Doctor in the Elder Care Assistance System.
 * Doctors can be scheduled for appointments and contacted in case of emergency.
 */
public class Doctor {

    private int doctorId;
    private String name;
    private String specialization;
    private String hospital;
    private String phone;

    public Doctor() {
    }

    public Doctor(int doctorId, String name, String specialization, String hospital, String phone) {
        this.doctorId = doctorId;
        setName(name);
        setSpecialization(specialization);
        setHospital(hospital);
        setPhone(phone);
    }

    public int getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(int doctorId) {
        this.doctorId = doctorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = ValidationUtil.requireNonBlank(name, "Doctor Name");
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = ValidationUtil.requireNonBlank(specialization, "Specialization");
    }

    public String getHospital() {
        return hospital;
    }

    public void setHospital(String hospital) {
        this.hospital = ValidationUtil.requireNonBlank(hospital, "Hospital");
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = ValidationUtil.validatePhone(phone);
    }

    @Override
    public String toString() {
        return String.format("Dr. %s (%s) | Hospital: %s | Phone: %s | ID: %d",
                name, specialization, hospital, phone, doctorId);
    }
}
