package com.eldercare.dao;

import com.eldercare.model.EmergencyAlert;
import com.eldercare.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link EmergencyAlert} entities.
 */
public class EmergencyAlertDAO implements DAO<EmergencyAlert> {

    private final DatabaseManager dbManager;

    public EmergencyAlertDAO() {
        this(DatabaseManager.getInstance());
    }

    public EmergencyAlertDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public int add(EmergencyAlert alert) throws SQLException {
        String sql = """
            INSERT INTO emergency_alerts (elder_id, timestamp, reason, contacts_notified)
            VALUES (?, ?, ?, ?);
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, alert.getElderId());
            pstmt.setString(2, DateUtil.formatDateTime(alert.getTimestamp()));
            pstmt.setString(3, alert.getReason());
            pstmt.setString(4, alert.getContactsNotified());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    alert.setAlertId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    @Override
    public Optional<EmergencyAlert> getById(int id) throws SQLException {
        String sql = "SELECT * FROM emergency_alerts WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAlert(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<EmergencyAlert> getAll() throws SQLException {
        List<EmergencyAlert> list = new ArrayList<>();
        String sql = "SELECT * FROM emergency_alerts ORDER BY timestamp DESC, id DESC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToAlert(rs));
            }
        }
        return list;
    }

    @Override
    public boolean update(EmergencyAlert alert) throws SQLException {
        String sql = """
            UPDATE emergency_alerts
            SET elder_id = ?, timestamp = ?, reason = ?, contacts_notified = ?
            WHERE id = ?;
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, alert.getElderId());
            pstmt.setString(2, DateUtil.formatDateTime(alert.getTimestamp()));
            pstmt.setString(3, alert.getReason());
            pstmt.setString(4, alert.getContactsNotified());
            pstmt.setInt(5, alert.getAlertId());

            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM emergency_alerts WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all emergency alerts recorded for a specific elder.
     *
     * @param elderId elder identifier
     * @return list of emergency alerts, latest first
     * @throws SQLException on database error
     */
    public List<EmergencyAlert> getByElderId(int elderId) throws SQLException {
        List<EmergencyAlert> list = new ArrayList<>();
        String sql = "SELECT * FROM emergency_alerts WHERE elder_id = ? ORDER BY timestamp DESC, id DESC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, elderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAlert(rs));
                }
            }
        }
        return list;
    }

    private EmergencyAlert mapResultSetToAlert(ResultSet rs) throws SQLException {
        EmergencyAlert alert = new EmergencyAlert();
        alert.setAlertId(rs.getInt("id"));
        alert.setElderId(rs.getInt("elder_id"));
        alert.setTimestamp(DateUtil.parseDateTime(rs.getString("timestamp")));
        alert.setReason(rs.getString("reason"));
        alert.setContactsNotified(rs.getString("contacts_notified"));
        return alert;
    }
}
