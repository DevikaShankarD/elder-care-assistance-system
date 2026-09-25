package com.eldercare.web;

import com.eldercare.dao.*;
import com.eldercare.model.*;
import com.eldercare.service.*;
import com.eldercare.util.DateUtil;
import com.eldercare.util.SampleDataLoader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Embedded HTTP Web Server for the Elder Care Assistance System.
 * Uses Java 17's built-in {@link com.sun.net.httpserver.HttpServer} (no external web container required).
 * Provides REST API endpoints and serves the modern Web Dashboard on http://localhost:8080.
 */
public class ElderCareWebServer {

    public static final int DEFAULT_PORT = 8080;

    private final int port;
    private HttpServer server;
    private final DatabaseManager dbManager;

    private final ElderDAO elderDAO;
    private final CaregiverDAO caregiverDAO;
    private final DoctorDAO doctorDAO;
    private final EmergencyContactDAO contactDAO;
    private final MedicationDAO medicationDAO;
    private final MedicationLogDAO medicationLogDAO;
    private final AppointmentDAO appointmentDAO;
    private final HealthRecordDAO healthRecordDAO;
    private final EmergencyAlertDAO alertDAO;

    private final ReminderService reminderService;
    private final AppointmentScheduler appointmentScheduler;
    private final EmergencyService emergencyService;
    private final HealthMonitor healthMonitor;
    private final ReportGenerator reportGenerator;

    public ElderCareWebServer() {
        this(DEFAULT_PORT);
    }

    public ElderCareWebServer(int port) {
        this.port = port;
        this.dbManager = DatabaseManager.getInstance();

        this.elderDAO = new ElderDAO(dbManager);
        this.caregiverDAO = new CaregiverDAO(dbManager);
        this.doctorDAO = new DoctorDAO(dbManager);
        this.contactDAO = new EmergencyContactDAO(dbManager);
        this.medicationDAO = new MedicationDAO(dbManager);
        this.medicationLogDAO = new MedicationLogDAO(dbManager);
        this.appointmentDAO = new AppointmentDAO(dbManager);
        this.healthRecordDAO = new HealthRecordDAO(dbManager);
        this.alertDAO = new EmergencyAlertDAO(dbManager);

        this.reminderService = new ReminderService(medicationDAO, medicationLogDAO);
        this.appointmentScheduler = new AppointmentScheduler(appointmentDAO, doctorDAO, elderDAO);
        this.emergencyService = new EmergencyService(alertDAO, elderDAO, contactDAO, doctorDAO, appointmentDAO);
        this.healthMonitor = new HealthMonitor(healthRecordDAO, emergencyService);
        this.reportGenerator = new ReportGenerator(elderDAO, healthRecordDAO, medicationDAO,
                medicationLogDAO, appointmentDAO, doctorDAO, alertDAO, healthMonitor, reminderService);
    }

