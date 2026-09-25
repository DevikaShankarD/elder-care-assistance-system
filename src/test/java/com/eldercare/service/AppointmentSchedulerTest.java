package com.eldercare.service;

import com.eldercare.dao.AppointmentDAO;
import com.eldercare.dao.DatabaseManager;
import com.eldercare.dao.DoctorDAO;
import com.eldercare.dao.ElderDAO;
import com.eldercare.model.Appointment;
import com.eldercare.model.AppointmentStatus;
import com.eldercare.model.Doctor;
import com.eldercare.model.Elder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AppointmentScheduler: booking, conflict detection, past-date rejection,
 * rescheduling, cancellation, completion, and doctor schedule queries.
 */
public class AppointmentSchedulerTest {

    private static final String TEST_DB = "test_scheduler.db";
    private static final String TEST_JDBC = "jdbc:sqlite:" + TEST_DB;

    private DatabaseManager dbManager;
    private AppointmentScheduler scheduler;
    private ElderDAO elderDAO;
    private DoctorDAO doctorDAO;
    private AppointmentDAO appointmentDAO;

    private int testElderId;
    private int testDoctorId;
    private int otherElderId;
    private int otherDoctorId;

    @BeforeEach
    public void setUp() throws SQLException {
        DatabaseManager.resetInstance();
        new File(TEST_DB).delete();

        dbManager = DatabaseManager.getInstance(TEST_JDBC);
        elderDAO = new ElderDAO(dbManager);
        doctorDAO = new DoctorDAO(dbManager);
        appointmentDAO = new AppointmentDAO(dbManager);
        scheduler = new AppointmentScheduler(appointmentDAO, doctorDAO, elderDAO);

        testElderId = elderDAO.add(new Elder(0, "Test Elder 1", "9876543210", "Address 1", 75, "Male", "O+", "None"));
        otherElderId = elderDAO.add(new Elder(0, "Test Elder 2", "9876543211", "Address 2", 80, "Female", "A+", "None"));

        testDoctorId = doctorDAO.add(new Doctor(0, "Dr. Alice Smith", "Cardiology", "City Hospital", "9876543220"));
        otherDoctorId = doctorDAO.add(new Doctor(0, "Dr. Bob Jones", "Neurology", "Care Clinic", "9876543221"));
    }

    @AfterEach
    public void tearDown() {
        DatabaseManager.resetInstance();
        new File(TEST_DB).delete();
    }

    @Test
    @DisplayName("Normal booking succeeds for future slot")
    public void testBookAppointmentSuccess() throws SQLException {
        LocalDateTime futureTime = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        Appointment appt = scheduler.bookAppointment(testElderId, testDoctorId, futureTime, "Cardiac Consultation");

        assertNotNull(appt);
        assertTrue(appt.getAppointmentId() > 0);
        assertEquals(AppointmentStatus.SCHEDULED, appt.getStatus());
        assertEquals("Cardiac Consultation", appt.getPurpose());
    }

    @Test
    @DisplayName("Past date booking is rejected")
    public void testPastDateRejection() {
        LocalDateTime pastTime = LocalDateTime.now().minusHours(2);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                scheduler.bookAppointment(testElderId, testDoctorId, pastTime, "Past Checkup"));

