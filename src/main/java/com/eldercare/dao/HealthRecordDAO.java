package com.eldercare.dao;

import com.eldercare.model.BloodPressureRecord;
import com.eldercare.model.BloodSugarRecord;
import com.eldercare.model.HealthRecord;
import com.eldercare.model.HeartRateRecord;
import com.eldercare.model.ReadingType;
import com.eldercare.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link HealthRecord} polymorphic entities.
 * Handles single-table inheritance persistence and polymorphic reconstruction of
 * {@link BloodPressureRecord}, {@link BloodSugarRecord}, and {@link HeartRateRecord}.
 */
public class HealthRecordDAO implements DAO<HealthRecord> {

    public static final String TYPE_BP = "BLOOD_PRESSURE";
    public static final String TYPE_SUGAR = "BLOOD_SUGAR";
    public static final String TYPE_HEART = "HEART_RATE";

    private final DatabaseManager dbManager;

    public HealthRecordDAO() {
        this(DatabaseManager.getInstance());
    }

    public HealthRecordDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public int add(HealthRecord record) throws SQLException {
        String sql = """
            INSERT INTO health_records (elder_id, record_type, recorded_at, notes, systolic, diastolic, sugar_value, sugar_type, heart_rate, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, record.getElderId());
            pstmt.setString(3, DateUtil.formatDateTime(record.getRecordedAt()));
            pstmt.setString(4, record.getNotes());
            pstmt.setString(10, record.evaluate().name());

            if (record instanceof BloodPressureRecord bp) {
                pstmt.setString(2, TYPE_BP);
                pstmt.setInt(5, bp.getSystolic());
                pstmt.setInt(6, bp.getDiastolic());
                pstmt.setNull(7, Types.REAL);
                pstmt.setNull(8, Types.VARCHAR);
                pstmt.setNull(9, Types.INTEGER);
            } else if (record instanceof BloodSugarRecord bs) {
                pstmt.setString(2, TYPE_SUGAR);
                pstmt.setNull(5, Types.INTEGER);
                pstmt.setNull(6, Types.INTEGER);
                pstmt.setDouble(7, bs.getValueMgDl());
                pstmt.setString(8, bs.getReadingType() != null ? bs.getReadingType().name() : ReadingType.RANDOM.name());
                pstmt.setNull(9, Types.INTEGER);
            } else if (record instanceof HeartRateRecord hr) {
                pstmt.setString(2, TYPE_HEART);
                pstmt.setNull(5, Types.INTEGER);
                pstmt.setNull(6, Types.INTEGER);
                pstmt.setNull(7, Types.REAL);
                pstmt.setNull(8, Types.VARCHAR);
                pstmt.setInt(9, hr.getBpm());
            } else {
                throw new IllegalArgumentException("Unsupported HealthRecord type: " + record.getClass().getName());
            }

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    record.setRecordId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    @Override
    public Optional<HealthRecord> getById(int id) throws SQLException {
        String sql = "SELECT * FROM health_records WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRecord(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<HealthRecord> getAll() throws SQLException {
        List<HealthRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM health_records ORDER BY recorded_at ASC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToRecord(rs));
            }
        }
        return list;
    }

    @Override
    public boolean update(HealthRecord record) throws SQLException {
        String sql = """
            UPDATE health_records
            SET elder_id = ?, record_type = ?, recorded_at = ?, notes = ?,
                systolic = ?, diastolic = ?, sugar_value = ?, sugar_type = ?,
                heart_rate = ?, status = ?
            WHERE id = ?;
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, record.getElderId());
            pstmt.setString(3, DateUtil.formatDateTime(record.getRecordedAt()));
            pstmt.setString(4, record.getNotes());
            pstmt.setString(10, record.evaluate().name());
            pstmt.setInt(11, record.getRecordId());

            if (record instanceof BloodPressureRecord bp) {
                pstmt.setString(2, TYPE_BP);
                pstmt.setInt(5, bp.getSystolic());
                pstmt.setInt(6, bp.getDiastolic());
                pstmt.setNull(7, Types.REAL);
                pstmt.setNull(8, Types.VARCHAR);
                pstmt.setNull(9, Types.INTEGER);
            } else if (record instanceof BloodSugarRecord bs) {
                pstmt.setString(2, TYPE_SUGAR);
                pstmt.setNull(5, Types.INTEGER);
                pstmt.setNull(6, Types.INTEGER);
                pstmt.setDouble(7, bs.getValueMgDl());
                pstmt.setString(8, bs.getReadingType() != null ? bs.getReadingType().name() : ReadingType.RANDOM.name());
                pstmt.setNull(9, Types.INTEGER);
            } else if (record instanceof HeartRateRecord hr) {
                pstmt.setString(2, TYPE_HEART);
                pstmt.setNull(5, Types.INTEGER);
                pstmt.setNull(6, Types.INTEGER);
                pstmt.setNull(7, Types.REAL);
                pstmt.setNull(8, Types.VARCHAR);
                pstmt.setInt(9, hr.getBpm());
            }

            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM health_records WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all health records for a specific elder, ordered chronologically.
     *
     * @param elderId elder ID
     * @return list of polymorphic health records
     * @throws SQLException on database error
     */
    public List<HealthRecord> getByElderId(int elderId) throws SQLException {
        List<HealthRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM health_records WHERE elder_id = ? ORDER BY recorded_at ASC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, elderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRecord(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves all health records of a given type for an elder.
     *
     * @param elderId elder ID
     * @param recordType TYPE_BP, TYPE_SUGAR, or TYPE_HEART
     * @return list of health records
     * @throws SQLException on database error
     */
    public List<HealthRecord> getByElderIdAndType(int elderId, String recordType) throws SQLException {
        List<HealthRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM health_records WHERE elder_id = ? AND record_type = ? ORDER BY recorded_at ASC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, elderId);
            pstmt.setString(2, recordType);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRecord(rs));
                }
            }
        }
        return list;
    }

    /**
     * Reconstructs the concrete HealthRecord subclass polymorphically from the ResultSet.
     */
    private HealthRecord mapResultSetToRecord(ResultSet rs) throws SQLException {
        String type = rs.getString("record_type");
        int recordId = rs.getInt("id");
        int elderId = rs.getInt("elder_id");
        var recordedAt = DateUtil.parseDateTime(rs.getString("recorded_at"));
        String notes = rs.getString("notes");

        if (TYPE_BP.equalsIgnoreCase(type)) {
            int systolic = rs.getInt("systolic");
            int diastolic = rs.getInt("diastolic");
            return new BloodPressureRecord(recordId, elderId, recordedAt, notes, systolic, diastolic);
        } else if (TYPE_SUGAR.equalsIgnoreCase(type)) {
            double sugarValue = rs.getDouble("sugar_value");
            String sugarTypeStr = rs.getString("sugar_type");
            ReadingType readingType = ReadingType.RANDOM;
            if (sugarTypeStr != null) {
                try {
                    readingType = ReadingType.valueOf(sugarTypeStr);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return new BloodSugarRecord(recordId, elderId, recordedAt, notes, sugarValue, readingType);
        } else if (TYPE_HEART.equalsIgnoreCase(type)) {
            int bpm = rs.getInt("heart_rate");
            return new HeartRateRecord(recordId, elderId, recordedAt, notes, bpm);
        } else {
            throw new IllegalStateException("Unknown record_type found in database: " + type);
        }
    }
}
