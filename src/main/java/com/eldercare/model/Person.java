package com.eldercare.model;

import com.eldercare.util.ValidationUtil;

/**
 * Abstract base class representing a generic Person in the Elder Care Assistance System.
 * Demonstrates the OOP principles of Abstraction and Encapsulation.
 * Subclasses include {@link Elder} and {@link Caregiver}.
 */
public abstract class Person {

    private int id;
    private String name;
    private String phone;
    private String address;

    /**
     * Default constructor for frameworks or builder usage.
     */
    public Person() {
    }

    /**
     * Parameterized constructor validating common person attributes.
     *
     * @param id unique identifier (or 0 if not yet persisted)
     * @param name full name
     * @param phone 10-digit contact number
     * @param address physical address
     */
    public Person(int id, String name, String phone, String address) {
        this.id = id;
        setName(name);
        setPhone(phone);
        setAddress(address);
    }

    /**
     * Abstract method demonstrating polymorphism.
     * Each subclass returns its specific role in the system.
     *
     * @return role description string (e.g. "Elder", "Caregiver")
     */
    public abstract String getRole();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = ValidationUtil.requireNonBlank(name, "Name");
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = ValidationUtil.validatePhone(phone);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = (address != null) ? address.trim() : "";
    }

    @Override
    public String toString() {
        return String.format("[%s] ID: %d | Name: %s | Phone: %s | Address: %s",
                getRole(), id, name, phone, address);
    }
}
