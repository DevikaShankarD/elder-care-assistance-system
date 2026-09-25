package com.eldercare.util;

import com.eldercare.dao.DatabaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

/**
 * Utility tool to inspect and print all SQLite database tables, row counts,
 * and contents directly from the command line.
 */
public class DatabaseViewer {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("            ELDER CARE ASSISTANCE SYSTEM - DATABASE INSPECTOR                   ");
        System.out.println("            Database File: " + DatabaseManager.DEFAULT_DB_FILE);
        System.out.println("================================================================================");

        String[] tables = {
                "elders",
                "doctors",
                "caregivers",
                "emergency_contacts",
                "medications",
                "medication_logs",
                "appointments",
                "health_records",
                "emergency_alerts"
        };

        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            try (Statement stmt = conn.createStatement()) {
                for (String table : tables) {
                    System.out.println("\n--------------------------------------------------------------------------------");
                    System.out.println(" TABLE: " + table.toUpperCase());
                    System.out.println("--------------------------------------------------------------------------------");

                    try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {
                        ResultSetMetaData meta = rs.getMetaData();
                        int colCount = meta.getColumnCount();

                        // Header
                        StringBuilder header = new StringBuilder();
                        for (int i = 1; i <= colCount; i++) {
                            header.append(String.format("%-18s ", meta.getColumnName(i)));
                        }
                        System.out.println(header);
                        System.out.println("-".repeat(Math.min(header.length(), 100)));

                        int rows = 0;
                        while (rs.next()) {
                            rows++;
                            StringBuilder row = new StringBuilder();
                            for (int i = 1; i <= colCount; i++) {
                                String val = rs.getString(i);
                                if (val == null) val = "NULL";
                                if (val.length() > 17) val = val.substring(0, 14) + "...";
                                row.append(String.format("%-18s ", val));
                            }
                            System.out.println(row);
                        }
                        if (rows == 0) {
                            System.out.println("  (No rows found)");
                        }
                        System.out.println("Total records: " + rows);
                    }
                }
            }
            System.out.println("\n================================================================================");
            System.out.println(" Database inspection complete.");
            System.out.println("================================================================================");
        } catch (Exception e) {
            System.err.println("Error inspecting database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
