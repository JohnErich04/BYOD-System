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

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    @FXML
    public void initialize() {
        initLogArrays();
        addStylesheetToScene();
        dateLabel.setText(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy").format(LocalDate.now()));
        dashboardButton.getStyleClass().add("active");
        loadHardcodedData();
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
        Scene scene = dateLabel.getScene();
        if (scene != null) {
            String css = getClass().getResource(STYLESHEET_PATH).toExternalForm();
            if (!scene.getStylesheets().contains(css)) {
                scene.getStylesheets().add(css);
            }
        } else {
            LOGGER.warning("Scene not available for stylesheet injection");
        }
    }

    private void loadHardcodedData() {
        updateSyncStatus(SYNC_STATUS_LOADING);
        Platform.runLater(() -> {
            // Statistics
            totalStudentsLabel.setText("245");
            totalDevicesLabel.setText("312");
            devicesInsideLabel.setText("187");
            ingressTodayLabel.setText("42");
            egressTodayLabel.setText("38");

            // Logs
            String[][] logs = {
                    {"John Michael Santos", "2024-00125", "📥 In", "08:15 AM"},
                    {"Maria Clara Gomez",   "2024-00456", "📤 Out", "05:30 PM"},
                    {"Jose Rizal Mercado",  "2024-00789", "📥 In", "09:20 AM"},
                    {"Andres Bonifacio",    "2024-00321", "📥 In", "10:45 AM"},
                    {"Gabriela Silang",     "2024-00888", "📤 Out", "04:00 PM"}
            };
            for (int i = 0; i < logs.length; i++) {
                logNames[i].setText(logs[i][0]);
                logIds[i].setText(logs[i][1]);
                logStatuses[i].setText(logs[i][2]);
                logTimes[i].setText(logs[i][3]);
                applyStatusStyle(logStatuses[i], logTimes[i], logs[i][2]);
            }

            // Chart data
            String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            int[] ingressData = {23, 31, 28, 35, 42, 18, 12};
            int[] egressData  = {20, 27, 25, 30, 38, 15, 10};
            updateChart(days, ingressData, egressData);

            updateSyncStatus(SYNC_STATUS_LIVE);
        });
    }

    private void applyStatusStyle(Label statusLabel, Label timeLabel, String statusText) {
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
        loadHardcodedData();
    }

    @FXML
    private void handleLogout() {
        Stage currentStage = (Stage) logoutButton.getScene().getWindow();
        currentStage.close();
        loadLoginScreen();
    }

    private void loadLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(LOGIN_FXML));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());

            Stage loginStage = new Stage();
            loginStage.setScene(scene);
            loginStage.setTitle("BYOD Monitoring System - Login");
            loginStage.setResizable(false);
            loginStage.centerOnScreen();
            loginStage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load login screen", e);
            showError("Login Error", "Failed to load login screen", e.getMessage());
        }
    }

    @FXML
    private void handleDashboard() {
        handleRefresh(); // already on dashboard
    }

    @FXML
    private void handleMonitoring() {
        navigateTo(MONITORING_FXML, "Monitoring - BYOD System");
    }

    @FXML
    private void handleRegistration() {
        navigateTo(REGISTRATION_FXML, "Registration - BYOD System");
    }

    @FXML
    private void handleReports() {
        navigateTo(REPORTS_FXML, "Reports - BYOD System");
    }

    @FXML
    private void handleAccount() {
        navigateTo(ACCOUNT_FXML, "Account - BYOD System");
    }

    private void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());

            Stage stage = (Stage) monitoringButton.getScene().getWindow();
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

    @FXML
    private void handleScanQr() {
        LOGGER.info("QR scan clicked");
        showInfoDialog("QR Scanner", "Scan Student QR Code",
                "This feature will open the camera to scan student QR codes for check‑in/out.");
    }

    @FXML
    private void handleSearch() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MONITORING_FXML));
            Parent root = loader.load();

            // Get the monitoring controller to access its search field
            Object controller = loader.getController();

            Stage stage = (Stage) searchButton.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Monitoring - BYOD System");
            stage.centerOnScreen();

            // After the scene is rendered, focus the search field
            Platform.runLater(() -> {
                try {
                    // Use reflection or a public method to access the search field
                    // Option 1: If MonitoringController has a public method focusSearchField()
                    // ((MonitoringController) controller).focusSearchField();

                    // Option 2: Direct lookup (simpler, no need to modify MonitoringController)
                    TextField searchField = (TextField) root.lookup("#searchField");
                    if (searchField != null) {
                        searchField.requestFocus();
                    } else {
                        LOGGER.warning("Search field not found in monitoring view");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Could not focus search field", e);
                }
            });

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load monitoring view", e);
            showError("Navigation Error", "Could not open Monitoring",
                    "Failed to load the monitoring view.\n\nDetails: " + e.getMessage());
        }
    }

    @FXML
    private void handleIngress() {
        LOGGER.info("Manual ingress clicked");
        showInfoDialog("Log Ingress", "Record Device Entry",
                "Select a student and device to log manual ingress.");
    }

    @FXML
    private void handleEgress() {
        LOGGER.info("Manual egress clicked");
        showInfoDialog("Log Egress", "Record Device Exit",
                "Select a student and device to log manual egress.");
    }

    @FXML
    private void handleExport() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(REPORTS_FXML));
            Parent root = loader.load();

            Stage stage = (Stage) exportButton.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Reports - BYOD System");
            stage.centerOnScreen();

            // After the scene is ready, programmatically click the "Export all" button in Reports view
            Platform.runLater(() -> {
                Button exportAllBtn = (Button) root.lookup("#exportAllBtn");
                if (exportAllBtn != null) {
                    exportAllBtn.fire();
                } else {
                    LOGGER.warning("exportAllBtn not found in Reports view");
                    showError("Export Error", "Button not found",
                            "The 'Export all' button could not be located in the Reports view.");
                }
            });

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load reports view", e);
            showError("Navigation Error", "Could not open Reports section",
                    "Failed to load the reports view.\n\nDetails: " + e.getMessage());
        }
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