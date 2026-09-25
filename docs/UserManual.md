# Elder Care Assistance System - User Manual & Operating Guide

Welcome to the **Elder Care Assistance System**! This comprehensive guide walks you through system requirements, compilation, running the application, and utilizing every console menu feature.

---

## 1. System Requirements & Prerequisites

- **Java Development Kit (JDK):** Version 17 LTS or higher (verified on JDK 17.0.12).
- **Apache Maven:** Version 3.8.0 or higher.
- **Operating System:** Windows, macOS, or Linux (cross-platform console support).
- **Memory & Disk:** Minimal (requires $< 50$ MB RAM and $< 10$ MB disk space).
- **Database:** SQLite 3 is embedded automatically via the JDBC driver; **no external database server installation or configuration is required**.

---

## 2. Building and Running the Application

### 2.1. From the Command Line (Windows PowerShell / CMD)

Open a terminal in the project root directory (`c:\eddercare management system_oops_mini project`).

1. **Compile the project:**
   ```powershell
   .\mvnw.bat compile
   ```
   *(Or `mvn compile` if Maven is on your system PATH)*

2. **Run all automated tests:**
   ```powershell
   .\mvnw.bat test
   ```

3. **Launch the Console Application:**
   ```powershell
   .\mvnw.bat exec:java
   ```
   *(Or double-click `run.bat`)*

4. **Launch the Web Dashboard Frontend:**
   ```powershell
   .\mvnw.bat exec:java -Pweb
   ```
   *(Or double-click `run-web.bat`)*  
   Then open your web browser at: **`http://localhost:8080/`**

5. **Package into an executable JAR file:**
   ```powershell
   .\mvnw.bat clean package
   ```
   Run the resulting standalone JAR directly with Java:
   ```powershell
   java -jar target/eldercare-assistance-system-1.0.0.jar
   ```

---

### 2.2. Running from an IDE (IntelliJ IDEA / Eclipse / VS Code)

#### IntelliJ IDEA
1. Open IntelliJ IDEA and select **File &rarr; Open...**
2. Choose the root project folder containing `pom.xml`.
3. Wait for IntelliJ to sync Maven dependencies automatically.
4. Navigate to `src/main/java/com/eldercare/ui/Main.java`.
5. Right-click on `Main.java` and select **Run 'Main.main()'** (or press `Shift + F10`).

#### Eclipse IDE
1. Select **File &rarr; Import... &rarr; Existing Maven Projects**.
2. Browse to the project folder and click **Finish**.
3. Open `src/main/java/com/eldercare/ui/Main.java`.
4. Right-click and choose **Run As &rarr; Java Application**.

---

## 3. Initial Startup & Loading Sample Data

On the first run, the system automatically checks if the database is brand new:
```text
================================================================================
               ELDER CARE ASSISTANCE SYSTEM - CONSOLE PLATFORM                  
          Empowering Independent Living with Health, Safety & Care              
================================================================================

[!] The database is currently empty (first run detected).
Would you like to seed demonstration sample data (2 elders, 3 doctors, meds, readings)? (Y/N): y
[SUCCESS] Sample data loaded successfully! You can now test all features immediately.
```
> **Recommendation:** Enter `Y` on your first launch. This pre-loads 2 elders, 3 doctors, 1 caregiver, 4 emergency contacts, active prescriptions, 14 days of vitals, moving averages, and appointments so you can test features without manual typing.

---

## 4. Main Menu Overview

```text
================================================================================
                                MAIN MENU                                       
================================================================================
 [1] Elder & Contact Management
 [2] Medication Reminders & Adherence
 [3] Doctor Appointments & Scheduling
 [4] Health Log Tracking (BP, Sugar, Heart Rate)
 [5] Reports & Dossiers
 [6] Emergency Alert History
 [9] *** ONE-TOUCH EMERGENCY SOS ***
 [0] Exit Application
================================================================================
```

---

## 5. Walkthrough of Every Menu Option

### 5.1. Elder & Contact Management (`Menu [1]`)

#### [1] Register New Elder
Allows registering an elderly individual:
- **Sample Input:**
  - Name: `Arthur William Jenkins`
  - Phone: `9123456780` *(must be exactly 10 digits)*
  - Address: `42 Oakridge Blvd, Riverdale`
  - Age: `82`
  - Gender: `Male`
  - Blood Group: `A+`
  - Conditions: `Arrhythmia, Mild Arthritis`
- **Output:** `[SUCCESS] Elder 'Arthur William Jenkins' registered with ID: 2`

#### [2] View All Elders
Prints a structured directory table of all registered elders with their age, contact, and chronic conditions.

