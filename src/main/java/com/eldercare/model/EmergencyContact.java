package com.eldercare.model;

import com.eldercare.util.ValidationUtil;

/**
 * Represents an Emergency Contact associated with an Elder.
 * Contacts are prioritized so emergency responders know who to reach first (priority 1 = call first).
 */
public class EmergencyContact implements Comparable<EmergencyContact> {

    private int contactId;
    private int elderId;
    private String name;
    private String relationship;
    private String phone;
    private int priority;

    public EmergencyContact() {
    }

    public EmergencyContact(int contactId, int elderId, String name, String relationship, String phone, int priority) {
        this.contactId = contactId;
        this.elderId = elderId;
        setName(name);
        setRelationship(relationship);
        setPhone(phone);
        setPriority(priority);
    }

    public int getContactId() {
        return contactId;
    }

    public void setContactId(int contactId) {
        this.contactId = contactId;
    }

    public int getElderId() {
        return elderId;
    }

    public void setElderId(int elderId) {
        this.elderId = elderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = ValidationUtil.requireNonBlank(name, "Contact Name");
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = ValidationUtil.requireNonBlank(relationship, "Relationship");
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = ValidationUtil.validatePhone(phone);
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = ValidationUtil.validatePriority(priority);
    }

    @Override
    public int compareTo(EmergencyContact other) {
        // Natural ordering by priority ascending (1 before 2)
        if (other == null) {
            return 1;
        }
        return Integer.compare(this.priority, other.priority);
    }

    @Override
    public String toString() {
        return String.format("Contact #%d [Priority %d]: %s (%s) - Phone: %s",
                contactId, priority, name, relationship, phone);
    }
}
