package com.eldercare.dao;

import com.eldercare.model.Medication;
import com.eldercare.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Data Access Object for {@link Medication} entities.
 */
public class MedicationDAO implements DAO<Medication> {

    private final DatabaseManager dbManager;

    public MedicationDAO() {
        this(DatabaseManager.getInstance());
    }

    public MedicationDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public int add(Medication med) throws SQLException {
        String sql = """
            INSERT INTO medications (elder_id, name, dosage, times_per_day, dose_times, start_date, end_date, instructions)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, med.getElderId());
            pstmt.setString(2, med.getName());
            pstmt.setString(3, med.getDosage());
            pstmt.setInt(4, med.getTimesPerDay());
            pstmt.setString(5, serializeDoseTimes(med.getDoseTimes()));
            pstmt.setString(6, DateUtil.formatDate(med.getStartDate()));
            pstmt.setString(7, DateUtil.formatDate(med.getEndDate()));
            pstmt.setString(8, med.getInstructions());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    med.setMedicationId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    @Override
    public Optional<Medication> getById(int id) throws SQLException {
        String sql = "SELECT * FROM medications WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMedication(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Medication> getAll() throws SQLException {
        List<Medication> list = new ArrayList<>();
        String sql = "SELECT * FROM medications ORDER BY name ASC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToMedication(rs));
            }
        }
        return list;
    }

    @Override
    public boolean update(Medication med) throws SQLException {
        String sql = """
            UPDATE medications
            SET elder_id = ?, name = ?, dosage = ?, times_per_day = ?,
                dose_times = ?, start_date = ?, end_date = ?, instructions = ?
            WHERE id = ?;
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, med.getElderId());
            pstmt.setString(2, med.getName());
            pstmt.setString(3, med.getDosage());
            pstmt.setInt(4, med.getTimesPerDay());
            pstmt.setString(5, serializeDoseTimes(med.getDoseTimes()));
            pstmt.setString(6, DateUtil.formatDate(med.getStartDate()));
            pstmt.setString(7, DateUtil.formatDate(med.getEndDate()));
            pstmt.setString(8, med.getInstructions());
            pstmt.setInt(9, med.getMedicationId());

            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM medications WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all medications prescribed for a given elder.
     *
     * @param elderId elder identifier
     * @return list of medications
     * @throws SQLException on database error
     */
    public List<Medication> getByElderId(int elderId) throws SQLException {
        List<Medication> list = new ArrayList<>();
        String sql = "SELECT * FROM medications WHERE elder_id = ? ORDER BY name ASC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, elderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMedication(rs));
                }
            }
        }
        return list;
    }

    private String serializeDoseTimes(List<LocalTime> doseTimes) {
        if (doseTimes == null || doseTimes.isEmpty()) {
            return "";
        }
        return doseTimes.stream()
                .map(DateUtil::formatTime)
                .collect(Collectors.joining(","));
    }

    private List<LocalTime> deserializeDoseTimes(String text) {
        List<LocalTime> list = new ArrayList<>();
        if (text != null && !text.trim().isEmpty()) {
            Arrays.stream(text.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(DateUtil::parseTime)
                    .forEach(list::add);
        }
        return list;
    }

    private Medication mapResultSetToMedication(ResultSet rs) throws SQLException {
        Medication med = new Medication();
        med.setMedicationId(rs.getInt("id"));
        med.setElderId(rs.getInt("elder_id"));
        med.setName(rs.getString("name"));
        med.setDosage(rs.getString("dosage"));
        med.setTimesPerDay(rs.getInt("times_per_day"));
        med.setDoseTimes(deserializeDoseTimes(rs.getString("dose_times")));
        med.setDateRange(DateUtil.parseDate(rs.getString("start_date")),
                         DateUtil.parseDate(rs.getString("end_date")));
        med.setInstructions(rs.getString("instructions"));
        return med;
    }
}
