package com.eldercare.dao;

import com.eldercare.model.EmergencyContact;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link EmergencyContact} entities.
 */
public class EmergencyContactDAO implements DAO<EmergencyContact> {

    private final DatabaseManager dbManager;

    public EmergencyContactDAO() {
        this(DatabaseManager.getInstance());
    }

    public EmergencyContactDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public int add(EmergencyContact contact) throws SQLException {
        String sql = """
            INSERT INTO emergency_contacts (elder_id, name, relationship, phone, priority)
            VALUES (?, ?, ?, ?, ?);
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, contact.getElderId());
            pstmt.setString(2, contact.getName());
            pstmt.setString(3, contact.getRelationship());
            pstmt.setString(4, contact.getPhone());
            pstmt.setInt(5, contact.getPriority());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    contact.setContactId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    @Override
    public Optional<EmergencyContact> getById(int id) throws SQLException {
        String sql = "SELECT * FROM emergency_contacts WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToContact(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<EmergencyContact> getAll() throws SQLException {
        List<EmergencyContact> list = new ArrayList<>();
        String sql = "SELECT * FROM emergency_contacts ORDER BY priority ASC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToContact(rs));
            }
        }
        return list;
    }

    @Override
    public boolean update(EmergencyContact contact) throws SQLException {
        String sql = """
            UPDATE emergency_contacts
            SET elder_id = ?, name = ?, relationship = ?, phone = ?, priority = ?
            WHERE id = ?;
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, contact.getElderId());
            pstmt.setString(2, contact.getName());
            pstmt.setString(3, contact.getRelationship());
            pstmt.setString(4, contact.getPhone());
            pstmt.setInt(5, contact.getPriority());
            pstmt.setInt(6, contact.getContactId());

            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM emergency_contacts WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all emergency contacts for a specific elder, ordered by priority ascending (1 = call first).
     *
     * @param elderId elder ID
     * @return sorted list of emergency contacts
     * @throws SQLException on database error
     */
    public List<EmergencyContact> getByElderId(int elderId) throws SQLException {
        List<EmergencyContact> list = new ArrayList<>();
        String sql = "SELECT * FROM emergency_contacts WHERE elder_id = ? ORDER BY priority ASC, name ASC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, elderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToContact(rs));
                }
            }
        }
        return list;
    }

    private EmergencyContact mapResultSetToContact(ResultSet rs) throws SQLException {
        EmergencyContact contact = new EmergencyContact();
        contact.setContactId(rs.getInt("id"));
        contact.setElderId(rs.getInt("elder_id"));
        contact.setName(rs.getString("name"));
        contact.setRelationship(rs.getString("relationship"));
        contact.setPhone(rs.getString("phone"));
        contact.setPriority(rs.getInt("priority"));
        return contact;
    }
}
