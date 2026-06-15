package com.example.service;

import java.sql.*;
import java.io.File;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class BYODService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/byod_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    public String validate(String sid, String fn, String ln, String contact, String yearSec) {
        return "VALID";
    }

    public void registerStudent(String sid, String ln, String fn, String ys, String cp,
                                String cn, String dt, String bm, String cd) throws Exception {
        String sql = "INSERT INTO student_device_logs (student_id, last_name, first_name, year_section, course_program, contact_number, device_type, brand_model, color_description) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sid); ps.setString(2, ln); ps.setString(3, fn);
            ps.setString(4, ys); ps.setString(5, cp); ps.setString(6, cn);
            ps.setString(7, dt); ps.setString(8, bm); ps.setString(9, cd);
            ps.executeUpdate();
        }
    }

    // Explicitly updates a student's active logs to set an Egress timestamp
    public void updateEgress(String sid) throws Exception {
        String sql = "UPDATE student_device_logs SET egress_time = CURRENT_TIMESTAMP WHERE student_id = ? AND egress_time IS NULL ORDER BY ingress_time DESC LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sid);
            ps.executeUpdate();
        }
    }

    // Adds a clean new Ingress log item for an existing student context
    public void updateIngress(String sid, String studentName, String brandModel) throws Exception {
        String[] nameParts = studentName.split(", ");
        String ln = nameParts[0];
        String fn = nameParts.length > 1 ? nameParts[1] : "";

        String sql = "INSERT INTO student_device_logs (student_id, last_name, first_name, brand_model, ingress_time, egress_time) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, NULL)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sid);
            ps.setString(2, ln);
            ps.setString(3, fn);
            ps.setString(4, brandModel);
            ps.executeUpdate();
        }
    }

    public List<Object[]> fetchLogs() throws Exception {
        List<Object[]> logs = new ArrayList<>();
        String sql = "SELECT log_id, student_id, last_name, first_name, brand_model, ingress_time, egress_time FROM student_device_logs ORDER BY ingress_time DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new Object[]{
                        rs.getInt("log_id"),
                        rs.getString("student_id"),
                        rs.getString("last_name") + ", " + rs.getString("first_name"),
                        rs.getString("brand_model"),
                        rs.getString("ingress_time"),
                        rs.getString("egress_time")
                });
            }
        }
        return logs;
    }

    // Gathers analytical numbers using standard dynamic SQL evaluations
    public Map<String, Integer> fetchDashboardMetrics() throws Exception {
        Map<String, Integer> metrics = new HashMap<>();

        String sql = "SELECT " +
                "  (SELECT COUNT(DISTINCT student_id) FROM student_device_logs) as total_students, " +
                "  (SELECT COUNT(*) FROM student_device_logs) as total_devices, " +
                "  (SELECT COUNT(*) FROM student_device_logs WHERE egress_time IS NULL) as devices_inside, " +
                "  (SELECT COUNT(*) FROM student_device_logs WHERE DATE(ingress_time) = CURRENT_DATE) as ingress_today, " +
                "  (SELECT COUNT(*) FROM student_device_logs WHERE DATE(egress_time) = CURRENT_DATE) as egress_today";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                metrics.put("totalStudents", rs.getInt("total_students"));
                metrics.put("totalDevices", rs.getInt("total_devices"));
                metrics.put("devicesInside", rs.getInt("devices_inside"));
                metrics.put("ingressToday", rs.getInt("ingress_today"));
                metrics.put("egressToday", rs.getInt("egress_today"));
            }
        }
        return metrics;
    }

    public String generateQR(String payload, String studentId, String outputDir) throws Exception {
        String path = outputDir + File.separator + "QR_" + studentId + ".png";
        BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 400, 400);
        MatrixToImageWriter.writeToPath(matrix, "PNG", FileSystems.getDefault().getPath(path));
        return path;
    }

    /* ════════════════════════════════════════════════════════════════════ */
    /* ── REPORT GENERATION METRIC QUERIES FOR REPORTSCONTROLLER ────────── */
    /* ════════════════════════════════════════════════════════════════════ */

    /**
     * Pulls the count of hardware grouped by device type from student_device_logs.
     */
    public Map<String, Integer> fetchInventoryBreakdown() {
        Map<String, Integer> breakdown = new HashMap<>();
        breakdown.put("Laptop", 0);
        breakdown.put("Tablet", 0);
        breakdown.put("Smartphone", 0);
        breakdown.put("Others", 0);

        String sql = "SELECT device_type, COUNT(*) as count FROM student_device_logs WHERE device_type IS NOT NULL GROUP BY device_type";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String type = rs.getString("device_type");
                int count = rs.getInt("count");

                if (type != null) {
                    if (type.equalsIgnoreCase("Laptop")) breakdown.put("Laptop", count);
                    else if (type.equalsIgnoreCase("Tablet")) breakdown.put("Tablet", count);
                    else if (type.equalsIgnoreCase("Smartphone") || type.equalsIgnoreCase("Mobile")) breakdown.put("Smartphone", count);
                    else breakdown.put("Others", breakdown.get("Others") + count);
                }
            }
        } catch (Exception e) {
            System.err.println("Error pulling inventory breakdown: " + e.getMessage());
        }
        return breakdown;
    }

    /**
     * Fetches all registered student profiles for the reports master table list.
     * Adjusted index mapping positions to line up with ReportsController targets.
     */
    public List<String[]> fetchRegisteredStudentsList() {
        List<String[]> students = new ArrayList<>();

        String sql = "SELECT DISTINCT student_id, first_name, last_name, course_program, device_type, brand_model " +
                "FROM student_device_logs WHERE student_id IS NOT NULL ORDER BY last_name ASC";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                students.add(new String[]{
                        rs.getString("student_id"), // Index 0: Student ID
                        fullName,                   // Index 1: Full Name
                        rs.getString("course_program") != null ? rs.getString("course_program") : "N/A",
                        rs.getString("device_type") != null ? rs.getString("device_type") : "Unknown",
                        rs.getString("brand_model") != null ? rs.getString("brand_model") : "N/A"
                });
            }
        } catch (Exception e) {
            System.err.println("Error fetching reporting students list: " + e.getMessage());
        }
        return students;
    }

    /**
     * Queries the database dynamically based on active filtering criteria to generate a raw CSV payload.
     */
    public List<String[]> fetchFilteredExportData(String courseFilter, boolean lap, boolean tab, boolean mob, boolean oth) {
        List<String[]> data = new ArrayList<>();
        List<String> types = new ArrayList<>();
        if (lap) types.add("'Laptop'");
        if (tab) types.add("'Tablet'");
        if (mob) {
            types.add("'Smartphone'");
            types.add("'Mobile'");
        }
        if (oth) types.add("'Others'");
        StringBuilder sql = new StringBuilder(
                "SELECT student_id, last_name, first_name, year_section, course_program, contact_number, device_type, brand_model, color_description, ingress_time, egress_time " +
                        "FROM student_device_logs WHERE 1=1"
        );

        if (courseFilter != null && !courseFilter.isBlank()) {
            sql.append(" AND course_program LIKE ?");
        }

        if (!types.isEmpty()) {
            sql.append(" AND device_type IN (").append(String.join(",", types)).append(")");
        }

        sql.append(" ORDER BY ingress_time DESC");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIdx = 1;
            if (courseFilter != null && !courseFilter.isBlank()) {
                ps.setString(paramIdx++, "%" + courseFilter.trim() + "%");
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.add(new String[]{
                            rs.getString("student_id"),
                            rs.getString("last_name") + ", " + rs.getString("first_name"),
                            rs.getString("year_section") != null ? rs.getString("year_section") : "N/A",
                            rs.getString("course_program") != null ? rs.getString("course_program") : "N/A",
                            rs.getString("device_type") != null ? rs.getString("device_type") : "Unknown",
                            rs.getString("brand_model") != null ? rs.getString("brand_model") : "N/A",
                            rs.getString("ingress_time") != null ? rs.getString("ingress_time").toString() : "N/A",
                            rs.getString("egress_time") != null ? rs.getString("egress_time").toString() : "Still Inside"
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Error running filter export compiler query: " + e.getMessage());
        }
        return data;
    }

    /**
     * Pulls aggregated weekly Ingress and Egress traffic counts for the Dashboard BarChart.
     */
    public Map<String, Map<String, Integer>> fetchWeeklyChartData() {
        Map<String, Map<String, Integer>> chartData = new HashMap<>();
        Map<String, Integer> ingressMap = new HashMap<>();
        Map<String, Integer> egressMap = new HashMap<>();

        String[] shortDays = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String d : shortDays) {
            ingressMap.put(d, 0);
            egressMap.put(d, 0);
        }

        String sql = "SELECT " +
                "  DATE_FORMAT(ingress_time, '%a') as log_day, " +
                "  COUNT(ingress_time) as ingress_count, " +
                "  COUNT(egress_time) as egress_count " +
                "FROM student_device_logs " +
                "WHERE ingress_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
                "GROUP BY DATE_FORMAT(ingress_time, '%a')";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String day = rs.getString("log_day");
                int ingressCount = rs.getInt("ingress_count");
                int egressCount = rs.getInt("egress_count");

                if (day != null && ingressMap.containsKey(day)) {
                    ingressMap.put(day, ingressCount);
                    egressMap.put(day, egressCount);
                }
            }
        } catch (Exception e) {
            System.err.println("Error executing weekly traffic breakdown query: " + e.getMessage());
        }

        chartData.put("Ingress", ingressMap);
        chartData.put("Egress", egressMap);
        return chartData;
    }

    /* ════════════════════════════════════════════════════════════════════ */
    /* ── NEW: ADMIN CRUD DATA CONTEXT OPERATORS ────────────────────────── */
    /* ════════════════════════════════════════════════════════════════════ */

    /**
     * Inserts a completely new student record cluster directly into logs.
     */
    public boolean insertRegisteredStudent(String id, String name, String dept, String type, String serial) {
        String sql = "INSERT INTO student_device_logs (student_id, first_name, last_name, course_program, device_type, brand_model) VALUES (?, ?, ?, ?, ?, ?)";

        String firstName = name;
        String lastName = "";
        if (name.contains(",")) {
            String[] parts = name.split(",", 2);
            lastName = parts[0].trim();
            firstName = parts[1].trim();
        } else if (name.contains(" ")) {
            int lastSpace = name.lastIndexOf(" ");
            firstName = name.substring(0, lastSpace).trim();
            lastName = name.substring(lastSpace).trim();
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, dept);
            ps.setString(5, type);
            ps.setString(6, serial);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error inserting admin entry: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates an existing record targeted by the primary student ID context.
     */
    public boolean updateRegisteredStudent(String id, String name, String dept, String type, String serial) {
        String sql = "UPDATE student_device_logs SET first_name = ?, last_name = ?, course_program = ?, device_type = ?, brand_model = ? WHERE student_id = ?";

        String firstName = name;
        String lastName = "";
        if (name.contains(",")) {
            String[] parts = name.split(",", 2);
            lastName = parts[0].trim();
            firstName = parts[1].trim();
        } else if (name.contains(" ")) {
            int lastSpace = name.lastIndexOf(" ");
            firstName = name.substring(0, lastSpace).trim();
            lastName = name.substring(lastSpace).trim();
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, dept);
            ps.setString(4, type);
            ps.setString(5, serial);
            ps.setString(6, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error updating admin entry: " + e.getMessage());
            return false;
        }
    }

    /**
     * Purges records linked to a specific unique student ID indicator tracking key.
     */
    public boolean deleteRegisteredStudent(String id) {
        String sql = "DELETE FROM student_device_logs WHERE student_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Error executing admin drop execution query: " + e.getMessage());
            return false;
        }
    }
}