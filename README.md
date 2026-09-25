# Elder Care Assistance System

An integrated Java application designed to support independent living for senior citizens. It provides automated medication reminders, vital health monitoring (Blood Pressure, Blood Sugar, Resting Heart Rate), 30-minute conflict-aware doctor appointment scheduling, emergency contact escalation, comprehensive health report generation, and an administrative oversight console.

---

## Key Features

- **OOP Architecture:** Demonstrates pure Encapsulation, Multi-level Inheritance (`Person` → `Elder`, `Caregiver`), Abstraction (`DAO<T>`, `HealthRecord`), and Dynamic Polymorphism (`HealthRecord.evaluate()`).
- **Permanent SQLite Storage:** All records, medication logs, appointments, alerts, and vitals persist to an embedded `eldercare.db` SQLite database with foreign keys and cascaded deletions.
- **Dual Interface:**
  - **Console UI (CLI):** Pure text-based interactive menu with robust input validation.
  - **Web Dashboard:** Clean browser-based interface running on Java 17's built-in `HttpServer` with role-based sign-in (Elder Portal and Admin Management Portal).
- **Admin Management Portal:** Manage elders, doctors, caregivers, emergency alerts, and appointments.
- **Automated Health Monitoring:** 7-day and 30-day moving averages, trend detection (improving/stable/worsening), and automated emergency escalation on critical vitals.
- **Safety Protection:** 30-minute buffer protection preventing double-booking appointments.

---

## Tech Stack

- **Language:** Java 17 (LTS)
- **Build Tool:** Apache Maven
- **Database:** SQLite 3 via JDBC (`org.xerial:sqlite-jdbc`)
- **Testing:** JUnit 5 (`org.junit.jupiter`) — 53 unit tests covering 100% of business rules

---

## Project Structure

```text
├── docs/
│   ├── DesignDocument.md      # Architecture, OOP principles & schema
│   ├── UserManual.md          # Step-by-step user guide
│   └── TestCases.md           # 53 test specifications & matrix
├── src/
│   ├── main/
│   │   ├── java/com/eldercare/
│   │   │   ├── dao/           # DatabaseManager and DAOs
│   │   │   ├── model/         # OOP domain entities & records
│   │   │   ├── service/       # Business logic & algorithms
│   │   │   ├── ui/            # Console menu & CLI Main
│   │   │   ├── util/          # Date utilities, sample data & inspector
│   │   │   └── web/           # Embedded HTTP server & REST handlers
│   │   └── resources/web/     # Clean frontend (HTML/CSS/JS)
│   └── test/java/com/eldercare/ # 53 JUnit 5 unit tests
├── eldercare.db               # SQLite database file (created on launch)
├── pom.xml                    # Maven configuration
├── run.bat                    # Launch Console CLI
├── run-web.bat                # Launch Web Server (http://localhost:8080)
├── test.bat                   # Execute JUnit 5 tests
└── view-db.bat                # Direct terminal database viewer
```

---

## How to Run

### 1. Web Application (Browser Dashboard)
Run the web batch script:
```cmd
run-web.bat
```
Then open: **`http://localhost:8080/`**
- **Admin Login:** Username `admin` | Password `admin`
- **Elder Login:** Select profile (e.g. *John Smith*) | PIN `1234`

### 2. Console Interface (CLI)
Run the console batch script:
```cmd
run.bat
```

### 3. Inspect SQLite Database
View all 9 database tables and records directly in your terminal:
```cmd
view-db.bat
```

### 4. Run Unit Tests
Execute the full test suite (53 tests):
```cmd
test.bat
```