    /**
     * Starts the HTTP server and registers API route handlers.
     *
     * @throws IOException on network binding error
     */
    public void start() throws IOException {
        // Auto-seed sample data on first run if database is empty
        if (dbManager.isDatabaseEmpty()) {
            try {
                new SampleDataLoader(dbManager).loadSampleData();
                System.out.println("[Web Server] Database was empty. Auto-seeded initial sample data.");
            } catch (SQLException e) {
                System.err.println("[Web Server] Failed to auto-seed: " + e.getMessage());
            }
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Static files (Web Dashboard)
        server.createContext("/", new StaticFileHandler());

        // REST API Endpoints
        server.createContext("/api/elders", new EldersHandler());
        server.createContext("/api/doctors", new DoctorsHandler());
        server.createContext("/api/caregivers", new CaregiversHandler());
        server.createContext("/api/admin/overview", new AdminOverviewHandler());
        server.createContext("/api/dashboard", new DashboardHandler());
        server.createContext("/api/health-reading", new HealthReadingHandler());
        server.createContext("/api/sos", new SosHandler());
        server.createContext("/api/dose-log", new DoseLogHandler());
        server.createContext("/api/appointment", new AppointmentHandler());
        server.createContext("/api/report", new ReportHandler());
        server.createContext("/api/seed", new SeedHandler());
        server.createContext("/api/reset-data", new ResetDataHandler());

        server.setExecutor(null); // Default single-thread executor
        server.start();

        System.out.println("================================================================================");
        System.out.println("          ELDER CARE ASSISTANCE SYSTEM - WEB DASHBOARD SERVER                   ");
        System.out.println("================================================================================");
        System.out.printf("  Server running at: http://localhost:%d/\n", port);
        System.out.println("  Open the URL above in your web browser to view the interactive frontend!");
        System.out.println("================================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public static void main(String[] args) {
        try {
            ElderCareWebServer webServer = new ElderCareWebServer(DEFAULT_PORT);
            webServer.start();

            // Try opening default browser on Windows
            try {
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler http://localhost:" + DEFAULT_PORT);
                }
            } catch (Exception ignored) {
            }

            System.out.println("Press Ctrl+C in this terminal to shut down the web server.");
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to start web server on port " + DEFAULT_PORT + ": " + e.getMessage());
        }
    }

    // =========================================================================
    // HANDLERS
    // =========================================================================

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html")) {
                serveResource(exchange, "/web/index.html", "text/html; charset=UTF-8");
            } else if (path.endsWith(".css")) {
                serveResource(exchange, "/web" + path, "text/css");
            } else if (path.endsWith(".js")) {
                serveResource(exchange, "/web" + path, "application/javascript");
            } else {
                sendResponse(exchange, 404, "Not Found", "text/plain");
            }
        }

        private void serveResource(HttpExchange exchange, String resourcePath, String contentType) throws IOException {
            // First check local disk directory (for development)
            File localFile = new File("src/main/resources" + resourcePath);
            if (!localFile.exists()) {
                localFile = new File("." + resourcePath);
            }

            byte[] bytes;
            if (localFile.exists() && localFile.isFile()) {
                bytes = java.nio.file.Files.readAllBytes(localFile.toPath());
            } else {
                try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                    if (is == null) {
                        sendResponse(exchange, 404, "File not found: " + resourcePath, "text/plain");
                        return;
                    }
                    bytes = is.readAllBytes();
                }
            }
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private class EldersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    List<Elder> list = elderDAO.getAll();
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        Elder e = list.get(i);
                        if (i > 0) json.append(",");
                        json.append(String.format("{\"id\":%d,\"name\":\"%s\",\"age\":%d,\"gender\":\"%s\",\"bloodGroup\":\"%s\",\"phone\":\"%s\",\"address\":\"%s\",\"conditions\":\"%s\"}",
                                e.getId(), escapeJson(e.getName()), e.getAge(), escapeJson(e.getGender()),
                                escapeJson(e.getBloodGroup()), escapeJson(e.getPhone()),
                                escapeJson(e.getAddress()), escapeJson(e.getMedicalConditions())));
                    }
                    json.append("]");
                    sendResponse(exchange, 200, json.toString(), "application/json");

                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map<String, String> params = parseRequestBody(exchange);
                    Elder elder = new Elder(0,
                            params.get("name"),
                            params.get("phone"),
                            params.get("address"),
                            Integer.parseInt(params.get("age")),
                            params.get("gender"),
                            params.get("bloodGroup"),
                            params.get("medicalConditions"));
                    int id = elderDAO.add(elder);
                    sendResponse(exchange, 200, "{\"success\":true,\"id\":" + id + "}", "application/json");

                } else if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                    int id = Integer.parseInt(query.get("id"));
                    boolean ok = elderDAO.delete(id);
                    sendResponse(exchange, 200, "{\"success\":" + ok + "}", "application/json");
                }
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class DoctorsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    List<Doctor> list = doctorDAO.getAll();
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        Doctor d = list.get(i);
                        if (i > 0) json.append(",");
                        json.append(String.format("{\"id\":%d,\"name\":\"%s\",\"specialization\":\"%s\",\"hospital\":\"%s\",\"phone\":\"%s\"}",
                                d.getDoctorId(), escapeJson(d.getName()), escapeJson(d.getSpecialization()),
                                escapeJson(d.getHospital()), escapeJson(d.getPhone())));
                    }
                    json.append("]");
                    sendResponse(exchange, 200, json.toString(), "application/json");

                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map<String, String> params = parseRequestBody(exchange);
                    Doctor doctor = new Doctor(0,
                            params.get("name"),
                            params.get("specialization"),
                            params.get("hospital"),
                            params.get("phone"));
                    int id = doctorDAO.add(doctor);
                    sendResponse(exchange, 200, "{\"success\":true,\"id\":" + id + "}", "application/json");

                } else if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                    int id = Integer.parseInt(query.get("id"));
                    boolean ok = doctorDAO.delete(id);
                    sendResponse(exchange, 200, "{\"success\":" + ok + "}", "application/json");
                }
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class CaregiversHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    List<Caregiver> list = caregiverDAO.getAll();
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        Caregiver c = list.get(i);
                        if (i > 0) json.append(",");
                        json.append(String.format("{\"id\":%d,\"name\":\"%s\",\"phone\":\"%s\",\"address\":\"%s\",\"relationship\":\"%s\"}",
                                c.getId(), escapeJson(c.getName()), escapeJson(c.getPhone()),
                                escapeJson(c.getAddress()), escapeJson(c.getRelationshipToElder())));
                    }
                    json.append("]");
                    sendResponse(exchange, 200, json.toString(), "application/json");

                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map<String, String> params = parseRequestBody(exchange);
                    Caregiver caregiver = new Caregiver(0,
                            params.get("name"),
                            params.get("phone"),
                            params.get("address"),
                            params.get("relationship"));
                    int id = caregiverDAO.add(caregiver);
                    sendResponse(exchange, 200, "{\"success\":true,\"id\":" + id + "}", "application/json");

                } else if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                    int id = Integer.parseInt(query.get("id"));
                    boolean ok = caregiverDAO.delete(id);
                    sendResponse(exchange, 200, "{\"success\":" + ok + "}", "application/json");
                }
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class AdminOverviewHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                List<Elder> elders = elderDAO.getAll();
                List<Doctor> doctors = doctorDAO.getAll();
                List<Caregiver> caregivers = caregiverDAO.getAll();
                List<EmergencyAlert> alerts = alertDAO.getAll();
                List<Appointment> appointments = appointmentDAO.getAll();

                Map<Integer, String> elderNames = elders.stream().collect(Collectors.toMap(Elder::getId, Elder::getName, (a, b) -> a));
                Map<Integer, Doctor> doctorMap = doctors.stream().collect(Collectors.toMap(Doctor::getDoctorId, d -> d, (a, b) -> a));

                StringBuilder sb = new StringBuilder("{");
                sb.append(String.format("\"stats\":{\"elders\":%d,\"doctors\":%d,\"caregivers\":%d,\"alerts\":%d,\"appointments\":%d},",
                        elders.size(), doctors.size(), caregivers.size(), alerts.size(), appointments.size()));

                // Elders
                sb.append("\"elders\":[");
                for (int i = 0; i < elders.size(); i++) {
                    Elder e = elders.get(i);
                    if (i > 0) sb.append(",");
                    sb.append(String.format("{\"id\":%d,\"name\":\"%s\",\"age\":%d,\"gender\":\"%s\",\"bloodGroup\":\"%s\",\"phone\":\"%s\",\"address\":\"%s\",\"conditions\":\"%s\"}",
                            e.getId(), escapeJson(e.getName()), e.getAge(), escapeJson(e.getGender()),
                            escapeJson(e.getBloodGroup()), escapeJson(e.getPhone()),
                            escapeJson(e.getAddress()), escapeJson(e.getMedicalConditions())));
                }
                sb.append("],");

                // Doctors
                sb.append("\"doctors\":[");
                for (int i = 0; i < doctors.size(); i++) {
                    Doctor d = doctors.get(i);
                    if (i > 0) sb.append(",");
                    sb.append(String.format("{\"id\":%d,\"name\":\"%s\",\"specialization\":\"%s\",\"hospital\":\"%s\",\"phone\":\"%s\"}",
                            d.getDoctorId(), escapeJson(d.getName()), escapeJson(d.getSpecialization()),
                            escapeJson(d.getHospital()), escapeJson(d.getPhone())));
                }
                sb.append("],");

                // Caregivers
                sb.append("\"caregivers\":[");
                for (int i = 0; i < caregivers.size(); i++) {
                    Caregiver c = caregivers.get(i);
                    if (i > 0) sb.append(",");
                    sb.append(String.format("{\"id\":%d,\"name\":\"%s\",\"phone\":\"%s\",\"address\":\"%s\",\"relationship\":\"%s\"}",
                            c.getId(), escapeJson(c.getName()), escapeJson(c.getPhone()),
                            escapeJson(c.getAddress()), escapeJson(c.getRelationshipToElder())));
                }
                sb.append("],");

                // Alerts
                sb.append("\"alerts\":[");
                for (int i = 0; i < alerts.size(); i++) {
                    EmergencyAlert a = alerts.get(i);
                    if (i > 0) sb.append(",");
                    String elderName = elderNames.getOrDefault(a.getElderId(), "Elder #" + a.getElderId());
                    sb.append(String.format("{\"id\":%d,\"elderId\":%d,\"elderName\":\"%s\",\"time\":\"%s\",\"reason\":\"%s\",\"notified\":\"%s\"}",
                            a.getAlertId(), a.getElderId(), escapeJson(elderName),
                            DateUtil.formatDateTime(a.getTimestamp()), escapeJson(a.getReason()), escapeJson(a.getContactsNotified())));
                }
                sb.append("],");

                // Appointments
                sb.append("\"appointments\":[");
                for (int i = 0; i < appointments.size(); i++) {
                    Appointment ap = appointments.get(i);
                    if (i > 0) sb.append(",");
                    String elderName = elderNames.getOrDefault(ap.getElderId(), "Elder #" + ap.getElderId());
                    Doctor doc = doctorMap.get(ap.getDoctorId());
                    String docName = (doc != null) ? doc.getName() : "Doctor #" + ap.getDoctorId();
                    String hospital = (doc != null) ? doc.getHospital() : "";
                    sb.append(String.format("{\"id\":%d,\"elderId\":%d,\"elderName\":\"%s\",\"time\":\"%s\",\"doctor\":\"%s\",\"hospital\":\"%s\",\"purpose\":\"%s\",\"status\":\"%s\"}",
                            ap.getAppointmentId(), ap.getElderId(), escapeJson(elderName),
                            DateUtil.formatDateTime(ap.getDateTime()), escapeJson(docName),
                            escapeJson(hospital), escapeJson(ap.getPurpose()), ap.getStatus().name()));
                }
                sb.append("]");

                sb.append("}");
                sendResponse(exchange, 200, sb.toString(), "application/json");

            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            try {
                Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                int elderId = query.containsKey("elderId") ? Integer.parseInt(query.get("elderId")) : 1;

                Optional<Elder> elderOpt = elderDAO.getById(elderId);
                if (elderOpt.isEmpty()) {
                    sendResponse(exchange, 404, "{\"error\":\"Elder not found\"}", "application/json");
                    return;
                }
                Elder elder = elderOpt.get();

                // 1. Emergency contacts
                List<EmergencyContact> contacts = contactDAO.getByElderId(elderId);

                // 2. Health averages & trends
                HealthAverage bpAvg7 = healthMonitor.get7DayAverage(elderId, HealthRecordDAO.TYPE_BP);
                HealthAverage bpAvg30 = healthMonitor.get30DayAverage(elderId, HealthRecordDAO.TYPE_BP);
                Trend bpTrend = healthMonitor.detectTrend(elderId, HealthRecordDAO.TYPE_BP);

                HealthAverage sugarAvg7 = healthMonitor.get7DayAverage(elderId, HealthRecordDAO.TYPE_SUGAR);
                HealthAverage sugarAvg30 = healthMonitor.get30DayAverage(elderId, HealthRecordDAO.TYPE_SUGAR);
                Trend sugarTrend = healthMonitor.detectTrend(elderId, HealthRecordDAO.TYPE_SUGAR);

                HealthAverage hrAvg7 = healthMonitor.get7DayAverage(elderId, HealthRecordDAO.TYPE_HEART);
                HealthAverage hrAvg30 = healthMonitor.get30DayAverage(elderId, HealthRecordDAO.TYPE_HEART);
                Trend hrTrend = healthMonitor.detectTrend(elderId, HealthRecordDAO.TYPE_HEART);

                // 3. Recent health records
                List<HealthRecord> readings = healthMonitor.getAllReadings(elderId);

                // 4. Today's medication schedule & overall adherence
                List<DoseScheduleItem> todaySchedule = reminderService.getTodaySchedule(elderId, LocalDate.now());
                double adherence = reminderService.calculateElderOverallAdherence(elderId);

                // 5. Appointments
                List<Appointment> upcoming = appointmentScheduler.getUpcomingAppointmentsForElder(elderId);

                // 6. Recent Alerts
                List<EmergencyAlert> alerts = emergencyService.getAlertHistory(elderId);

                // Build JSON response
                StringBuilder sb = new StringBuilder();
                sb.append("{");
                sb.append(String.format("\"elder\":{\"id\":%d,\"name\":\"%s\",\"age\":%d,\"gender\":\"%s\",\"bloodGroup\":\"%s\",\"phone\":\"%s\",\"address\":\"%s\",\"conditions\":\"%s\"},",
                        elder.getId(), escapeJson(elder.getName()), elder.getAge(), escapeJson(elder.getGender()),
                        escapeJson(elder.getBloodGroup()), escapeJson(elder.getPhone()),
                        escapeJson(elder.getAddress()), escapeJson(elder.getMedicalConditions())));

                // Contacts
                sb.append("\"contacts\":[");
                for (int i = 0; i < contacts.size(); i++) {
                    EmergencyContact c = contacts.get(i);
                    if (i > 0) sb.append(",");
                    sb.append(String.format("{\"id\":%d,\"name\":\"%s\",\"relationship\":\"%s\",\"phone\":\"%s\",\"priority\":%d}",
                            c.getContactId(), escapeJson(c.getName()), escapeJson(c.getRelationship()), escapeJson(c.getPhone()), c.getPriority()));
                }
                sb.append("],");

                // Metrics
                sb.append(String.format("\"metrics\":{" +
                                "\"adherence\":%.1f," +
                                "\"bp\":{\"avg7Sys\":%s,\"avg7Dia\":%s,\"avg30Sys\":%s,\"avg30Dia\":%s,\"trend\":\"%s\"}," +
                                "\"sugar\":{\"avg7\":%s,\"avg30\":%s,\"trend\":\"%s\"}," +
                                "\"heart\":{\"avg7\":%s,\"avg30\":%s,\"trend\":\"%s\"}" +
                                "},",
                        adherence,
                        bpAvg7.getPrimaryAverage() != null ? String.format("%.1f", bpAvg7.getPrimaryAverage()) : "null",
                        bpAvg7.getSecondaryAverage() != null ? String.format("%.1f", bpAvg7.getSecondaryAverage()) : "null",
                        bpAvg30.getPrimaryAverage() != null ? String.format("%.1f", bpAvg30.getPrimaryAverage()) : "null",
                        bpAvg30.getSecondaryAverage() != null ? String.format("%.1f", bpAvg30.getSecondaryAverage()) : "null",
                        bpTrend.getDescription(),
                        sugarAvg7.getPrimaryAverage() != null ? String.format("%.1f", sugarAvg7.getPrimaryAverage()) : "null",
                        sugarAvg30.getPrimaryAverage() != null ? String.format("%.1f", sugarAvg30.getPrimaryAverage()) : "null",
                        sugarTrend.getDescription(),
                        hrAvg7.getPrimaryAverage() != null ? String.format("%.1f", hrAvg7.getPrimaryAverage()) : "null",
                        hrAvg30.getPrimaryAverage() != null ? String.format("%.1f", hrAvg30.getPrimaryAverage()) : "null",
                        hrTrend.getDescription()
                ));

                // Schedule
                sb.append("\"todaySchedule\":[");
                for (int i = 0; i < todaySchedule.size(); i++) {
                    DoseScheduleItem d = todaySchedule.get(i);
                    if (i > 0) sb.append(",");
                    sb.append(String.format("{\"medicationId\":%d,\"name\":\"%s\",\"dosage\":\"%s\",\"time\":\"%s\",\"scheduledTime\":\"%s\",\"status\":\"%s\",\"instructions\":\"%s\"}",
                            d.getMedicationId(), escapeJson(d.getMedicationName()), escapeJson(d.getDosage()),
                            DateUtil.formatTime(d.getDoseTime()), DateUtil.formatDateTime(d.getScheduledDateTime()),
                            d.getStatus() != null ? d.getStatus().name() : "PENDING",
                            escapeJson(d.getInstructions())));
                }
                sb.append("],");

                // Readings
                sb.append("\"readings\":[");
                for (int i = 0; i < readings.size(); i++) {
                    HealthRecord r = readings.get(i);
                    if (i > 0) sb.append(",");
                    sb.append(String.format("{\"id\":%d,\"type\":\"%s\",\"time\":\"%s\",\"summary\":\"%s\",\"status\":\"%s\",\"notes\":\"%s\"}",
                            r.getRecordId(), r.getType(), DateUtil.formatDateTime(r.getRecordedAt()),
                            escapeJson(r.getSummary()), r.evaluate().name(), escapeJson(r.getNotes())));
                }
                sb.append("],");

                // Appointments
                sb.append("\"appointments\":[");
                for (int i = 0; i < upcoming.size(); i++) {
                    Appointment a = upcoming.get(i);
                    if (i > 0) sb.append(",");
                    String docName = doctorDAO.getById(a.getDoctorId()).map(Doctor::getName).orElse("Doctor");
                    String hospital = doctorDAO.getById(a.getDoctorId()).map(Doctor::getHospital).orElse("");
                    sb.append(String.format("{\"id\":%d,\"time\":\"%s\",\"doctor\":\"%s\",\"hospital\":\"%s\",\"purpose\":\"%s\",\"status\":\"%s\"}",
                            a.getAppointmentId(), DateUtil.formatDateTime(a.getDateTime()),
                            escapeJson(docName), escapeJson(hospital), escapeJson(a.getPurpose()), a.getStatus().name()));
                }
                sb.append("],");

                // Alerts
                sb.append("\"alerts\":[");
                for (int i = 0; i < alerts.size(); i++) {
                    EmergencyAlert a = alerts.get(i);
                    if (i > 0) sb.append(",");
                    sb.append(String.format("{\"id\":%d,\"time\":\"%s\",\"reason\":\"%s\",\"notified\":\"%s\"}",
                            a.getAlertId(), DateUtil.formatDateTime(a.getTimestamp()),
                            escapeJson(a.getReason()), escapeJson(a.getContactsNotified())));
                }
                sb.append("]");

                sb.append("}");

                sendResponse(exchange, 200, sb.toString(), "application/json");

            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class HealthReadingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            try {
                Map<String, String> body = parseRequestBody(exchange);
                int elderId = Integer.parseInt(body.get("elderId"));
                String type = body.get("type");
                String notes = body.getOrDefault("notes", "Web Entry");
                LocalDateTime now = LocalDateTime.now();

                HealthRecord record;
                if ("BP".equalsIgnoreCase(type)) {
                    int sys = Integer.parseInt(body.get("systolic"));
                    int dia = Integer.parseInt(body.get("diastolic"));
                    record = new BloodPressureRecord(0, elderId, now, notes, sys, dia);
                } else if ("SUGAR".equalsIgnoreCase(type)) {
                    double val = Double.parseDouble(body.get("sugarValue"));
                    ReadingType rType = ReadingType.valueOf(body.getOrDefault("sugarType", "RANDOM").toUpperCase());
                    record = new BloodSugarRecord(0, elderId, now, notes, val, rType);
                } else if ("HEART".equalsIgnoreCase(type)) {
                    int bpm = Integer.parseInt(body.get("bpm"));
                    record = new HeartRateRecord(0, elderId, now, notes, bpm);
                } else {
                    throw new IllegalArgumentException("Unknown reading type: " + type);
                }

                healthMonitor.addReading(record);
                HealthStatus status = record.evaluate();

                String resJson = String.format("{\"success\":true,\"id\":%d,\"summary\":\"%s\",\"status\":\"%s\",\"isCritical\":%b}",
                        record.getRecordId(), escapeJson(record.getSummary()), status.name(), status == HealthStatus.CRITICAL);
                sendResponse(exchange, 200, resJson, "application/json");

            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class SosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            try {
                Map<String, String> body = parseRequestBody(exchange);
                int elderId = Integer.parseInt(body.get("elderId"));
                String reason = body.getOrDefault("reason", "One-Touch Emergency SOS Pressed from Web Dashboard");

                SOSResult result = emergencyService.triggerSOS(elderId, reason);

                StringBuilder contactsJson = new StringBuilder("[");
                List<EmergencyContact> contacts = result.getContactsNotified();
                for (int i = 0; i < contacts.size(); i++) {
                    EmergencyContact c = contacts.get(i);
                    if (i > 0) contactsJson.append(",");
                    contactsJson.append(String.format("{\"name\":\"%s\",\"phone\":\"%s\",\"priority\":%d,\"relation\":\"%s\"}",
                            escapeJson(c.getName()), escapeJson(c.getPhone()), c.getPriority(), escapeJson(c.getRelationship())));
                }
                contactsJson.append("]");

                String doctorStr = result.getAssignedDoctor() != null
                        ? String.format("Dr. %s (%s) - %s", result.getAssignedDoctor().getName(), result.getAssignedDoctor().getSpecialization(), result.getAssignedDoctor().getPhone())
                        : "None Assigned";

                String resJson = String.format("{\"success\":true,\"alertId\":%d,\"time\":\"%s\",\"reason\":\"%s\",\"doctor\":\"%s\",\"contacts\":%s}",
                        result.getAlertId(), DateUtil.formatDateTime(result.getTimestamp()),
                        escapeJson(result.getReason()), escapeJson(doctorStr), contactsJson);

                sendResponse(exchange, 200, resJson, "application/json");

            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class DoseLogHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            try {
                Map<String, String> body = parseRequestBody(exchange);
                int medicationId = Integer.parseInt(body.get("medicationId"));
                LocalDateTime scheduledTime = DateUtil.parseDateTime(body.get("scheduledTime"));
                DoseStatus status = DoseStatus.valueOf(body.get("status").toUpperCase());

                int logId = reminderService.recordDose(medicationId, scheduledTime, status);
                sendResponse(exchange, 200, "{\"success\":true,\"logId\":" + logId + "}", "application/json");

            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class AppointmentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }

            try {
                Map<String, String> body = parseRequestBody(exchange);
                int elderId = Integer.parseInt(body.get("elderId"));
                int doctorId = Integer.parseInt(body.get("doctorId"));
                LocalDateTime dateTime = DateUtil.parseDateTime(body.get("dateTime"));
                String purpose = body.get("purpose");

                Appointment appt = appointmentScheduler.bookAppointment(elderId, doctorId, dateTime, purpose);
                sendResponse(exchange, 200, "{\"success\":true,\"appointmentId\":" + appt.getAppointmentId() + "}", "application/json");

            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class ReportHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            try {
                Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                int elderId = Integer.parseInt(query.getOrDefault("elderId", "1"));
                String type = query.getOrDefault("type", "full");

                String reportText = switch (type) {
                    case "health" -> reportGenerator.generateHealthSummaryReport(elderId);
                    case "adherence" -> reportGenerator.generateMedicationAdherenceReport(elderId);
                    case "appointments" -> reportGenerator.generateAppointmentHistoryReport(elderId);
                    case "alerts" -> reportGenerator.generateEmergencyAlertLogReport(elderId);
                    default -> reportGenerator.generateFullElderReport(elderId);
                };

                sendResponse(exchange, 200, "{\"report\":\"" + escapeJson(reportText) + "\"}", "application/json");

            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class SeedHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            try {
                new SampleDataLoader(dbManager).loadSampleData();
                sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Sample data seeded successfully!\"}", "application/json");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    private class ResetDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                dbManager.clearAllData();
                new SampleDataLoader(dbManager).loadSampleData();
                sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Database successfully reset with simple sample data.\"}", "application/json");
            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}", "application/json");
            }
        }
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    private void sendResponse(HttpExchange exchange, int status, String responseText, String contentType) throws IOException {
        byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void setCORSHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.trim().isEmpty()) {
            return map;
        }
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                map.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private Map<String, String> parseRequestBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> map = new HashMap<>();
        if (body.trim().startsWith("{") && body.trim().endsWith("}")) {
            // Simple JSON key-value parser
            String trimmed = body.trim();
            trimmed = trimmed.substring(1, trimmed.length() - 1);
            String[] tokens = trimmed.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String token : tokens) {
                String[] kv = token.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String val = kv[1].trim().replace("\"", "");
                    map.put(key, val);
                }
            }
        } else {
            // Form url-encoded
            for (String param : body.split("&")) {
                String[] pair = param.split("=");
                if (pair.length > 1) {
                    map.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                }
            }
        }
        return map;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
