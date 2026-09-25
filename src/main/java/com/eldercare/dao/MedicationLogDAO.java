package com.eldercare.dao;

import com.eldercare.model.DoseStatus;
import com.eldercare.model.MedicationLog;
import com.eldercare.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link MedicationLog} entities.
 */
public class MedicationLogDAO implements DAO<MedicationLog> {

    private final DatabaseManager dbManager;

    public MedicationLogDAO() {
        this(DatabaseManager.getInstance());
    }

    public MedicationLogDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public int add(MedicationLog log) throws SQLException {
        String sql = """
            INSERT INTO medication_logs (medication_id, scheduled_time, status, logged_at)
            VALUES (?, ?, ?, ?);
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, log.getMedicationId());
            pstmt.setString(2, DateUtil.formatDateTime(log.getScheduledTime()));
            pstmt.setString(3, log.getStatus().name());
            pstmt.setString(4, DateUtil.formatDateTime(log.getLoggedAt()));

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    log.setLogId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    @Override
    public Optional<MedicationLog> getById(int id) throws SQLException {
        String sql = "SELECT * FROM medication_logs WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLog(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<MedicationLog> getAll() throws SQLException {
        List<MedicationLog> list = new ArrayList<>();
        String sql = "SELECT * FROM medication_logs ORDER BY scheduled_time DESC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToLog(rs));
            }
        }
        return list;
    }

    @Override
    public boolean update(MedicationLog log) throws SQLException {
        String sql = """
            UPDATE medication_logs
            SET medication_id = ?, scheduled_time = ?, status = ?, logged_at = ?
            WHERE id = ?;
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, log.getMedicationId());
            pstmt.setString(2, DateUtil.formatDateTime(log.getScheduledTime()));
            pstmt.setString(3, log.getStatus().name());
            pstmt.setString(4, DateUtil.formatDateTime(log.getLoggedAt()));
            pstmt.setInt(5, log.getLogId());

            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM medication_logs WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all adherence logs for a specific medication.
     *
     * @param medicationId medication ID
     * @return list of medication logs
     * @throws SQLException on database error
     */
    public List<MedicationLog> getByMedicationId(int medicationId) throws SQLException {
        List<MedicationLog> list = new ArrayList<>();
        String sql = "SELECT * FROM medication_logs WHERE medication_id = ? ORDER BY scheduled_time ASC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, medicationId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLog(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves a log for a medication at an exact scheduled time, if one exists.
     *
     * @param medicationId medication ID
     * @param scheduledTime scheduled time
     * @return Optional of MedicationLog
     * @throws SQLException on database error
     */
    public Optional<MedicationLog> getByMedicationAndScheduledTime(int medicationId, LocalDateTime scheduledTime) throws SQLException {
        String sql = "SELECT * FROM medication_logs WHERE medication_id = ? AND scheduled_time = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, medicationId);
            pstmt.setString(2, DateUtil.formatDateTime(scheduledTime));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLog(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Retrieves all logs for all medications belonging to a specific elder.
     *
     * @param elderId elder ID
     * @return list of logs
     * @throws SQLException on database error
     */
    public List<MedicationLog> getByElderId(int elderId) throws SQLException {
        List<MedicationLog> list = new ArrayList<>();
        String sql = """
            SELECT ml.* FROM medication_logs ml
            INNER JOIN medications m ON ml.medication_id = m.id
            WHERE m.elder_id = ?
            ORDER BY ml.scheduled_time DESC;
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, elderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLog(rs));
                }
            }
        }
        return list;
    }

    private MedicationLog mapResultSetToLog(ResultSet rs) throws SQLException {
        MedicationLog log = new MedicationLog();
        log.setLogId(rs.getInt("id"));
        log.setMedicationId(rs.getInt("medication_id"));
        log.setScheduledTime(DateUtil.parseDateTime(rs.getString("scheduled_time")));
        log.setStatus(DoseStatus.valueOf(rs.getString("status")));
        log.setLoggedAt(DateUtil.parseDateTime(rs.getString("logged_at")));
        return log;
    }
}
