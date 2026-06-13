package com.example.service;

import java.sql.*;
import java.io.File;
import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.nio.file.FileSystems;

public class BYODService {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/byod_system_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    // Returns "VALID" or error message — reuse RegistrationValidator as-is
    public String validate(String sid, String fn, String ln, String contact, String yearSec) {
        return RegistrationValidator.validate(sid, fn, ln, contact, yearSec);
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

    public String generateQR(String payload, String studentId, String outputDir) throws Exception {
        String path = outputDir + File.separator + "QR_" + studentId + ".png";
        BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 400, 400);
        MatrixToImageWriter.writeToPath(matrix, "PNG", FileSystems.getDefault().getPath(path));
        return path;
    }

    public void updateEgress(String sid) throws Exception {
        String sql = "UPDATE student_device_logs SET egress_time = CURRENT_TIMESTAMP WHERE student_id = ? AND egress_time IS NULL LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sid);
            ps.executeUpdate();
        }
    }

    // Returns list of log rows for MonitoringController
    public java.util.List<Object[]> fetchLogs() throws Exception {
        java.util.List<Object[]> logs = new java.util.ArrayList<>();
        String sql = "SELECT id, student_id, last_name, first_name, device_type, ingress_time, egress_time FROM student_device_logs ORDER BY ingress_time DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new Object[]{
                        rs.getInt("id"), rs.getString("student_id"),
                        rs.getString("last_name") + ", " + rs.getString("first_name"),
                        rs.getString("device_type"), rs.getString("ingress_time"),
                        rs.getString("egress_time")
                });
            }
        }
        return logs;
    }
}