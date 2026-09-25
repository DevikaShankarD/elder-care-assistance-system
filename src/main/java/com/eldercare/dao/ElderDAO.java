package com.eldercare.dao;

import com.eldercare.model.Elder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link Elder} entities.
 * Implements standard CRUD operations using PreparedStatements.
 */
public class ElderDAO implements DAO<Elder> {

    private final DatabaseManager dbManager;

    public ElderDAO() {
        this(DatabaseManager.getInstance());
    }

    public ElderDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public int add(Elder elder) throws SQLException {
        String sql = """
            INSERT INTO elders (name, phone, address, age, gender, blood_group, medical_conditions)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """;

        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, elder.getName());
            pstmt.setString(2, elder.getPhone());
            pstmt.setString(3, elder.getAddress());
            pstmt.setInt(4, elder.getAge());
            pstmt.setString(5, elder.getGender());
            pstmt.setString(6, elder.getBloodGroup());
            pstmt.setString(7, elder.getMedicalConditions());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    elder.setId(generatedId);
                    return generatedId;
                }
            }
        }
        return -1;
    }

    @Override
    public Optional<Elder> getById(int id) throws SQLException {
        String sql = "SELECT * FROM elders WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToElder(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Elder> getAll() throws SQLException {
        List<Elder> list = new ArrayList<>();
        String sql = "SELECT * FROM elders ORDER BY name ASC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToElder(rs));
            }
        }
        return list;
    }

    @Override
    public boolean update(Elder elder) throws SQLException {
        String sql = """
            UPDATE elders
            SET name = ?, phone = ?, address = ?, age = ?, gender = ?, blood_group = ?, medical_conditions = ?
            WHERE id = ?;
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, elder.getName());
            pstmt.setString(2, elder.getPhone());
            pstmt.setString(3, elder.getAddress());
            pstmt.setInt(4, elder.getAge());
            pstmt.setString(5, elder.getGender());
            pstmt.setString(6, elder.getBloodGroup());
            pstmt.setString(7, elder.getMedicalConditions());
            pstmt.setInt(8, elder.getId());

            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM elders WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Searches elders by full or partial name (case-insensitive).
     *
     * @param keyword search keyword
     * @return matching list of elders
     * @throws SQLException on database error
     */
    public List<Elder> searchByName(String keyword) throws SQLException {
        List<Elder> list = new ArrayList<>();
        String sql = "SELECT * FROM elders WHERE LOWER(name) LIKE ? ORDER BY name ASC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword.toLowerCase().trim() + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToElder(rs));
                }
            }
        }
        return list;
    }

    private Elder mapResultSetToElder(ResultSet rs) throws SQLException {
        Elder elder = new Elder();
        elder.setId(rs.getInt("id"));
        elder.setName(rs.getString("name"));
        elder.setPhone(rs.getString("phone"));
        elder.setAddress(rs.getString("address"));
        elder.setAge(rs.getInt("age"));
        elder.setGender(rs.getString("gender"));
        elder.setBloodGroup(rs.getString("blood_group"));
        elder.setMedicalConditions(rs.getString("medical_conditions"));
        return elder;
    }
}
