package com.example.dashboard;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.example.service.BYODService;
import com.example.monitoring.QRScannerWindow;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardController {

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());
    private static final String STYLESHEET_PATH = "/css/stylesheet.css";
    private static final String LOGIN_FXML = "/fxml/login.fxml";
    private static final String MONITORING_FXML = "/fxml/monitoring.fxml";
    private static final String REGISTRATION_FXML = "/fxml/registration.fxml";
    private static final String REPORTS_FXML = "/fxml/reports.fxml";
    private static final String ACCOUNT_FXML = "/fxml/account.fxml";

    private static final String SYNC_STATUS_LIVE = "Live";
    private static final String SYNC_STATUS_OFFLINE = "Offline";
    private static final String SYNC_STATUS_LOADING = "Loading";

    public enum PendingAction { NONE, REPORTS, EXPORT }
    private static PendingAction pendingAction = PendingAction.NONE;

    public static PendingAction getPendingAction() {
        return pendingAction;
    }

    public static void clearPendingAction() {
        pendingAction = PendingAction.NONE;
    }

    // Stats cards
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalDevicesLabel;
    @FXML private Label devicesInsideLabel;
    @FXML private Label ingressTodayLabel;
    @FXML private Label egressTodayLabel;

    // Header
    @FXML private Label dateLabel;
    @FXML private Label syncStatusLabel;

    // Buttons
    @FXML private Button refreshButton;
    @FXML private Button logoutButton;
    @FXML private Button dashboardButton;
    @FXML private Button monitoringButton;
    @FXML private Button registrationButton;
    @FXML private Button reportsButton;
    @FXML private Button accountButton;
    @FXML private Button seeAllButton;
    @FXML private Button scanQrButton;
    @FXML private Button searchButton;
    @FXML private Button ingressButton;
    @FXML private Button egressButton;
    @FXML private Button exportButton;

    // Chart
    @FXML private BarChart<String, Number> activityChart;

    // Log entries (5 rows)
    @FXML private Label logName0, logName1, logName2, logName3, logName4;
    @FXML private Label logId0, logId1, logId2, logId3, logId4;
    @FXML private Label logStatus0, logStatus1, logStatus2, logStatus3, logStatus4;
    @FXML private Label logTime0, logTime1, logTime2, logTime3, logTime4;

    // Arrays to access log rows conveniently
    private final Label[] logNames = new Label[5];
    private final Label[] logIds = new Label[5];
    private final Label[] logStatuses = new Label[5];
    private final Label[] logTimes = new Label[5];

    // CONNECTED: Database Service Instance
    private final BYODService byodService = new BYODService();

    @FXML
    public void initialize() {
        initLogArrays();
        addStylesheetToScene();
        dateLabel.setText(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy").format(LocalDate.now()));

        if (dashboardButton != null) {
            dashboardButton.getStyleClass().add("active");
        }

        loadLiveDatabaseData();
    }

    private void initLogArrays() {
        logNames[0] = logName0; logNames[1] = logName1; logNames[2] = logName2;
        logNames[3] = logName3; logNames[4] = logName4;
        logIds[0] = logId0;     logIds[1] = logId1;     logIds[2] = logId2;
        logIds[3] = logId3;     logIds[4] = logId4;
        logStatuses[0] = logStatus0; logStatuses[1] = logStatus1; logStatuses[2] = logStatus2;
        logStatuses[3] = logStatus3; logStatuses[4] = logStatus4;
        logTimes[0] = logTime0; logTimes[1] = logTime1; logTimes[2] = logTime2;
        logTimes[3] = logTime3; logTimes[4] = logTime4;
    }

    private void addStylesheetToScene() {
        if (dateLabel != null && dateLabel.getScene() != null) {
            Scene scene = dateLabel.getScene();
            String css = getClass().getResource(STYLESHEET_PATH).toExternalForm();
            if (!scene.getStylesheets().contains(css)) {
                scene.getStylesheets().add(css);
            }
        } else {
            LOGGER.warning("Scene not available for stylesheet injection");
        }
    }

    // CONNECTED: Pulls actual relational metrics and live operational database transaction logs
    private void loadLiveDatabaseData() {
        updateSyncStatus(SYNC_STATUS_LOADING);

        Platform.runLater(() -> {
            try {
                // 1. Load Live Analytical Statistics Counters
                Map<String, Integer> metrics = byodService.fetchDashboardMetrics();
                if (!metrics.isEmpty()) {
                    totalStudentsLabel.setText(String.valueOf(metrics.getOrDefault("totalStudents", 0)));
                    totalDevicesLabel.setText(String.valueOf(metrics.getOrDefault("totalDevices", 0)));
                    devicesInsideLabel.setText(String.valueOf(metrics.getOrDefault("devicesInside", 0)));
                    ingressTodayLabel.setText(String.valueOf(metrics.getOrDefault("ingressToday", 0)));
                    egressTodayLabel.setText(String.valueOf(metrics.getOrDefault("egressToday", 0)));
                }

                // 2. Clear out log UI structures before fresh rendering passes
                for (int i = 0; i < 5; i++) {
                    if (logNames[i] != null) {
                        logNames[i].setText("");
                        logIds[i].setText("");
                        logStatuses[i].setText("");
                        logTimes[i].setText("");
                    }
                }

                // 3. Load Recent Log Entries (Max 5 Rows)
                List<Object[]> databaseLogs = byodService.fetchLogs();
                int displayLimit = Math.min(databaseLogs.size(), 5);

                for (int i = 0; i < displayLimit; i++) {
                    Object[] row = databaseLogs.get(i);
                    String studentName = (String) row[1];
                    String studentId = (String) row[2];
                    String egressTime = (String) row[5];

                    String statusText = (egressTime == null) ? "📥 In" : "📤 Out";
                    String timestampText = (egressTime == null) ? (String) row[4] : egressTime;

                    // Clean string styling trim if dates contain long ISO string blocks
                    if (timestampText != null && timestampText.contains(" ")) {
                        String[] parts = timestampText.split(" ");
                        if (parts.length > 1) timestampText = parts[1].substring(0, 5); // Pulls 'HH:MM' format block
                    }

                    if (logNames[i] != null) {
                        logNames[i].setText(studentName);
                        logIds[i].setText(studentId);
                        logStatuses[i].setText(statusText);
                        logTimes[i].setText(timestampText);
                        applyStatusStyle(logStatuses[i], logTimes[i], statusText);
                    }
                }

                // 4. Update Weekly Activity Analytics Chart Visuals
                String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
                int[] ingressData = {23, 31, 28, 35, 42, 18, 12}; // Can hook up to custom database chart query counts later
                int[] egressData  = {20, 27, 25, 30, 38, 15, 10};
                updateChart(days, ingressData, egressData);

                updateSyncStatus(SYNC_STATUS_LIVE);

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to load live tracking logs dashboard metrics", ex);
                updateSyncStatus(SYNC_STATUS_OFFLINE);
            }
        });
    }

    private void applyStatusStyle(Label statusLabel, Label timeLabel, String statusText) {
        if (statusLabel == null || timeLabel == null) return;
        statusLabel.getStyleClass().removeAll("log-status-in", "log-status-out");
        timeLabel.getStyleClass().removeAll("log-time-in", "log-time-out");
        if (statusText.contains("In")) {
            statusLabel.getStyleClass().add("log-status-in");
            timeLabel.getStyleClass().add("log-time-in");
        } else if (statusText.contains("Out")) {
            statusLabel.getStyleClass().add("log-status-out");
            timeLabel.getStyleClass().add("log-time-out");
        }
    }

    private void updateChart(String[] days, int[] ingress, int[] egress) {
        if (activityChart == null) return;
        XYChart.Series<String, Number> ingressSeries = new XYChart.Series<>();
        ingressSeries.setName("Ingress");
        XYChart.Series<String, Number> egressSeries = new XYChart.Series<>();
        egressSeries.setName("Egress");
        for (int i = 0; i < days.length; i++) {
            ingressSeries.getData().add(new XYChart.Data<>(days[i], ingress[i]));
            egressSeries.getData().add(new XYChart.Data<>(days[i], egress[i]));
        }
        activityChart.getData().clear();
        activityChart.getData().addAll(ingressSeries, egressSeries);
    }

    private void updateSyncStatus(String status) {
        if (syncStatusLabel == null) return;
        syncStatusLabel.getStyleClass().removeAll("sync-status-live", "sync-status-offline", "sync-status-connecting");
        if (SYNC_STATUS_LIVE.equalsIgnoreCase(status)) {
            syncStatusLabel.setText("⏺ Live");
            syncStatusLabel.getStyleClass().add("sync-status-live");
        } else if (SYNC_STATUS_OFFLINE.equalsIgnoreCase(status)) {
            syncStatusLabel.setText("⏺ Offline");
            syncStatusLabel.getStyleClass().add("sync-status-offline");
        } else {
            syncStatusLabel.setText("⏳ Loading...");
            syncStatusLabel.getStyleClass().add("sync-status-connecting");
        }
    }

    // ==================== Navigation Handlers ====================
    @FXML
    private void handleRefresh() {
        LOGGER.info("Manual refresh triggered");
        loadLiveDatabaseData();
    }

    @FXML
    private void handleLogout() {
        handleRefresh();
    }

    private void requireLoginThenOpen(PendingAction action) {
        pendingAction = action;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(LOGIN_FXML));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());

            Stage loginStage = new Stage();
            loginStage.setScene(scene);
            loginStage.setTitle("BYOD Monitoring System - Login Required");
            loginStage.setResizable(false);
            loginStage.centerOnScreen();
            loginStage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load login screen", e);
            showError("Login Error", "Failed to load login screen", e.getMessage());
        }
    }

    @FXML private void handleDashboard() { handleRefresh(); }
    @FXML private void handleMonitoring() { navigateTo(MONITORING_FXML, "Monitoring - BYOD System"); }
    @FXML private void handleRegistration() { navigateTo(REGISTRATION_FXML, "Registration - BYOD System"); }
    @FXML private void handleReports() { requireLoginThenOpen(PendingAction.REPORTS); }
    @FXML private void handleAccount() { navigateTo(ACCOUNT_FXML, "Account - BYOD System"); }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath.toLowerCase()));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());

            // FIXED: Grabs root anchor window from the active click sender element safely
            Stage stage = (Stage) dateLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.setMaximized(false);
            stage.centerOnScreen();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load " + fxmlPath, e);
            showError("Navigation Error", "Could not load view", "Failed to open " + title + "\n\n" + e.getMessage());
        }
    }

    // ==================== Additional Action Handlers ====================
    @FXML
    private void handleSeeAll() {
        navigateTo(MONITORING_FXML, "Monitoring - BYOD System");
    }

    // CONNECTED: Directly opens your live webcam scanner modal window right from the main dashboard dashboard view button!
    @FXML
    private void handleScanQr() {
        Stage currentStage = (Stage) dateLabel.getScene().getWindow();

        QRScannerWindow.openScanner(currentStage, qrPayload -> {
            try {
                String studentId = qrPayload;
                if (qrPayload.contains("|")) {
                    studentId = qrPayload.split("\\|")[0];
                }

                // Call core business data transaction loop
                byodService.updateEgress(studentId);

                showInfoDialog("Access Granted", "Scan Successful", "QR Scanned! You can now exit the Campus.");
                loadLiveDatabaseData(); // Reload stats counters dynamically instantly

            } catch (Exception ex) {
                showError("Scanner Database Error", "Failed to clear campus egress logging details", ex.getMessage());
            }
        });
    }

    @FXML
    private void handleSearch() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MONITORING_FXML));
            Parent root = loader.load();

            Stage stage = (Stage) dateLabel.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Monitoring - BYOD System");
            stage.centerOnScreen();

            Platform.runLater(() -> {
                try {
                    TextField searchField = (TextField) root.lookup("#searchField");
                    if (searchField != null) {
                        searchField.requestFocus();
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Could not focus search field", e);
                }
            });

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load monitoring view", e);
            showError("Navigation Error", "Could not open Monitoring", e.getMessage());
        }
    }

    @FXML
    private void handleIngress() {
        navigateTo(MONITORING_FXML, "Monitoring - BYOD System");
    }

    @FXML
    private void handleEgress() {
        navigateTo(MONITORING_FXML, "Monitoring - BYOD System");
    }

    @FXML
    private void handleExport() {
        requireLoginThenOpen(PendingAction.EXPORT);
    }
    // ==================== Standard Alert Helpers ====================
    private void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfoDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}