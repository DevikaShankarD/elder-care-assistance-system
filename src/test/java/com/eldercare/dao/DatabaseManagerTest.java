package com.eldercare.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DatabaseManager: singleton behavior, connection handling, and schema creation.
 */
public class DatabaseManagerTest {

    private static final String TEST_DB_FILE = "test_phase1_eldercare.db";
    private static final String TEST_JDBC_URL = "jdbc:sqlite:" + TEST_DB_FILE;

    @BeforeEach
    public void setUp() {
        DatabaseManager.resetInstance();
        File dbFile = new File(TEST_DB_FILE);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @AfterEach
    public void tearDown() {
        DatabaseManager.resetInstance();
        File dbFile = new File(TEST_DB_FILE);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test
    public void testSchemaCreationAndTablesExist() throws SQLException {
        DatabaseManager dbManager = DatabaseManager.getInstance(TEST_JDBC_URL);
        assertNotNull(dbManager);

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {

            Set<String> tableNames = new HashSet<>();
            while (rs.next()) {
                tableNames.add(rs.getString("name"));
            }

            assertTrue(tableNames.contains("elders"), "elders table must exist");
            assertTrue(tableNames.contains("caregivers"), "caregivers table must exist");
            assertTrue(tableNames.contains("doctors"), "doctors table must exist");
            assertTrue(tableNames.contains("emergency_contacts"), "emergency_contacts table must exist");
            assertTrue(tableNames.contains("medications"), "medications table must exist");
            assertTrue(tableNames.contains("medication_logs"), "medication_logs table must exist");
            assertTrue(tableNames.contains("appointments"), "appointments table must exist");
            assertTrue(tableNames.contains("health_records"), "health_records table must exist");
            assertTrue(tableNames.contains("emergency_alerts"), "emergency_alerts table must exist");
        }
    }

    @Test
    public void testIsDatabaseEmptyOnFreshDb() {
        DatabaseManager dbManager = DatabaseManager.getInstance(TEST_JDBC_URL);
        assertTrue(dbManager.isDatabaseEmpty(), "Fresh database should be empty");
    }
}
