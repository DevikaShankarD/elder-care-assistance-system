# Elder Care Assistance System - Test Cases & Verification Report

**Test Framework:** JUnit 5 (Jupiter 5.10.2)  
**Execution Command:** `mvn test` (or `.\mvnw.bat test`)  
**Total Tests Executed:** 53  
**Passing Tests:** 53 (100% Pass Rate)  
**Failures / Errors:** 0  
**Execution Date:** September 2026  

---

## 1. Summary of Test Execution

| Module / Test Suite | Class Name | Tests Run | Pass | Fail | Description |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Health Evaluation** | `HealthRecordEvaluationTest` | 20 | 20 | 0 | Dynamic polymorphism, AHA BP ranges, ADA sugar thresholds, resting heart rate, and input validation bounds. |
| **Appointment Scheduler** | `AppointmentSchedulerTest` | 9 | 9 | 0 | Future booking, past-date rejection, 30-min doctor conflicts, 30-min elder conflicts, rescheduling, and status transitions. |
| **Reminder Service** | `ReminderServiceTest` | 6 | 6 | 0 | Daily schedule calculation, active/expired date filtering, overdue dose detection, status logging, and mathematical adherence %. |
| **Emergency Service** | `EmergencyServiceTest` | 4 | 4 | 0 | Priority contact ordering (1 before 2 before 3), doctor/medical info linking, automated critical alerts, and history logging. |
| **Health Monitoring** | `HealthMonitorTest` | 5 | 5 | 0 | Polymorphic reading addition, automated critical alert dispatch, 7/30-day moving averages, trend detection, and abnormal filtering. |
| **DAO Persistence** | `DAOCrudTest` | 7 | 7 | 0 | Full CRUD operations for all entities against an isolated temporary SQLite database, including polymorphic single-table mapping. |
| **Database Manager** | `DatabaseManagerTest` | 2 | 2 | 0 | Schema creation, verifying all 9 relational tables, foreign key enablement, and initial empty-state check. |
| **TOTAL** | | **53** | **53** | **0** | **100% Automated Test Pass Rate** |

---

## 2. Comprehensive Test Case Matrix