#### [3] Search Elder by Name or ID
Allows finding an elder by exact ID or case-insensitive keyword search (e.g. typing `margaret` or `vance`). Displays a full profile card with their prioritized emergency contacts.

#### [4] Update Elder Details
Allows modifying elder details. Pressing Enter keeps the existing value without retyping.

#### [5] Delete Elder
Deletes an elder and cascades deletion to their emergency contacts, prescriptions, appointments, and health records.

#### [6] Manage Emergency Contacts
Manage contacts for an elder:
- **Add New Contact:** Input name, relation, 10-digit phone, and priority (`1` = call first).
- **Edit Contact Priority:** Adjust responder ordering.
- **Remove Contact:** Remove a contact by ID.

#### [7] & [8] Register Caregiver & Doctor
Add certified aides or medical specialists to the system.

---

### 5.2. Medication Reminders & Adherence (`Menu [2]`)

#### [1] Add Prescribed Medication
- **Sample Input:**
  - Elder ID: `1`
  - Medication name: `Metformin`
  - Dosage: `500 mg`
  - Times per day: `2`
  - Dose 1 time: `08:00`
  - Dose 2 time: `20:00`
  - Start date: `01-09-2026`
  - End date: `01-12-2026`
  - Special instructions: `Take with meals`
- **Output:** `[SUCCESS] Medication 'Metformin' saved with ID: 1`

#### [3] View Today's Dose Schedule
Computes today's active medicines and lists each dose time in chronological order:
```text
--- TODAY'S DOSE SCHEDULE (25-09-2026) ---
  [08:00] Metformin (500 mg) - Take with meal | Status: PENDING
  [09:00] Lisinopril (10 mg) - Take in the morning with water | Status: PENDING
  [20:00] Metformin (500 mg) - Take with meal | Status: PENDING
```

#### [4] View Due / Overdue Doses
Filters today's schedule against the current computer time. If a scheduled time has passed and has not been logged as `TAKEN` or `SKIPPED`, it is flagged with `[!]`.

#### [5] Record Dose Status
Mark a dose as `TAKEN`, `MISSED`, or `SKIPPED`. Re-recording the same dose time updates the existing record rather than creating a duplicate.

#### [6] View Adherence Percentages
Calculates the mathematically exact adherence percentage:
$$\text{Adherence \%} = \frac{\text{Taken Doses}}{\text{Total Logged Doses}} \times 100$$
Outputs overall elder compliance as well as a per-prescription breakdown.

---

### 5.3. Doctor Appointments & Scheduling (`Menu [3]`)

#### [1] Book New Appointment (with Conflict Detection)
- **Sample Input:**
  - Elder ID: `1`
  - Doctor ID: `1`
  - Date & Time: `30-09-2026 14:00` *(Format: `dd-MM-yyyy HH:mm`)*
  - Purpose: `Cardiac Follow-up`
- **Conflict Checking Rules:**
  - **Past Date:** Rejects any date before current system time.
  - **Doctor 30-min Buffer:** If Dr. Sharma already has an appointment scheduled at `14:15`, booking at `14:00` is rejected with:
    `[BOOKING REJECTED] Doctor conflict: Dr. (ID 1) already has an appointment at 30-09-2026 14:15 (within 30 minutes).`
  - **Elder 30-min Buffer:** If the elder has another appointment scheduled with a different doctor at `14:10`, booking is rejected with:
    `[BOOKING REJECTED] Elder conflict: Elder (ID 1) already has an appointment at 30-09-2026 14:10 (within 30 minutes).`

#### [2] View Upcoming Appointments
Lists active scheduled consultations occurring from today onwards.

#### [3] Reschedule Appointment
Moves an existing consultation to a new date and time, running conflict checks against all other bookings.

#### [4] & [5] Cancel / Complete Appointment
Transitions status between `SCHEDULED`, `COMPLETED`, and `CANCELLED`.

#### [6] View Doctor's Daily Schedule
Allows doctors or clinic staff to query their complete schedule for a given date.

---

### 5.4. Health Log Tracking (`Menu [4]`)

#### [1] Record Blood Pressure Reading
- **Input:** Systolic (mmHg), Diastolic (mmHg), Notes.
- **Clinical Evaluation:**
  - Evaluates according to AHA criteria: `NORMAL`, `ELEVATED`, `HIGH`, `LOW`, or `CRITICAL`.
  - If reading is $\ge 180$ systolic or $\ge 120$ diastolic, **Hypertensive Crisis** is detected, and an emergency alert is automatically dispatched to responders.

