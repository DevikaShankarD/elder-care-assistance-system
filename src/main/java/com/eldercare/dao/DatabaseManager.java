package com.eldercare.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton database manager handling SQLite database connection and schema lifecycle.
 * Ensures the database file and all required tables exist upon startup.
 */
public class DatabaseManager {

    public static final String DEFAULT_DB_FILE = "eldercare.db";
    public static final String DEFAULT_JDBC_URL = "jdbc:sqlite:" + DEFAULT_DB_FILE;

    private static volatile DatabaseManager instance;
    private final String jdbcUrl;
    private Connection connection;

    /**
     * Private constructor for Singleton pattern.
     *
     * @param jdbcUrl the SQLite JDBC connection URL
     */
    private DatabaseManager(String jdbcUrl) {
        this.jdbcUrl = (jdbcUrl != null && !jdbcUrl.trim().isEmpty()) ? jdbcUrl : DEFAULT_JDBC_URL;
        initializeSchema();
    }

    /**
     * Retrieves the Singleton instance using the default database (eldercare.db).
     *
     * @return DatabaseManager singleton instance
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager(DEFAULT_JDBC_URL);
        }
        return instance;
    }

    /**
     * Retrieves or initializes the Singleton instance with a specific JDBC URL.
     * Useful for isolated JUnit testing with test databases.
     *
     * @param customJdbcUrl custom connection string (e.g. "jdbc:sqlite:test_eldercare.db")
     * @return DatabaseManager instance
     */
    public static synchronized DatabaseManager getInstance(String customJdbcUrl) {
        if (instance == null || !instance.jdbcUrl.equals(customJdbcUrl)) {
            if (instance != null) {
                instance.closeConnection();
            }
            instance = new DatabaseManager(customJdbcUrl);
        }
        return instance;
    }

    /**
     * Resets the singleton instance and closes any open connection.
     * Intended for cleaning up between test suites.
     */
    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.closeConnection();
            instance = null;
        }
    }

    /**
     * Returns an active database connection, creating one if not currently open.
     *
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(jdbcUrl);
            // Enable Foreign Key support in SQLite
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        }
        return connection;
    }

    /**
     * Closes the underlying database connection.
     */
    public synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ignored) {
            } finally {
                connection = null;
            }
        }
    }

    /**
     * Creates all required tables if they do not already exist.
     * Keeps SQL simple, robust, and clean.
     */
    public synchronized void initializeSchema() {
        try {
            Connection conn = getConnection();
            try (Statement stmt = conn.createStatement()) {

                // 1. Elders table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS elders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        address TEXT,
                        age INTEGER NOT NULL,
                        gender TEXT,
                        blood_group TEXT,
                        medical_conditions TEXT
                    );
                """);

                // 2. Caregivers table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS caregivers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        address TEXT,
                        relationship_to_elder TEXT NOT NULL
                    );
                """);

                // 3. Doctors table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS doctors (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        specialization TEXT NOT NULL,
                        hospital TEXT NOT NULL,
                        phone TEXT NOT NULL
                    );
                """);

                // 4. Emergency Contacts table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS emergency_contacts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        elder_id INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        relationship TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        priority INTEGER NOT NULL,
                        FOREIGN KEY (elder_id) REFERENCES elders(id) ON DELETE CASCADE
                    );
                """);

                // 5. Medications table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS medications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        elder_id INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        dosage TEXT NOT NULL,
                        times_per_day INTEGER NOT NULL,
                        dose_times TEXT NOT NULL,
                        start_date TEXT NOT NULL,
                        end_date TEXT NOT NULL,
                        instructions TEXT,
                        FOREIGN KEY (elder_id) REFERENCES elders(id) ON DELETE CASCADE
                    );
                """);

                // 6. Medication Logs table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS medication_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        medication_id INTEGER NOT NULL,
                        scheduled_time TEXT NOT NULL,
                        status TEXT NOT NULL,
                        logged_at TEXT NOT NULL,
                        FOREIGN KEY (medication_id) REFERENCES medications(id) ON DELETE CASCADE
                    );
                """);

                // 7. Appointments table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS appointments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        elder_id INTEGER NOT NULL,
                        doctor_id INTEGER NOT NULL,
                        date_time TEXT NOT NULL,
                        purpose TEXT NOT NULL,
                        status TEXT NOT NULL,
                        FOREIGN KEY (elder_id) REFERENCES elders(id) ON DELETE CASCADE,
                        FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE
                    );
                """);

                // 8. Health Records table (Single Table Inheritance for records)
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS health_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        elder_id INTEGER NOT NULL,
                        record_type TEXT NOT NULL,
                        recorded_at TEXT NOT NULL,
                        notes TEXT,
                        systolic INTEGER,
                        diastolic INTEGER,
                        sugar_value REAL,
                        sugar_type TEXT,
                        heart_rate INTEGER,
                        status TEXT NOT NULL,
                        FOREIGN KEY (elder_id) REFERENCES elders(id) ON DELETE CASCADE
                    );
                """);

                // 9. Emergency Alerts table
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS emergency_alerts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        elder_id INTEGER NOT NULL,
                        timestamp TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        contacts_notified TEXT NOT NULL,
                        FOREIGN KEY (elder_id) REFERENCES elders(id) ON DELETE CASCADE
                    );
                """);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if the database is currently empty (contains no registered elders).
     * Used by the UI on startup to prompt for sample data seeding.
     *
     * @return true if elders table has 0 records, false otherwise
     */
    public boolean isDatabaseEmpty() {
        try {
            Connection conn = getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS count FROM elders")) {
                if (rs.next()) {
                    return rs.getInt("count") == 0;
                }
            }
        } catch (SQLException e) {
            return true;
        }
        return true;
    }

    /**
     * Clears all data from all tables and resets auto-increment sequences.
     */
    public synchronized void clearAllData() {
        try {
            Connection conn = getConnection();
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = OFF;");
                stmt.execute("DELETE FROM emergency_alerts;");
                stmt.execute("DELETE FROM health_records;");
                stmt.execute("DELETE FROM appointments;");
                stmt.execute("DELETE FROM medication_logs;");
                stmt.execute("DELETE FROM medications;");
                stmt.execute("DELETE FROM emergency_contacts;");
                stmt.execute("DELETE FROM caregivers;");
                stmt.execute("DELETE FROM doctors;");
                stmt.execute("DELETE FROM elders;");
                stmt.execute("DELETE FROM sqlite_sequence;");
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear database data: " + e.getMessage(), e);
        }
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }
}

