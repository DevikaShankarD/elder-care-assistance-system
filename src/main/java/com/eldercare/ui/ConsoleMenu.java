package com.eldercare.ui;

import com.eldercare.dao.*;
import com.eldercare.model.*;
import com.eldercare.service.*;
import com.eldercare.util.DateUtil;
import com.eldercare.util.SampleDataLoader;
import com.eldercare.util.ValidationUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

/**
 * ConsoleMenu drives the interactive text interface for the Elder Care Assistance System.
 * Ensures strict input validation and friendly error handling so the application never crashes.
 */
public class ConsoleMenu {

    private final Scanner scanner;
    private final DatabaseManager dbManager;

    private final ElderDAO elderDAO;
    private final CaregiverDAO caregiverDAO;
    private final DoctorDAO doctorDAO;
    private final EmergencyContactDAO contactDAO;
    private final MedicationDAO medicationDAO;
    private final MedicationLogDAO medicationLogDAO;
    private final AppointmentDAO appointmentDAO;
    private final HealthRecordDAO healthRecordDAO;
    private final EmergencyAlertDAO alertDAO;

    private final ReminderService reminderService;
    private final AppointmentScheduler appointmentScheduler;
    private final EmergencyService emergencyService;
    private final HealthMonitor healthMonitor;
    private final ReportGenerator reportGenerator;

    public ConsoleMenu() {
        this(new Scanner(System.in));
    }

    public ConsoleMenu(Scanner scanner) {
        this.scanner = scanner;
        this.dbManager = DatabaseManager.getInstance();

        this.elderDAO = new ElderDAO(dbManager);
        this.caregiverDAO = new CaregiverDAO(dbManager);
        this.doctorDAO = new DoctorDAO(dbManager);
        this.contactDAO = new EmergencyContactDAO(dbManager);
        this.medicationDAO = new MedicationDAO(dbManager);
        this.medicationLogDAO = new MedicationLogDAO(dbManager);
        this.appointmentDAO = new AppointmentDAO(dbManager);
        this.healthRecordDAO = new HealthRecordDAO(dbManager);
        this.alertDAO = new EmergencyAlertDAO(dbManager);

        this.reminderService = new ReminderService(medicationDAO, medicationLogDAO);
        this.appointmentScheduler = new AppointmentScheduler(appointmentDAO, doctorDAO, elderDAO);
        this.emergencyService = new EmergencyService(alertDAO, elderDAO, contactDAO, doctorDAO, appointmentDAO);
        this.healthMonitor = new HealthMonitor(healthRecordDAO, emergencyService);
        this.reportGenerator = new ReportGenerator(elderDAO, healthRecordDAO, medicationDAO,
                medicationLogDAO, appointmentDAO, doctorDAO, alertDAO, healthMonitor, reminderService);
    }