#### [2] Record Blood Sugar Reading
- **Input:** Blood sugar in mg/dL, Context (`FASTING`, `POST_MEAL`, `RANDOM`), Notes.
- **Clinical Evaluation:** Thresholds automatically adjust based on reading context (e.g. 115 mg/dL is `NORMAL` post-meal, but `ELEVATED` fasting).

#### [3] Record Heart Rate Reading
- **Input:** Resting pulse in BPM (beats per minute).
- **Clinical Evaluation:** Evaluates resting pulse against normal adult bounds (60–100 bpm).

#### [4] View History & Moving Averages (7 & 30 Days)
Displays statistical averages computed over the last 7 days and 30 days:
```text
--- MOVING STATISTICAL AVERAGES ---
  * 7-Day BLOOD_PRESSURE Avg: 131.8 / 85.0 mmHg (6 readings)
  * 30-Day BLOOD_PRESSURE Avg: 128.2 / 81.4 mmHg (14 readings)
  * 7-Day BLOOD_SUGAR Avg: 119.8 mg/dL (6 readings)
  * 30-Day BLOOD_SUGAR Avg: 107.2 mg/dL (14 readings)
```

#### [5] View Trajectory Trends
Analyzes historical reading trajectory and reports direction:
- `Rising (^)`: Metric is trending upward over time.
- `Falling (v)`: Metric is trending downward over time.
- `Stable (=)`: Metric is steady.

#### [6] View Flagged Abnormal Readings
Filters out normal readings, highlighting measurements that require clinical attention.

---

### 5.5. Reports & Dossiers (`Menu [5]`)

Generates neatly formatted text reports:
1. **Health Summary Report:** Elder vitals, 7-day and 30-day averages, trends, full reading history table, and abnormal flags.
2. **Medication Adherence Report:** Overall compliance percentage, active course status table, and recent dose logs.
3. **Appointment History Report:** Table of past, completed, and upcoming appointments.
4. **Emergency Alert Log:** Audit trail of all SOS alerts with timestamps and notified contacts.
5. **Complete Master Dossier:** Combines all 4 reports into a unified patient document.

---

### 5.6. Emergency Support & One-Touch SOS (`Menu [9]`)

Pressing `9` from the Main Menu triggers an immediate SOS:
```text
================================================================================
                         *** EMERGENCY SOS TRIGGERED ***                        
================================================================================
Alert ID    : #2
Time        : 25-09-2026 10:15
Elder       : Margaret Eleanor Vance (Age: 78, Gender: Female)
Phone       : 9876543210
Address     : 14 Rosewood Lane, Greenwood
Blood Group : O+
Conditions  : Hypertension, Type 2 Diabetes
Reason      : Severe dizziness and blurred vision
--------------------------------------------------------------------------------
NOTIFIED CONTACTS (ORDERED BY PRIORITY):
  Priority 1: David Vance (Son) - Phone: 9871122334
  Priority 2: Sarah Vance (Daughter-in-law) - Phone: 9872233445
--------------------------------------------------------------------------------
PRIMARY / ATTENDING DOCTOR:
  Dr. Rajesh Sharma (Cardiology) | Hospital: City General Hospital | Phone: 9845112233
================================================================================
```

---

## 6. Input Validation & Error Handling

The application is designed to be **completely crash-proof**:
- **Phone Numbers:** Must be 10 digits (`^\d{10}$`). Re-prompts on alphabetic input, spaces, or wrong length.
- **Dates & Times:** Dates must match `dd-MM-yyyy` (e.g. `25-09-2026`) and times must match `HH:mm` (e.g. `09:30`).
- **Health Boundaries:** Systolic pressure must be strictly greater than diastolic pressure. Values outside human survivability (e.g. BP $< 50$ or $> 260$) are rejected.
- **Menu Selections:** Non-numeric characters or out-of-range choices display a friendly prompt and redisplay the options.

---

## 7. Troubleshooting Guide

| Issue | Cause | Solution |
| :--- | :--- | :--- |
| `'mvn'` is not recognized as a command | Maven is not added to your system `PATH` | Run the included wrapper `.\mvnw.bat` in the project root folder. It uses the pre-installed Maven binary automatically. |
| Database locked / SQLite error | Another process holds an exclusive lock on `eldercare.db` | Close any external SQLite viewers (e.g. DB Browser for SQLite) before running the application. |
| Appointment booking rejected with past-date error | Input date is in the past | Enter a future date and time relative to your computer's current clock. |
| Corrupted or scrambled database | Experimental manual database edits | Delete `eldercare.db`. On next launch, the system will recreate all tables cleanly and offer to seed sample data. |