| Test ID | Module | Description | Input | Expected Output | Actual Output | Pass/Fail |
| :---: | :--- | :--- | :--- | :--- | :--- | :---: |
| **TC-01** | Model / BP | Normal BP Range Evaluation | Systolic: 115, Diastolic: 75 | `HealthStatus.NORMAL`, Summary contains "Normal" | `HealthStatus.NORMAL`, "115/75 mmHg (Normal)" | **PASS** |
| **TC-02** | Model / BP | Low BP Boundary (Systolic < 90) | Systolic: 85, Diastolic: 58 | `HealthStatus.LOW` | `HealthStatus.LOW` | **PASS** |
| **TC-03** | Model / BP | Low BP Boundary (Diastolic < 60) | Systolic: 100, Diastolic: 55 | `HealthStatus.LOW` | `HealthStatus.LOW` | **PASS** |
| **TC-04** | Model / BP | Elevated BP Boundary (120-129 / < 80) | Systolic: 125, Diastolic: 76 | `HealthStatus.ELEVATED` | `HealthStatus.ELEVATED` | **PASS** |
| **TC-05** | Model / BP | High BP / Hypertension Stage 1 (Systolic >= 130) | Systolic: 135, Diastolic: 78 | `HealthStatus.HIGH` | `HealthStatus.HIGH` | **PASS** |
| **TC-06** | Model / BP | High BP / Hypertension Stage 1 (Diastolic >= 80) | Systolic: 124, Diastolic: 84 | `HealthStatus.HIGH` | `HealthStatus.HIGH` | **PASS** |
| **TC-07** | Model / BP | Critical BP / Hypertensive Crisis (Systolic >= 180) | Systolic: 185, Diastolic: 95 | `HealthStatus.CRITICAL` | `HealthStatus.CRITICAL` | **PASS** |
| **TC-08** | Model / BP | Critical BP / Hypertensive Crisis (Diastolic >= 120) | Systolic: 160, Diastolic: 125 | `HealthStatus.CRITICAL` | `HealthStatus.CRITICAL` | **PASS** |
| **TC-09** | Model / BP | Invalid BP Validation (Systolic <= Diastolic) | Systolic: 80, Diastolic: 120 | `IllegalArgumentException` thrown | `IllegalArgumentException` caught | **PASS** |
| **TC-10** | Model / BP | Out-of-Bounds BP Validation | Systolic: 280, Diastolic: 80 | `IllegalArgumentException` thrown | `IllegalArgumentException` caught | **PASS** |
| **TC-11** | Model / Sugar | Fasting Blood Sugar: Normal (70–99 mg/dL) | 88.0 mg/dL, Context: FASTING | `HealthStatus.NORMAL` | `HealthStatus.NORMAL` | **PASS** |
| **TC-12** | Model / Sugar | Fasting Blood Sugar: Low (< 70 mg/dL) | 65.0 mg/dL, Context: FASTING | `HealthStatus.LOW` | `HealthStatus.LOW` | **PASS** |
| **TC-13** | Model / Sugar | Fasting Blood Sugar: Elevated (100–125 mg/dL) | 115.0 mg/dL, Context: FASTING | `HealthStatus.ELEVATED` | `HealthStatus.ELEVATED` | **PASS** |
| **TC-14** | Model / Sugar | Fasting Blood Sugar: High Diabetic (126–249 mg/dL) | 160.0 mg/dL, Context: FASTING | `HealthStatus.HIGH` | `HealthStatus.HIGH` | **PASS** |
| **TC-15** | Model / Sugar | Fasting Blood Sugar: Critical Hypoglycemia (< 50) | 42.0 mg/dL, Context: FASTING | `HealthStatus.CRITICAL` | `HealthStatus.CRITICAL` | **PASS** |
| **TC-16** | Model / Sugar | Fasting Blood Sugar: Critical Hyperglycemia (>= 250) | 265.0 mg/dL, Context: FASTING | `HealthStatus.CRITICAL` | `HealthStatus.CRITICAL` | **PASS** |
| **TC-17** | Model / Sugar | Post-Meal Blood Sugar: Normal (70–139 mg/dL) | 125.0 mg/dL, Context: POST_MEAL | `HealthStatus.NORMAL` | `HealthStatus.NORMAL` | **PASS** |
| **TC-18** | Model / Sugar | Post-Meal Blood Sugar: Elevated (140–199 mg/dL) | 175.0 mg/dL, Context: POST_MEAL | `HealthStatus.ELEVATED` | `HealthStatus.ELEVATED` | **PASS** |
| **TC-19** | Model / Sugar | Post-Meal Blood Sugar: Critical Spike (>= 300) | 320.0 mg/dL, Context: POST_MEAL | `HealthStatus.CRITICAL` | `HealthStatus.CRITICAL` | **PASS** |
| **TC-20** | Model / Sugar | Unrealistic Blood Sugar Bounds Validation | Values: 10.0 mg/dL and 700.0 mg/dL | `IllegalArgumentException` thrown | `IllegalArgumentException` caught | **PASS** |
| **TC-21** | Model / Pulse | Heart Rate: Normal Adult Pulse (60–100 bpm) | 72 bpm | `HealthStatus.NORMAL` | `HealthStatus.NORMAL` | **PASS** |
| **TC-22** | Model / Pulse | Heart Rate: Low Bradycardia (40–59 bpm) | 52 bpm | `HealthStatus.LOW` | `HealthStatus.LOW` | **PASS** |
| **TC-23** | Model / Pulse | Heart Rate: Elevated Mild Tachycardia (101–120) | 110 bpm | `HealthStatus.ELEVATED` | `HealthStatus.ELEVATED` | **PASS** |
| **TC-24** | Model / Pulse | Heart Rate: High Tachycardia (121–139) | 130 bpm | `HealthStatus.HIGH` | `HealthStatus.HIGH` | **PASS** |
| **TC-25** | Model / Pulse | Heart Rate: Critical Severe Bradycardia (< 40) | 36 bpm | `HealthStatus.CRITICAL` | `HealthStatus.CRITICAL` | **PASS** |
| **TC-26** | Model / Pulse | Heart Rate: Critical Severe Tachycardia (>= 140) | 155 bpm | `HealthStatus.CRITICAL` | `HealthStatus.CRITICAL` | **PASS** |
| **TC-27** | Service / Appt | Book Appointment Success (Future Slot) | Elder 1, Doctor 1, DateTime: +2 Days, 10:00 | Confirmed Appointment with ID > 0, Status: SCHEDULED | ID > 0, Status: SCHEDULED | **PASS** |
| **TC-28** | Service / Appt | Past-Date Appointment Rejection | DateTime: -2 Hours | `IllegalArgumentException` ("past date") | `IllegalArgumentException` caught | **PASS** |
| **TC-29** | Service / Appt | Doctor Conflict Rejection (< 30 min buffer) | Dr. 1 booked at 14:00; attempt Dr. 1 at 14:15 | `IllegalArgumentException` ("Doctor conflict") | `IllegalArgumentException` caught | **PASS** |
| **TC-30** | Service / Appt | Elder Conflict Rejection (< 30 min buffer) | Elder 1 booked at 15:00; attempt Elder 1 at 15:20 | `IllegalArgumentException` ("Elder conflict") | `IllegalArgumentException` caught | **PASS** |
| **TC-31** | Service / Appt | Booking Allowed Outside Conflict Buffer (>= 30 min) | Dr. 1 booked at 10:00; booking at 10:30 | Booking confirmed, Status: SCHEDULED | Booking confirmed, Status: SCHEDULED | **PASS** |
| **TC-32** | Service / Appt | Reschedule Appointment Successfully | Appointment #1 moved to +1 day | DateTime updated, Status: SCHEDULED | DateTime updated, Status: SCHEDULED | **PASS** |
| **TC-33** | Service / Appt | Cancel Appointment Transition | Appointment #1 cancelled | Returns true, status updated to CANCELLED | Status: CANCELLED verified | **PASS** |
| **TC-34** | Service / Appt | Complete Appointment Transition | Appointment #1 completed | Returns true, status updated to COMPLETED | Status: COMPLETED verified | **PASS** |
| **TC-35** | Service / Appt | Doctor Daily Schedule Agenda | Dr. 1 on Date X with 2 bookings | 2 appointments returned in chronological order | 2 appointments ordered by time | **PASS** |
| **TC-36** | Service / Reminder | Today's Dose Schedule Active Prescription Filtering | Active Metformin (08:00, 20:00) | 2 dose schedule items returned ordered by time | 2 items returned at 08:00 and 20:00 | **PASS** |
| **TC-37** | Service / Reminder | Expired Prescription Exclusion | Medication with end date in the past | 0 doses scheduled for today | 0 doses scheduled | **PASS** |
| **TC-38** | Service / Reminder | Due and Overdue Dose Detection | 08:00 and 18:00 doses; evaluation at 12:00 PM | 08:00 dose flagged overdue; 18:00 excluded | 1 dose returned (08:00) | **PASS** |
| **TC-39** | Service / Reminder | Dose Status Recording & Idempotent Update | Record 21:00 as TAKEN, then update to MISSED | Same log ID updated to MISSED without duplicate | Log ID preserved, status updated | **PASS** |
| **TC-40** | Service / Reminder | Medication Adherence Percentage Calculation | 4 logged doses: 3 TAKEN, 1 MISSED | Exact 75.0% adherence | 75.0% adherence | **PASS** |
| **TC-41** | Service / Reminder | Elder Overall Adherence Across Prescriptions | Med 1 (2/2 taken) + Med 2 (0/2 taken) = 2/4 | Exact 50.0% adherence | 50.0% adherence | **PASS** |
| **TC-42** | Service / Emergency | SOS Emergency Contact Priority Ordering | Contacts added with priorities 3, 1, 2 | Returned list strictly ordered: Priority 1, 2, 3 | Ordered: P1 (Alice), P2 (Bob), P3 (Charlie) | **PASS** |
| **TC-43** | Service / Emergency | SOS Contextual Dispatch Information | Elder medical info and attending doctor | Banner contains conditions and doctor name | Banner formatted with full context | **PASS** |
| **TC-44** | Service / Emergency | Automated Critical Health Alert Dispatch | Trigger critical alert for 190/125 BP | Alert saved with reason "AUTOMATIC ALERT" | Alert persisted in database | **PASS** |
| **TC-45** | Service / Emergency | Alert History Retrieval Order | 2 alerts triggered sequentially | History returned latest-first (`ORDER BY timestamp DESC, id DESC`) | Most recent alert is first element | **PASS** |
| **TC-46** | Service / Monitor | Polymorphic Health Reading Persistence | Add BP, Sugar, and Heart Rate via HealthRecord ref | All 3 persisted with unique IDs | 3 records saved and retrievable | **PASS** |
| **TC-47** | Service / Monitor | Automatic Alert on Critical Reading Addition | Add BloodPressureRecord (190/125 mmHg) | EmergencyService logs emergency alert automatically | Alert found in emergency_alerts table | **PASS** |
| **TC-48** | Service / Monitor | Moving Averages Calculation (7-day and 30-day) | 2 readings in 7 days, 1 reading 20 days ago | 7-day avg: 125.0 / 82.0; 30-day avg: 130.0 / 84.7 | Exact averages calculated | **PASS** |
| **TC-49** | Service / Monitor | Trajectory Trend Detection | Rising pulse (65, 70, 78, 85); Stable sugar (95, 96, 95, 94) | `Trend.RISING` for HR; `Trend.STABLE` for Sugar | RISING and STABLE confirmed | **PASS** |
| **TC-50** | Service / Monitor | Flagging Abnormal Readings | 2 normal readings + 2 abnormal readings | 2 abnormal records flagged; normal excluded | 2 abnormal records returned | **PASS** |
| **TC-51** | DAO / Crud | ElderDAO & CaregiverDAO Full CRUD | Add, getById, getAll, update, delete, search | All CRUD and search operations succeed | Records added, updated, deleted cleanly | **PASS** |
| **TC-52** | DAO / Crud | Polymorphic HealthRecordDAO Single-Table Persistence | Persist BP, Sugar, HR into `health_records` table | Retrieved objects cast to exact subclasses | `BloodPressureRecord`, `BloodSugarRecord`, `HeartRateRecord` instances verified | **PASS** |
| **TC-53** | DAO / Database | DatabaseManager Schema & Foreign Keys | Initialize schema on clean database | All 9 tables exist and empty database check passes | All 9 tables verified in `sqlite_master` | **PASS** |
