package com.eldercare.dao;

import com.eldercare.model.Caregiver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link Caregiver} entities.
 */
public class CaregiverDAO implements DAO<Caregiver> {

    private final DatabaseManager dbManager;

    public CaregiverDAO() {
        this(DatabaseManager.getInstance());
    }

    public CaregiverDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public int add(Caregiver caregiver) throws SQLException {
        String sql = """
            INSERT INTO caregivers (name, phone, address, relationship_to_elder)
            VALUES (?, ?, ?, ?);
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, caregiver.getName());
            pstmt.setString(2, caregiver.getPhone());
            pstmt.setString(3, caregiver.getAddress());
            pstmt.setString(4, caregiver.getRelationshipToElder());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    caregiver.setId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    @Override
    public Optional<Caregiver> getById(int id) throws SQLException {
        String sql = "SELECT * FROM caregivers WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCaregiver(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Caregiver> getAll() throws SQLException {
        List<Caregiver> list = new ArrayList<>();
        String sql = "SELECT * FROM caregivers ORDER BY name ASC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToCaregiver(rs));
            }
        }
        return list;
    }

    @Override
    public boolean update(Caregiver caregiver) throws SQLException {
        String sql = """
            UPDATE caregivers
            SET name = ?, phone = ?, address = ?, relationship_to_elder = ?
            WHERE id = ?;
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, caregiver.getName());
            pstmt.setString(2, caregiver.getPhone());
            pstmt.setString(3, caregiver.getAddress());
            pstmt.setString(4, caregiver.getRelationshipToElder());
            pstmt.setInt(5, caregiver.getId());

            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM caregivers WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private Caregiver mapResultSetToCaregiver(ResultSet rs) throws SQLException {
        Caregiver caregiver = new Caregiver();
        caregiver.setId(rs.getInt("id"));
        caregiver.setName(rs.getString("name"));
        caregiver.setPhone(rs.getString("phone"));
        caregiver.setAddress(rs.getString("address"));
        caregiver.setRelationshipToElder(rs.getString("relationship_to_elder"));
        return caregiver;
    }
}