    /**
     * Starts the main menu loop.
     */
    public void start() {
        printWelcomeBanner();
        checkAndOfferSampleData();

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Enter your choice: ", 0, 9);
            System.out.println();

            try {
                switch (choice) {
                    case 1 -> elderManagementSubmenu();
                    case 2 -> medicationRemindersSubmenu();
                    case 3 -> appointmentSchedulingSubmenu();
                    case 4 -> healthTrackingSubmenu();
                    case 5 -> reportsSubmenu();
                    case 6 -> viewEmergencyAlerts();
                    case 9 -> triggerManualSOS();
                    case 0 -> {
                        System.out.println("Thank you for using Elder Care Assistance System. Goodbye!");
                        running = false;
                    }
                    default -> System.out.println("[!] Invalid option. Please choose a valid menu number.");
                }
            } catch (Exception e) {
                System.out.println("[ERROR] An unexpected error occurred: " + e.getMessage());
            }
            if (running) {
                System.out.println();
            }
        }
    }

    private void printWelcomeBanner() {
        System.out.println("================================================================================");
        System.out.println("               ELDER CARE ASSISTANCE SYSTEM - CONSOLE PLATFORM                  ");
        System.out.println("          Empowering Independent Living with Health, Safety & Care              ");
        System.out.println("================================================================================");
    }

    private void checkAndOfferSampleData() {
        if (dbManager.isDatabaseEmpty()) {
            System.out.println("\n[!] The database is currently empty (first run detected).");
            boolean load = readYesNo("Would you like to seed demonstration sample data (2 elders, 3 doctors, meds, readings)? (Y/N): ");
            if (load) {
                try {
                    new SampleDataLoader(dbManager).loadSampleData();
                    System.out.println("[SUCCESS] Sample data loaded successfully! You can now test all features immediately.\n");
                } catch (SQLException e) {
                    System.out.println("[ERROR] Failed to seed sample data: " + e.getMessage());
                }
            }
        }
    }

    private void printMainMenu() {
        System.out.println("================================================================================");
        System.out.println("                                MAIN MENU                                       ");
        System.out.println("================================================================================");
        System.out.println(" [1] Elder & Contact Management");
        System.out.println(" [2] Medication Reminders & Adherence");
        System.out.println(" [3] Doctor Appointments & Scheduling");
        System.out.println(" [4] Health Log Tracking (BP, Sugar, Heart Rate)");
        System.out.println(" [5] Reports & Dossiers");
        System.out.println(" [6] Emergency Alert History");
        System.out.println(" [9] *** ONE-TOUCH EMERGENCY SOS ***");
        System.out.println(" [0] Exit Application");
        System.out.println("================================================================================");
    }

    // =========================================================================
    // SUBMENU 1: Elder & Contact Management
    // =========================================================================
    private void elderManagementSubmenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- ELDER & CONTACT MANAGEMENT ---");
            System.out.println(" [1] Register New Elder");
            System.out.println(" [2] View All Elders");
            System.out.println(" [3] Search Elder by Name or ID");
            System.out.println(" [4] Update Elder Information");
            System.out.println(" [5] Delete Elder");
            System.out.println(" [6] Manage Emergency Contacts (Add, Edit, Remove, Prioritize)");
            System.out.println(" [7] Register Caregiver");
            System.out.println(" [8] Register Doctor");
            System.out.println(" [9] View All Caregivers & Doctors");
            System.out.println(" [0] Back to Main Menu");

            int choice = readInt("Select an option: ", 0, 9);
            System.out.println();
            try {
                switch (choice) {
                    case 1 -> registerElder();
                    case 2 -> viewAllElders();
                    case 3 -> searchElder();
                    case 4 -> updateElder();
                    case 5 -> deleteElder();
                    case 6 -> manageEmergencyContacts();
                    case 7 -> registerCaregiver();
                    case 8 -> registerDoctor();
                    case 9 -> viewAllCaregiversAndDoctors();
                    case 0 -> back = true;
                    default -> System.out.println("[!] Invalid option.");
                }
            } catch (SQLException e) {
                System.out.println("[DATABASE ERROR] " + e.getMessage());
            }
        }
    }

    private void registerElder() throws SQLException {
        System.out.println("--- Register New Elder ---");
        String name = readNonBlank("Enter full name: ");
        String phone = readPhone("Enter 10-digit phone number: ");
        String address = readString("Enter physical address: ");
        int age = readInt("Enter age (1-130): ", 1, 130);
        String gender = readString("Enter gender (Male/Female/Other): ");
        String blood = readString("Enter blood group (e.g. O+, A+, B-, AB+): ");
        String conditions = readString("Enter known medical conditions (e.g. Diabetes, Hypertension): ");

        Elder elder = new Elder(0, name, phone, address, age, gender, blood, conditions);
        int id = elderDAO.add(elder);
        System.out.printf("[SUCCESS] Elder '%s' registered with ID: %d\n", name, id);
    }

    private void viewAllElders() throws SQLException {
        List<Elder> elders = elderDAO.getAll();
        if (elders.isEmpty()) {
            System.out.println("No elders registered yet.");
            return;
        }
        System.out.println("+-----+--------------------------------+-----+--------+-------+------------+---------------------------+");
        System.out.println("| ID  | Name                           | Age | Gender | Blood | Phone      | Medical Conditions        |");
        System.out.println("+-----+--------------------------------+-----+--------+-------+------------+---------------------------+");
        for (Elder e : elders) {
            System.out.printf("| %-3d | %-30s | %-3d | %-6s | %-5s | %-10s | %-25s |\n",
                    e.getId(), truncate(e.getName(), 30), e.getAge(),
                    truncate(e.getGender(), 6), e.getBloodGroup(), e.getPhone(),
                    truncate(e.getMedicalConditions(), 25));
        }
        System.out.println("+-----+--------------------------------+-----+--------+-------+------------+---------------------------+");
    }

    private void searchElder() throws SQLException {
        System.out.println(" [1] Search by Elder ID");
        System.out.println(" [2] Search by Name");
        int method = readInt("Choose search type: ", 1, 2);
        if (method == 1) {
            int id = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
            Optional<Elder> opt = elderDAO.getById(id);
            if (opt.isPresent()) {
                printElderCard(opt.get());
            } else {
                System.out.println("[!] No elder found with ID: " + id);
            }
        } else {
            String name = readNonBlank("Enter name or partial keyword: ");
            List<Elder> list = elderDAO.searchByName(name);
            if (list.isEmpty()) {
                System.out.println("[!] No elders found matching '" + name + "'");
            } else {
                for (Elder e : list) {
                    printElderCard(e);
                }
            }
        }
    }

    private void printElderCard(Elder e) throws SQLException {
        System.out.println("\n------------------------------------------------------------");
        System.out.printf("Elder ID   : %d\n", e.getId());
        System.out.printf("Name       : %s (Role: %s)\n", e.getName(), e.getRole());
        System.out.printf("Age/Gender : %d years | %s\n", e.getAge(), e.getGender());
        System.out.printf("Blood Group: %s\n", e.getBloodGroup());
        System.out.printf("Phone      : %s\n", e.getPhone());
        System.out.printf("Address    : %s\n", e.getAddress());
        System.out.printf("Conditions : %s\n", e.getMedicalConditions());

        List<EmergencyContact> contacts = contactDAO.getByElderId(e.getId());
        System.out.println("Emergency Contacts:");
        if (contacts.isEmpty()) {
            System.out.println("  (None registered)");
        } else {
            for (EmergencyContact c : contacts) {
                System.out.printf("  [Priority %d] %s (%s) - Phone: %s\n",
                        c.getPriority(), c.getName(), c.getRelationship(), c.getPhone());
            }
        }
        System.out.println("------------------------------------------------------------");
    }

    private void updateElder() throws SQLException {
        int id = readInt("Enter Elder ID to update: ", 1, Integer.MAX_VALUE);
        Optional<Elder> opt = elderDAO.getById(id);
        if (opt.isEmpty()) {
            System.out.println("[!] Elder not found with ID: " + id);
            return;
        }
        Elder elder = opt.get();
        System.out.printf("Updating Elder: %s (Press Enter to keep current value)\n", elder.getName());

        String name = readOptionalString("Name [" + elder.getName() + "]: ", elder.getName());
        String phone = readOptionalPhone("Phone [" + elder.getPhone() + "]: ", elder.getPhone());
        String address = readOptionalString("Address [" + elder.getAddress() + "]: ", elder.getAddress());
        int age = readOptionalInt("Age [" + elder.getAge() + "]: ", elder.getAge(), 1, 130);
        String gender = readOptionalString("Gender [" + elder.getGender() + "]: ", elder.getGender());
        String blood = readOptionalString("Blood Group [" + elder.getBloodGroup() + "]: ", elder.getBloodGroup());
        String conditions = readOptionalString("Conditions [" + elder.getMedicalConditions() + "]: ", elder.getMedicalConditions());

        elder.setName(name);
        elder.setPhone(phone);
        elder.setAddress(address);
        elder.setAge(age);
        elder.setGender(gender);
        elder.setBloodGroup(blood);
        elder.setMedicalConditions(conditions);

        if (elderDAO.update(elder)) {
            System.out.println("[SUCCESS] Elder details updated successfully.");
        } else {
            System.out.println("[!] Failed to update elder.");
        }
    }

    private void deleteElder() throws SQLException {
        int id = readInt("Enter Elder ID to delete: ", 1, Integer.MAX_VALUE);
        Optional<Elder> opt = elderDAO.getById(id);
        if (opt.isEmpty()) {
            System.out.println("[!] Elder not found with ID: " + id);
            return;
        }
        boolean confirm = readYesNo("Are you sure you want to delete Elder '" + opt.get().getName() +
                "' and all associated data? (Y/N): ");
        if (confirm) {
            if (elderDAO.delete(id)) {
                System.out.println("[SUCCESS] Elder and all related records deleted.");
            } else {
                System.out.println("[!] Failed to delete elder.");
            }
        }
    }

    private void manageEmergencyContacts() throws SQLException {
        int elderId = readInt("Enter Elder ID to manage contacts for: ", 1, Integer.MAX_VALUE);
        if (elderDAO.getById(elderId).isEmpty()) {
            System.out.println("[!] Elder not found.");
            return;
        }

        System.out.println("\n--- Emergency Contacts for Elder #" + elderId + " ---");
        List<EmergencyContact> contacts = contactDAO.getByElderId(elderId);
        for (EmergencyContact c : contacts) {
            System.out.printf("  [Contact ID: %d] Priority %d: %s (%s) - Phone: %s\n",
                    c.getContactId(), c.getPriority(), c.getName(), c.getRelationship(), c.getPhone());
        }

        System.out.println("\n [1] Add New Contact");
        System.out.println(" [2] Edit Contact Priority");
        System.out.println(" [3] Remove Contact");
        System.out.println(" [0] Cancel");
        int sub = readInt("Choose action: ", 0, 3);

        if (sub == 1) {
            String name = readNonBlank("Contact full name: ");
            String rel = readNonBlank("Relationship (e.g. Son, Daughter, Physician): ");
            String phone = readPhone("10-digit contact phone: ");
            int priority = readInt("Priority (1 = call first, 2 = second, etc.): ", 1, 99);

            EmergencyContact contact = new EmergencyContact(0, elderId, name, rel, phone, priority);
            int cid = contactDAO.add(contact);
            System.out.printf("[SUCCESS] Contact added with ID: %d\n", cid);
        } else if (sub == 2) {
            int cid = readInt("Enter Contact ID to edit priority: ", 1, Integer.MAX_VALUE);
            Optional<EmergencyContact> cOpt = contactDAO.getById(cid);
            if (cOpt.isPresent() && cOpt.get().getElderId() == elderId) {
                int newPri = readInt("Enter new priority (1 = call first): ", 1, 99);
                EmergencyContact c = cOpt.get();
                c.setPriority(newPri);
                contactDAO.update(c);
                System.out.println("[SUCCESS] Contact priority updated.");
            } else {
                System.out.println("[!] Contact not found for this elder.");
            }
        } else if (sub == 3) {
            int cid = readInt("Enter Contact ID to remove: ", 1, Integer.MAX_VALUE);
            if (contactDAO.delete(cid)) {
                System.out.println("[SUCCESS] Contact removed.");
            } else {
                System.out.println("[!] Contact not found.");
            }
        }
    }

    private void registerCaregiver() throws SQLException {
        System.out.println("--- Register Caregiver ---");
        String name = readNonBlank("Caregiver name: ");
        String phone = readPhone("10-digit phone number: ");
        String address = readString("Address: ");
        String rel = readNonBlank("Relationship to Elder (e.g. Professional Aide, Daughter): ");

        Caregiver cg = new Caregiver(0, name, phone, address, rel);
        int id = caregiverDAO.add(cg);
        System.out.printf("[SUCCESS] Caregiver registered with ID: %d (Role: %s)\n", id, cg.getRole());
    }

    private void registerDoctor() throws SQLException {
        System.out.println("--- Register Doctor ---");
        String name = readNonBlank("Doctor name: ");
        String spec = readNonBlank("Specialization (e.g. Cardiology, Geriatrics): ");
        String hospital = readNonBlank("Hospital / Clinic: ");
        String phone = readPhone("10-digit phone number: ");

        Doctor doc = new Doctor(0, name, spec, hospital, phone);
        int id = doctorDAO.add(doc);
        System.out.printf("[SUCCESS] Doctor registered with ID: %d\n", id);
    }

    private void viewAllCaregiversAndDoctors() throws SQLException {
        System.out.println("\n--- REGISTERED CAREGIVERS ---");
        List<Caregiver> caregivers = caregiverDAO.getAll();
        if (caregivers.isEmpty()) {
            System.out.println("No caregivers registered.");
        } else {
            for (Caregiver c : caregivers) {
                System.out.println("  " + c);
            }
        }

        System.out.println("\n--- REGISTERED DOCTORS ---");
        List<Doctor> doctors = doctorDAO.getAll();
        if (doctors.isEmpty()) {
            System.out.println("No doctors registered.");
        } else {
            for (Doctor d : doctors) {
                System.out.println("  " + d);
            }
        }
    }

    // =========================================================================
    // SUBMENU 2: Medication Reminders & Adherence
    // =========================================================================
    private void medicationRemindersSubmenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MEDICATION REMINDERS & ADHERENCE ---");
            System.out.println(" [1] Add Prescribed Medication");
            System.out.println(" [2] View All Prescriptions for Elder");
            System.out.println(" [3] View Today's Dose Schedule");
            System.out.println(" [4] View Due / Overdue Doses");
            System.out.println(" [5] Record Dose (Taken / Missed / Skipped)");
            System.out.println(" [6] View Adherence Percentages");
            System.out.println(" [7] Remove Medication");
            System.out.println(" [0] Back to Main Menu");

            int choice = readInt("Select an option: ", 0, 7);
            System.out.println();
            try {
                switch (choice) {
                    case 1 -> addMedication();
                    case 2 -> viewPrescriptions();
                    case 3 -> viewTodaySchedule();
                    case 4 -> viewDueOrOverdueDoses();
                    case 5 -> recordDose();
                    case 6 -> viewAdherencePercentages();
                    case 7 -> removeMedication();
                    case 0 -> back = true;
                    default -> System.out.println("[!] Invalid option.");
                }
            } catch (SQLException e) {
                System.out.println("[DATABASE ERROR] " + e.getMessage());
            }
        }
    }

    private void addMedication() throws SQLException {
        int elderId = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
        if (elderDAO.getById(elderId).isEmpty()) {
            System.out.println("[!] Elder not found.");
            return;
        }

        String name = readNonBlank("Medication name: ");
        String dosage = readNonBlank("Dosage (e.g. 500 mg, 1 tablet): ");
        int times = readInt("Times per day (e.g. 1, 2, 3): ", 1, 10);

        List<LocalTime> doseTimes = new ArrayList<>();
        System.out.println("Enter scheduled dose times (HH:mm 24-hr format):");
        for (int i = 1; i <= times; i++) {
            LocalTime t = readTime("  Dose " + i + " time (HH:mm): ");
            doseTimes.add(t);
        }

        LocalDate startDate = readDate("Start date (dd-MM-yyyy): ");
        LocalDate endDate = readDate("End date (dd-MM-yyyy): ");
        String instructions = readString("Special instructions (e.g. Take after meal): ");

        try {
            Medication med = new Medication(0, elderId, name, dosage, times, doseTimes, startDate, endDate, instructions);
            int medId = medicationDAO.add(med);
            System.out.printf("[SUCCESS] Medication '%s' saved with ID: %d\n", name, medId);
        } catch (IllegalArgumentException e) {
            System.out.println("[VALIDATION ERROR] " + e.getMessage());
        }
    }

    private void viewPrescriptions() throws SQLException {
        int elderId = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
        List<Medication> meds = medicationDAO.getByElderId(elderId);
        if (meds.isEmpty()) {
            System.out.println("No medications prescribed for this elder.");
            return;
        }
        System.out.println("+-----+----------------------+------------+------------+--------------------+---------------------------+");
        System.out.println("| ID  | Medication Name      | Dosage     | Times/Day  | Active Period      | Instructions              |");
        System.out.println("+-----+----------------------+------------+------------+--------------------+---------------------------+");
        for (Medication m : meds) {
            String period = DateUtil.formatDate(m.getStartDate()) + " to " + DateUtil.formatDate(m.getEndDate());
            System.out.printf("| %-3d | %-20s | %-10s | %-10d | %-18s | %-25s |\n",
                    m.getMedicationId(), truncate(m.getName(), 20),
                    truncate(m.getDosage(), 10), m.getTimesPerDay(),
                    period, truncate(m.getInstructions(), 25));
        }
        System.out.println("+-----+----------------------+------------+------------+--------------------+---------------------------+");
    }

    private void viewTodaySchedule() throws SQLException {
        int elderId = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
        LocalDate today = LocalDate.now();
        List<DoseScheduleItem> schedule = reminderService.getTodaySchedule(elderId, today);
        System.out.println("\n--- TODAY'S DOSE SCHEDULE (" + DateUtil.formatDate(today) + ") ---");
        if (schedule.isEmpty()) {
            System.out.println("No active doses scheduled for today.");
            return;
        }
        for (DoseScheduleItem item : schedule) {
            System.out.println("  " + item);
        }
    }

    private void viewDueOrOverdueDoses() throws SQLException {
        int elderId = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
        LocalDateTime now = LocalDateTime.now();
        List<DoseScheduleItem> overdue = reminderService.getDueOrOverdueDoses(elderId, now);
        System.out.println("\n--- DOSES DUE OR OVERDUE RIGHT NOW (" + DateUtil.formatDateTime(now) + ") ---");
        if (overdue.isEmpty()) {
            System.out.println("[OK] All doses up to this time have been logged as taken or skipped!");
            return;
        }
        for (DoseScheduleItem item : overdue) {
            System.out.println("  [!] " + item);
        }
    }

    private void recordDose() throws SQLException {
        int medId = readInt("Enter Medication ID: ", 1, Integer.MAX_VALUE);
        Optional<Medication> medOpt = medicationDAO.getById(medId);
        if (medOpt.isEmpty()) {
            System.out.println("[!] Medication not found.");
            return;
        }

        System.out.println("Recording dose for: " + medOpt.get().getName() + " (" + medOpt.get().getDosage() + ")");
        LocalDateTime scheduledTime = readDateTime("Enter scheduled dose date-time (dd-MM-yyyy HH:mm): ");

        System.out.println("Dose Status:");
        System.out.println(" [1] TAKEN");
        System.out.println(" [2] MISSED");
        System.out.println(" [3] SKIPPED");
        int stChoice = readInt("Select status: ", 1, 3);
        DoseStatus status = switch (stChoice) {
            case 1 -> DoseStatus.TAKEN;
            case 2 -> DoseStatus.MISSED;
            default -> DoseStatus.SKIPPED;
        };

        int logId = reminderService.recordDose(medId, scheduledTime, status);
        System.out.printf("[SUCCESS] Dose recorded as '%s' (Log ID: %d)\n", status, logId);
    }

    private void viewAdherencePercentages() throws SQLException {
        int elderId = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
        double overall = reminderService.calculateElderOverallAdherence(elderId);
        System.out.printf("\n>>> OVERALL ADHERENCE FOR ELDER #%d: %.1f%% <<<\n\n", elderId, overall);

        Map<Medication, Double> breakdown = reminderService.getMedicationAdherenceBreakdown(elderId);
        System.out.println("Individual Prescription Adherence:");
        for (Map.Entry<Medication, Double> e : breakdown.entrySet()) {
            System.out.printf("  * %-20s (%s): %.1f%%\n",
                    e.getKey().getName(), e.getKey().getDosage(), e.getValue());
        }
    }

    private void removeMedication() throws SQLException {
        int medId = readInt("Enter Medication ID to remove: ", 1, Integer.MAX_VALUE);
        if (medicationDAO.delete(medId)) {
            System.out.println("[SUCCESS] Medication course removed.");
        } else {
            System.out.println("[!] Medication not found.");
        }
    }

    // =========================================================================
    // SUBMENU 3: Doctor Appointments & Scheduling
    // =========================================================================
    private void appointmentSchedulingSubmenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- DOCTOR APPOINTMENTS & SCHEDULING ---");
            System.out.println(" [1] Book New Appointment (with conflict detection)");
            System.out.println(" [2] View Upcoming Appointments for Elder");
            System.out.println(" [3] Reschedule Appointment");
            System.out.println(" [4] Cancel Appointment");
            System.out.println(" [5] Mark Appointment as Completed");
            System.out.println(" [6] View Doctor's Daily Schedule");
            System.out.println(" [0] Back to Main Menu");

            int choice = readInt("Select an option: ", 0, 6);
            System.out.println();
            try {
                switch (choice) {
                    case 1 -> bookAppointment();
                    case 2 -> viewUpcomingAppointments();
                    case 3 -> rescheduleAppointment();
                    case 4 -> cancelAppointment();
                    case 5 -> completeAppointment();
                    case 6 -> viewDoctorDailySchedule();
                    case 0 -> back = true;
                    default -> System.out.println("[!] Invalid option.");
                }
            } catch (SQLException e) {
                System.out.println("[DATABASE ERROR] " + e.getMessage());
            }
        }
    }

    private void bookAppointment() throws SQLException {
        System.out.println("--- Book Doctor Appointment ---");
        int elderId = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
        int doctorId = readInt("Enter Doctor ID: ", 1, Integer.MAX_VALUE);
        LocalDateTime dateTime = readDateTime("Enter Appointment Date & Time (dd-MM-yyyy HH:mm): ");
        String purpose = readNonBlank("Enter visit purpose (e.g. Cardiology Follow-up): ");

        try {
            Appointment appt = appointmentScheduler.bookAppointment(elderId, doctorId, dateTime, purpose);
            System.out.printf("[SUCCESS] Appointment #%d confirmed for %s!\n",
                    appt.getAppointmentId(), DateUtil.formatDateTime(appt.getDateTime()));
        } catch (IllegalArgumentException e) {
            System.out.println("[BOOKING REJECTED] " + e.getMessage());
        }
    }

    private void viewUpcomingAppointments() throws SQLException {
        int elderId = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
        List<Appointment> upcoming = appointmentScheduler.getUpcomingAppointmentsForElder(elderId);
        System.out.println("\n--- UPCOMING APPOINTMENTS FOR ELDER #" + elderId + " ---");
        if (upcoming.isEmpty()) {
            System.out.println("No upcoming appointments scheduled.");
            return;
        }
        for (Appointment a : upcoming) {
            String docInfo = doctorDAO.getById(a.getDoctorId())
                    .map(d -> "Dr. " + d.getName() + " (" + d.getSpecialization() + ")")
                    .orElse("Doctor ID " + a.getDoctorId());
            System.out.printf("  Appointment #%d: %s with %s | Purpose: %s\n",
                    a.getAppointmentId(), DateUtil.formatDateTime(a.getDateTime()), docInfo, a.getPurpose());
        }
    }

    private void rescheduleAppointment() throws SQLException {
        int apptId = readInt("Enter Appointment ID to reschedule: ", 1, Integer.MAX_VALUE);
        LocalDateTime newTime = readDateTime("Enter new Date & Time (dd-MM-yyyy HH:mm): ");
        try {
            Appointment appt = appointmentScheduler.rescheduleAppointment(apptId, newTime);
            System.out.printf("[SUCCESS] Appointment rescheduled to: %s\n", DateUtil.formatDateTime(appt.getDateTime()));
        } catch (IllegalArgumentException e) {
            System.out.println("[RESCHEDULE REJECTED] " + e.getMessage());
        }
    }

    private void cancelAppointment() throws SQLException {
        int apptId = readInt("Enter Appointment ID to cancel: ", 1, Integer.MAX_VALUE);
        try {
            if (appointmentScheduler.cancelAppointment(apptId)) {
                System.out.println("[SUCCESS] Appointment cancelled.");
            } else {
                System.out.println("[!] Appointment not found.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("[!] " + e.getMessage());
        }
    }

    private void completeAppointment() throws SQLException {
        int apptId = readInt("Enter Appointment ID to mark completed: ", 1, Integer.MAX_VALUE);
        try {
            if (appointmentScheduler.completeAppointment(apptId)) {
                System.out.println("[SUCCESS] Appointment marked as COMPLETED.");
            } else {
                System.out.println("[!] Appointment not found.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("[!] " + e.getMessage());
        }
    }

    private void viewDoctorDailySchedule() throws SQLException {
        int docId = readInt("Enter Doctor ID: ", 1, Integer.MAX_VALUE);
        LocalDate date = readDate("Enter schedule date (dd-MM-yyyy): ");
        List<Appointment> schedule = appointmentScheduler.getDoctorSchedule(docId, date);
        System.out.println("\n--- SCHEDULE FOR DOCTOR #" + docId + " ON " + DateUtil.formatDate(date) + " ---");
        if (schedule.isEmpty()) {
            System.out.println("No appointments scheduled for this doctor on this day.");
            return;
        }
        for (Appointment a : schedule) {
            String elderName = elderDAO.getById(a.getElderId())
                    .map(Elder::getName)
                    .orElse("Elder #" + a.getElderId());
            System.out.printf("  [%s] Appt #%d: Patient %s | Status: %s | Purpose: %s\n",
                    DateUtil.formatTime(a.getDateTime().toLocalTime()),
                    a.getAppointmentId(), elderName, a.getStatus(), a.getPurpose());
        }
    }

    // =========================================================================
    // SUBMENU 4: Health Log Tracking
    // =========================================================================
    private void healthTrackingSubmenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- HEALTH LOG TRACKING (BP, SUGAR, HEART RATE) ---");
            System.out.println(" [1] Record Blood Pressure Reading");
            System.out.println(" [2] Record Blood Sugar Reading");
            System.out.println(" [3] Record Heart Rate Reading");
            System.out.println(" [4] View History & Moving Averages (7 & 30 Days)");
            System.out.println(" [5] View Trajectory Trends (Rising / Falling / Stable)");
            System.out.println(" [6] View Flagged Abnormal Readings");
            System.out.println(" [0] Back to Main Menu");

            int choice = readInt("Select an option: ", 0, 6);
            System.out.println();
            try {
                switch (choice) {
                    case 1 -> recordBloodPressure();
                    case 2 -> recordBloodSugar();
                    case 3 -> recordHeartRate();
                    case 4 -> viewHealthHistoryAndAverages();
                    case 5 -> viewHealthTrends();
                    case 6 -> viewAbnormalReadings();
                    case 0 -> back = true;
                    default -> System.out.println("[!] Invalid option.");
                }
            } catch (SQLException e) {
                System.out.println("[DATABASE ERROR] " + e.getMessage());
            }
        }
    }

    private void recordBloodPressure() throws SQLException {
        int elderId = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
        if (elderDAO.getById(elderId).isEmpty()) {
            System.out.println("[!] Elder not found.");
            return;
        }

        int systolic = readInt("Enter Systolic pressure (50-260 mmHg): ", 50, 260);
        int diastolic = readInt("Enter Diastolic pressure (30-160 mmHg): ", 30, 160);
        if (systolic <= diastolic) {
            System.out.println("[!] Systolic pressure must be greater than diastolic pressure.");
            return;
        }
        String notes = readString("Enter notes (or press Enter): ");

        BloodPressureRecord bp = new BloodPressureRecord(0, elderId, LocalDateTime.now(), notes, systolic, diastolic);
        healthMonitor.addReading(bp);

        printClinicalEvaluationFeedback(bp);
    }

    private void recordBloodSugar() throws SQLException {
        int elderId = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
        if (elderDAO.getById(elderId).isEmpty()) {
            System.out.println("[!] Elder not found.");
            return;
        }

        double value = readDouble("Enter Blood Sugar (20.0 - 600.0 mg/dL): ", 20.0, 600.0);
        System.out.println("Reading Context:");
        System.out.println(" [1] FASTING (Before meal)");
        System.out.println(" [2] POST_MEAL (2 hours after meal)");
        System.out.println(" [3] RANDOM");
        int typeChoice = readInt("Choose context: ", 1, 3);
        ReadingType type = switch (typeChoice) {
            case 1 -> ReadingType.FASTING;
            case 2 -> ReadingType.POST_MEAL;
            default -> ReadingType.RANDOM;
        };
        String notes = readString("Enter notes (or press Enter): ");

        BloodSugarRecord bs = new BloodSugarRecord(0, elderId, LocalDateTime.now(), notes, value, type);
        healthMonitor.addReading(bs);

        printClinicalEvaluationFeedback(bs);
    }

    private void recordHeartRate() throws SQLException {
        int elderId = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
        if (elderDAO.getById(elderId).isEmpty()) {
            System.out.println("[!] Elder not found.");
            return;
        }

        int bpm = readInt("Enter Heart Rate (30 - 220 bpm): ", 30, 220);
        String notes = readString("Enter notes (or press Enter): ");

        HeartRateRecord hr = new HeartRateRecord(0, elderId, LocalDateTime.now(), notes, bpm);
        healthMonitor.addReading(hr);

        printClinicalEvaluationFeedback(hr);
    }

    private void printClinicalEvaluationFeedback(HealthRecord record) {
        HealthStatus status = record.evaluate();
        System.out.println("\n------------------------------------------------------------");
        System.out.printf("Reading Recorded: %s | Measurement: %s\n", record.getType(), record.getSummary());
        System.out.printf("Clinical Status : %s\n", status.getDisplayName().toUpperCase());

        switch (status) {
            case NORMAL -> System.out.println("[STATUS: NORMAL] Reading is within healthy target parameters.");
            case ELEVATED -> System.out.println("[WARNING: ELEVATED] Reading is mildly above ideal range. Monitor closely.");
            case HIGH -> System.out.println("[WARNING: HIGH] Reading is significantly elevated. Consult attending doctor.");
            case LOW -> System.out.println("[WARNING: LOW] Reading is below safe limits. Ensure nourishment and rest.");
            case CRITICAL -> {
                System.out.println("************************************************************");
                System.out.println(" [!] CRITICAL CLINICAL EMERGENCY DETECTED! ");
                System.out.println(" An automatic emergency alert has been logged and dispatched");
                System.out.println(" to designated emergency contacts and attending physician.");
                System.out.println("************************************************************");
            }
        }
        System.out.println("------------------------------------------------------------");
    }

    private void viewHealthHistoryAndAverages() throws SQLException {
        int elderId = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
        System.out.println("\n--- MOVING STATISTICAL AVERAGES ---");
        System.out.println("  * " + healthMonitor.get7DayAverage(elderId, HealthRecordDAO.TYPE_BP));
        System.out.println("  * " + healthMonitor.get30DayAverage(elderId, HealthRecordDAO.TYPE_BP));
        System.out.println("  * " + healthMonitor.get7DayAverage(elderId, HealthRecordDAO.TYPE_SUGAR));
        System.out.println("  * " + healthMonitor.get30DayAverage(elderId, HealthRecordDAO.TYPE_SUGAR));
        System.out.println("  * " + healthMonitor.get7DayAverage(elderId, HealthRecordDAO.TYPE_HEART));
        System.out.println("  * " + healthMonitor.get30DayAverage(elderId, HealthRecordDAO.TYPE_HEART));

        System.out.println("\n--- ALL RECENT HEALTH READINGS ---");
        List<HealthRecord> readings = healthMonitor.getAllReadings(elderId);
        if (readings.isEmpty()) {
            System.out.println("No readings recorded.");
            return;
        }
        for (HealthRecord r : readings) {
            System.out.printf("  [%s] %-14s : %-20s | Status: %-8s | Notes: %s\n",
                    DateUtil.formatDateTime(r.getRecordedAt()), r.getType(),
                    r.getSummary(), r.evaluate().name(), r.getNotes());
        }
    }

    private void viewHealthTrends() throws SQLException {
        int elderId = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
        System.out.println("\n--- TRAJECTORY TRENDS FOR ELDER #" + elderId + " ---");
        Trend bpTrend = healthMonitor.detectTrend(elderId, HealthRecordDAO.TYPE_BP);
        Trend sugarTrend = healthMonitor.detectTrend(elderId, HealthRecordDAO.TYPE_SUGAR);
        Trend hrTrend = healthMonitor.detectTrend(elderId, HealthRecordDAO.TYPE_HEART);

        System.out.println("  * Blood Pressure Trajectory : " + bpTrend.getDescription());
        System.out.println("  * Blood Sugar Trajectory    : " + sugarTrend.getDescription());
        System.out.println("  * Heart Rate Trajectory     : " + hrTrend.getDescription());
    }

    private void viewAbnormalReadings() throws SQLException {
        int elderId = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
        List<HealthRecord> abnormal = healthMonitor.getAbnormalReadings(elderId);
        System.out.println("\n--- FLAGGED ABNORMAL READINGS FOR ELDER #" + elderId + " (" + abnormal.size() + " found) ---");
        if (abnormal.isEmpty()) {
            System.out.println("[OK] No abnormal readings recorded!");
            return;
        }
        for (HealthRecord r : abnormal) {
            System.out.printf("  [!] %s: %s | %s | Status: %s\n",
                    DateUtil.formatDateTime(r.getRecordedAt()), r.getType(), r.getSummary(), r.evaluate());
        }
    }

    // =========================================================================
    // SUBMENU 5: Reports & Dossiers
    // =========================================================================
    private void reportsSubmenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- REPORTS & DOSSIERS ---");
            System.out.println(" [1] Health Diagnostic Summary Report");
            System.out.println(" [2] Medication Adherence & Compliance Report");
            System.out.println(" [3] Doctor Appointment History Report");
            System.out.println(" [4] Emergency Alert & Dispatch Log");
            System.out.println(" [5] Complete Master Elder Dossier (All Reports Combined)");
            System.out.println(" [0] Back to Main Menu");

            int choice = readInt("Select an option: ", 0, 5);
            System.out.println();
            try {
                switch (choice) {
                    case 1 -> {
                        int id = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
                        System.out.println(reportGenerator.generateHealthSummaryReport(id));
                    }
                    case 2 -> {
                        int id = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
                        System.out.println(reportGenerator.generateMedicationAdherenceReport(id));
                    }
                    case 3 -> {
                        int id = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
                        System.out.println(reportGenerator.generateAppointmentHistoryReport(id));
                    }
                    case 4 -> {
                        int id = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
                        System.out.println(reportGenerator.generateEmergencyAlertLogReport(id));
                    }
                    case 5 -> {
                        int id = readInt("Enter Elder ID: ", 1, Integer.MAX_VALUE);
                        System.out.println(reportGenerator.generateFullElderReport(id));
                    }
                    case 0 -> back = true;
                    default -> System.out.println("[!] Invalid option.");
                }
            } catch (SQLException | IllegalArgumentException e) {
                System.out.println("[REPORT ERROR] " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // EMERGENCY SOS & ALERTS
    // =========================================================================
    private void triggerManualSOS() throws SQLException {
        System.out.println("********************************************************************************");
        System.out.println("                       *** INITIATING EMERGENCY SOS ***                         ");
        System.out.println("********************************************************************************");
        int elderId = readInt("Enter Elder ID in emergency: ", 1, Integer.MAX_VALUE);
        String reason = readString("Reason/Notes (e.g. Chest pain, Fall, Dizziness): ");

        try {
            SOSResult sosResult = emergencyService.triggerSOS(elderId, reason);
            System.out.println("\n" + sosResult.toFormattedAlertBanner());
            System.out.println("[DISPATCH RECORDED] Emergency alert logged permanently in database.");
        } catch (IllegalArgumentException e) {
            System.out.println("[!] " + e.getMessage());
        }
    }

    private void viewEmergencyAlerts() throws SQLException {
        int elderId = readInt("Enter Elder ID to view emergency history: ", 1, Integer.MAX_VALUE);
        List<EmergencyAlert> alerts = emergencyService.getAlertHistory(elderId);
        System.out.println("\n--- EMERGENCY ALERT LOG FOR ELDER #" + elderId + " ---");
        if (alerts.isEmpty()) {
            System.out.println("No emergency alerts on record for this elder.");
            return;
        }
        for (EmergencyAlert a : alerts) {
            System.out.printf("  Alert #%d [%s] Reason: %s | Notified: %s\n",
                    a.getAlertId(), DateUtil.formatDateTime(a.getTimestamp()), a.getReason(), a.getContactsNotified());
        }
    }

    // =========================================================================
    // SAFE INPUT HELPER METHODS (Never crash on bad input)
    // =========================================================================
    private String readNonBlank(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            if (input != null && !input.trim().isEmpty()) {
                return input.trim();
            }
            System.out.println("[!] Input cannot be empty. Please enter a value.");
        }
    }

    private String readString(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine();
        return (input != null) ? input.trim() : "";
    }

    private String readOptionalString(String prompt, String defaultValue) {
        System.out.print(prompt);
        String input = scanner.nextLine();
        if (input == null || input.trim().isEmpty()) {
            return defaultValue;
        }
        return input.trim();
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                int val = Integer.parseInt(line.trim());
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.printf("[!] Please enter a number between %d and %d.\n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid number format. Please enter an integer.");
            }
        }
    }

    private int readOptionalInt(String prompt, int defaultValue, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            if (line == null || line.trim().isEmpty()) {
                return defaultValue;
            }
            try {
                int val = Integer.parseInt(line.trim());
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.printf("[!] Please enter a number between %d and %d.\n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid number format. Please enter an integer.");
            }
        }
    }

    private double readDouble(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                double val = Double.parseDouble(line.trim());
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.printf("[!] Please enter a value between %.1f and %.1f.\n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid numeric format. Please enter a valid decimal number.");
            }
        }
    }

    private String readPhone(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                return ValidationUtil.validatePhone(line);
            } catch (IllegalArgumentException e) {
                System.out.println("[!] " + e.getMessage());
            }
        }
    }

    private String readOptionalPhone(String prompt, String defaultPhone) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            if (line == null || line.trim().isEmpty()) {
                return defaultPhone;
            }
            try {
                return ValidationUtil.validatePhone(line);
            } catch (IllegalArgumentException e) {
                System.out.println("[!] " + e.getMessage());
            }
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                return DateUtil.parseDate(line);
            } catch (IllegalArgumentException e) {
                System.out.println("[!] " + e.getMessage());
            }
        }
    }

    private LocalTime readTime(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                return DateUtil.parseTime(line);
            } catch (IllegalArgumentException e) {
                System.out.println("[!] " + e.getMessage());
            }
        }
    }

    private LocalDateTime readDateTime(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            try {
                return DateUtil.parseDateTime(line);
            } catch (IllegalArgumentException e) {
                System.out.println("[!] " + e.getMessage());
            }
        }
    }

    private boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine();
            if (line != null) {
                String trimmed = line.trim().toLowerCase();
                if (trimmed.equals("y") || trimmed.equals("yes")) {
                    return true;
                }
                if (trimmed.equals("n") || trimmed.equals("no")) {
                    return false;
                }
            }
            System.out.println("[!] Please enter 'Y' for Yes or 'N' for No.");
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
