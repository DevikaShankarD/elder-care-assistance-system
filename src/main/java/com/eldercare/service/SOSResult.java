package com.eldercare.service;

import com.eldercare.model.Doctor;
import com.eldercare.model.Elder;
import com.eldercare.model.EmergencyContact;
import com.eldercare.util.DateUtil;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result object containing complete contextual dispatch information when an SOS alert is triggered.
 */
public class SOSResult {

    private final int alertId;
    private final Elder elder;
    private final LocalDateTime timestamp;
    private final String reason;
    private final List<EmergencyContact> contactsNotified;
    private final Doctor assignedDoctor;

    public SOSResult(int alertId, Elder elder, LocalDateTime timestamp, String reason,
                     List<EmergencyContact> contactsNotified, Doctor assignedDoctor) {
        this.alertId = alertId;
        this.elder = elder;
        this.timestamp = timestamp;
        this.reason = reason;
        this.contactsNotified = contactsNotified;
        this.assignedDoctor = assignedDoctor;
    }

    public int getAlertId() {
        return alertId;
    }

    public Elder getElder() {
        return elder;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getReason() {
        return reason;
    }

    public List<EmergencyContact> getContactsNotified() {
        return contactsNotified;
    }

    public Doctor getAssignedDoctor() {
        return assignedDoctor;
    }

    /**
     * Formats the SOS alert into a high-visibility text banner for responders.
     */
    public String toFormattedAlertBanner() {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("                         *** EMERGENCY SOS TRIGGERED ***                        \n");
        sb.append("================================================================================\n");
        sb.append(String.format("Alert ID    : #%d\n", alertId));
        sb.append(String.format("Time        : %s\n", DateUtil.formatDateTime(timestamp)));
        sb.append(String.format("Elder       : %s (Age: %d, Gender: %s)\n", elder.getName(), elder.getAge(), elder.getGender()));
        sb.append(String.format("Phone       : %s\n", elder.getPhone()));
        sb.append(String.format("Address     : %s\n", elder.getAddress()));
        sb.append(String.format("Blood Group : %s\n", elder.getBloodGroup()));
        sb.append(String.format("Conditions  : %s\n", elder.getMedicalConditions()));
        sb.append(String.format("Reason      : %s\n", reason));
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("NOTIFIED CONTACTS (ORDERED BY PRIORITY):\n");
        if (contactsNotified.isEmpty()) {
            sb.append("  [!] No emergency contacts registered for this elder!\n");
        } else {
            for (EmergencyContact c : contactsNotified) {
                sb.append(String.format("  Priority %d: %s (%s) - Phone: %s\n",
                        c.getPriority(), c.getName(), c.getRelationship(), c.getPhone()));
            }
        }
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("PRIMARY / ATTENDING DOCTOR:\n");
        if (assignedDoctor != null) {
            sb.append(String.format("  Dr. %s (%s) | Hospital: %s | Phone: %s\n",
                    assignedDoctor.getName(), assignedDoctor.getSpecialization(),
                    assignedDoctor.getHospital(), assignedDoctor.getPhone()));
        } else {
            sb.append("  No specific attending doctor assigned.\n");
        }
        sb.append("================================================================================\n");
        return sb.toString();
    }
}
