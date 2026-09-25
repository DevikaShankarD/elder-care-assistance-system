package com.eldercare.dao;

import com.eldercare.model.Appointment;
import com.eldercare.model.AppointmentStatus;
import com.eldercare.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link Appointment} entities.
 */
public class AppointmentDAO implements DAO<Appointment> {

    private final DatabaseManager dbManager;

    public AppointmentDAO() {
        this(DatabaseManager.getInstance());
    }

    public AppointmentDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public int add(Appointment appt) throws SQLException {
        String sql = """
            INSERT INTO appointments (elder_id, doctor_id, date_time, purpose, status)
            VALUES (?, ?, ?, ?, ?);
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, appt.getElderId());
            pstmt.setInt(2, appt.getDoctorId());
            pstmt.setString(3, DateUtil.formatDateTime(appt.getDateTime()));
            pstmt.setString(4, appt.getPurpose());
            pstmt.setString(5, appt.getStatus().name());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    appt.setAppointmentId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    @Override
    public Optional<Appointment> getById(int id) throws SQLException {
        String sql = "SELECT * FROM appointments WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAppointment(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Appointment> getAll() throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT * FROM appointments ORDER BY date_time ASC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToAppointment(rs));
            }
        }
        return list;
    }

    @Override
    public boolean update(Appointment appt) throws SQLException {
        String sql = """
            UPDATE appointments
            SET elder_id = ?, doctor_id = ?, date_time = ?, purpose = ?, status = ?
            WHERE id = ?;
        """;
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, appt.getElderId());
            pstmt.setInt(2, appt.getDoctorId());
            pstmt.setString(3, DateUtil.formatDateTime(appt.getDateTime()));
            pstmt.setString(4, appt.getPurpose());
            pstmt.setString(5, appt.getStatus().name());
            pstmt.setInt(6, appt.getAppointmentId());

            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM appointments WHERE id = ?;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all appointments for a specific elder.
     *
     * @param elderId elder ID
     * @return list of appointments
     * @throws SQLException on database error
     */
    public List<Appointment> getByElderId(int elderId) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT * FROM appointments WHERE elder_id = ? ORDER BY date_time ASC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, elderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAppointment(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves all appointments for a specific doctor.
     *
     * @param doctorId doctor ID
     * @return list of appointments
     * @throws SQLException on database error
     */
    public List<Appointment> getByDoctorId(int doctorId) throws SQLException {
        List<Appointment> list = new ArrayList<>();
        String sql = "SELECT * FROM appointments WHERE doctor_id = ? ORDER BY date_time ASC;";
        Connection conn = dbManager.getConnection();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, doctorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAppointment(rs));
                }
            }
        }
        return list;
    }

    private Appointment mapResultSetToAppointment(ResultSet rs) throws SQLException {
        Appointment appt = new Appointment();
        appt.setAppointmentId(rs.getInt("id"));
        appt.setElderId(rs.getInt("elder_id"));
        appt.setDoctorId(rs.getInt("doctor_id"));
        appt.setDateTime(DateUtil.parseDateTime(rs.getString("date_time")));
        appt.setPurpose(rs.getString("purpose"));
        appt.setStatus(AppointmentStatus.valueOf(rs.getString("status")));
        return appt;
    }
}