        assertTrue(ex.getMessage().contains("Cannot schedule an appointment in the past"));
    }

    @Test
    @DisplayName("Doctor conflict detected when within 30 minutes")
    public void testDoctorConflictDetection() throws SQLException {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0);
        scheduler.bookAppointment(testElderId, testDoctorId, baseTime, "First Visit");

        // Try booking another elder with the SAME doctor at 14:15 (15 mins later, within 30 mins)
        LocalDateTime conflictTime = baseTime.plusMinutes(15);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                scheduler.bookAppointment(otherElderId, testDoctorId, conflictTime, "Second Visit"));

        assertTrue(ex.getMessage().contains("Doctor conflict"));
    }

    @Test
    @DisplayName("Elder conflict detected when within 30 minutes")
    public void testElderConflictDetection() throws SQLException {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(3).withHour(15).withMinute(0);
        scheduler.bookAppointment(testElderId, testDoctorId, baseTime, "Cardio check");

        // Try booking the SAME elder with a DIFFERENT doctor at 15:20 (20 mins later, within 30 mins)
        LocalDateTime conflictTime = baseTime.plusMinutes(20);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                scheduler.bookAppointment(testElderId, otherDoctorId, conflictTime, "Neuro check"));

        assertTrue(ex.getMessage().contains("Elder conflict"));
    }

    @Test
    @DisplayName("Booking exactly or more than 30 minutes apart succeeds")
    public void testBookingAllowedOutsideConflictWindow() throws SQLException {
        LocalDateTime baseTime = LocalDateTime.now().plusDays(4).withHour(10).withMinute(0);
        scheduler.bookAppointment(testElderId, testDoctorId, baseTime, "Morning slot");

        // Exactly 30 minutes later with same doctor is permitted
        LocalDateTime nextSlot = baseTime.plusMinutes(30);
        Appointment appt2 = scheduler.bookAppointment(otherElderId, testDoctorId, nextSlot, "Subsequent slot");
        assertNotNull(appt2);
        assertEquals(AppointmentStatus.SCHEDULED, appt2.getStatus());
    }

    @Test
    @DisplayName("Reschedule appointment successfully to new non-conflicting time")
    public void testRescheduleSuccess() throws SQLException {
        LocalDateTime time1 = LocalDateTime.now().plusDays(5).withHour(11).withMinute(0);
        Appointment appt = scheduler.bookAppointment(testElderId, testDoctorId, time1, "Initial");

        LocalDateTime time2 = time1.plusDays(1);
        Appointment rescheduled = scheduler.rescheduleAppointment(appt.getAppointmentId(), time2);

        assertEquals(time2, rescheduled.getDateTime());
        assertEquals(AppointmentStatus.SCHEDULED, rescheduled.getStatus());
    }

    @Test
    @DisplayName("Cancel appointment updates status to CANCELLED")
    public void testCancelAppointment() throws SQLException {
        LocalDateTime time = LocalDateTime.now().plusDays(6).withHour(9).withMinute(0);
        Appointment appt = scheduler.bookAppointment(testElderId, testDoctorId, time, "To be cancelled");

        boolean cancelled = scheduler.cancelAppointment(appt.getAppointmentId());
        assertTrue(cancelled);

        Appointment updated = appointmentDAO.getById(appt.getAppointmentId()).orElseThrow();
        assertEquals(AppointmentStatus.CANCELLED, updated.getStatus());
    }

    @Test
    @DisplayName("Complete appointment updates status to COMPLETED")
    public void testCompleteAppointment() throws SQLException {
        LocalDateTime time = LocalDateTime.now().plusDays(7).withHour(16).withMinute(0);
        Appointment appt = scheduler.bookAppointment(testElderId, testDoctorId, time, "Consultation");

        boolean completed = scheduler.completeAppointment(appt.getAppointmentId());
        assertTrue(completed);

        Appointment updated = appointmentDAO.getById(appt.getAppointmentId()).orElseThrow();
        assertEquals(AppointmentStatus.COMPLETED, updated.getStatus());
    }

    @Test
    @DisplayName("Retrieve Doctor daily schedule for a given date")
    public void testDoctorDailySchedule() throws SQLException {
        LocalDate date = LocalDate.now().plusDays(8);
        scheduler.bookAppointment(testElderId, testDoctorId, date.atTime(9, 0), "Slot 1");
        scheduler.bookAppointment(otherElderId, testDoctorId, date.atTime(10, 0), "Slot 2");

        List<Appointment> schedule = scheduler.getDoctorSchedule(testDoctorId, date);
        assertEquals(2, schedule.size());
        assertEquals(LocalTime.of(9, 0), schedule.get(0).getDateTime().toLocalTime());
        assertEquals(LocalTime.of(10, 0), schedule.get(1).getDateTime().toLocalTime());
    }
}
