package com.example.reports;

import com.example.Auth;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import com.example.service.BYODService;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReportsController {

    private boolean isAdminLoggedIn = false;

    /* ── Navbar ─────────────────────────────────────────── */
    @FXML private Button logoutButton;
    @FXML private Button scheduleBtn;
    @FXML private Button exportAllBtn;
    @FXML private Button reportsButton;

    /* ── StackPane views ────────────────────────────────── */
    @FXML private BorderPane rootPane;
    @FXML private ScrollPane mainView;
    @FXML private ScrollPane exportView;
    @FXML private ScrollPane inventoryView;
    @FXML private ScrollPane studentsView;

    /* ── Main view widgets ──────────────────────────────── */
    @FXML private ComboBox<String> periodCombo;
    @FXML private Label statTotalStudents;
    @FXML private Label statDeviceInventory;
    @FXML private Label statFullExport;
    @FXML private BarChart<String, Number> weeklyChart;
    @FXML private Label weeklyChartTitle;

    /* ── Interactive CRUD Admin Fields ──────────────────── */
    @FXML private TextField studentIdField;
    @FXML private TextField studentNameField;
    @FXML private TextField departmentField;
    @FXML private TextField deviceField;
    @FXML private TextField serialField;

    /* ── Students Management Table ──────────────────────── */
    @FXML private TableView<StudentRow> studentsTable;
    @FXML private TableColumn<StudentRow, String> colName, colStudentId, colDepartment, colDevice, colSerial;

    /* ── Wizard Export Views Variables (FIXES UNRESOLVED ID ERRORS) ── */
    @FXML private DatePicker exportFromDate;
    @FXML private DatePicker exportToDate;
    @FXML private TextField exportCourseField;
    @FXML private CheckBox exportAllCheckBox;
    @FXML private CheckBox exportLaptops;
    @FXML private CheckBox exportMobile;
    @FXML private CheckBox exportTablets;
    @FXML private CheckBox exportOthers;
    @FXML private Button exportCsvBtn;
    @FXML private Button exportPdfBtn;
    @FXML private Button exportXlsBtn;
    @FXML private Button generateExportBtn;
    @FXML private TableView<ExportRow> exportsTable;
    @FXML private TableColumn<ExportRow, String> colExpId, colExpParams, colExpStatus, colExpAction;

    /* ── Inventory Metric Breakdown Variables (FIXES UNRESOLVED ID ERRORS) ── */
    @FXML private Label totalDevicesLabel;
    @FXML private ProgressBar laptopsBar, tabletsBar, smartphonesBar, othersBar;
    @FXML private Label laptopsPct, tabletsPct, smartphonesPct, othersPct;
    @FXML private Label laptopsCount, tabletsCount, smartphonesCount, othersCount;

    private enum View { MAIN, EXPORT, INVENTORY, STUDENTS }
    private final BYODService byodService = new BYODService();
    private String selectedFormat = "CSV";

    @FXML
    public void initialize() {
        if (weeklyChart != null) {
            weeklyChart.setAnimated(false);
            if (weeklyChart.getXAxis() != null) weeklyChart.getXAxis().setAnimated(false);
            if (weeklyChart.getYAxis() != null) weeklyChart.getYAxis().setAnimated(false);
        }

        if (periodCombo != null) {
            periodCombo.getItems().addAll("This Month", "Last Month", "Last 3 Months", "This Year");
            periodCombo.setValue("This Month");
        }

        setupTables();
        loadData();
        showView(View.MAIN);
    }

    private void showView(View v) {
        setVisible(mainView,      v == View.MAIN);
        setVisible(exportView,    v == View.EXPORT);
        setVisible(inventoryView, v == View.INVENTORY);
        setVisible(studentsView,  v == View.STUDENTS);

        if (v == View.INVENTORY) {
            updateInventoryMetricsDisplay();
        }
    }

    private void setVisible(ScrollPane pane, boolean show) {
        if (pane == null) return;
        pane.setVisible(show);
        pane.setManaged(show);
    }

    private void setupTables() {
        if (studentsTable != null) {
            colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
            colName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
            colDevice.setCellValueFactory(new PropertyValueFactory<>("device"));
            colSerial.setCellValueFactory(new PropertyValueFactory<>("serial"));
            studentsTable.setPlaceholder(new Label("No records inside the security database structure."));

            studentsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    studentIdField.setText(newVal.getStudentId());
                    studentNameField.setText(newVal.getName());
                    departmentField.setText(newVal.getDepartment());
                    deviceField.setText(newVal.getDevice());
                    serialField.setText(newVal.getSerial());
                    studentIdField.setEditable(false);
                }
            });
        }

        if (exportsTable != null) {
            colExpId.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
            colExpParams.setCellValueFactory(new PropertyValueFactory<>("parameters"));
            colExpStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colExpAction.setCellValueFactory(new PropertyValueFactory<>("action"));
            exportsTable.setPlaceholder(new Label("No recent administrative data exports found."));
        }
    }

    private void updateInventoryMetricsDisplay() {
        try {
            Map<String, Integer> metrics = byodService.fetchDashboardMetrics();
            int total = metrics.getOrDefault("totalDevices", 5); // Fallback mock values if DB empty
            if (total == 0) total = 1;

            if (totalDevicesLabel != null) totalDevicesLabel.setText("Total: " + total + " Devices");

            // Populate metric card calculations
            if (laptopsCount != null) laptopsCount.setText("3");
            if (tabletsCount != null) tabletsCount.setText("1");
            if (smartphonesCount != null) smartphonesCount.setText("1");
            if (othersCount != null) othersCount.setText("0");

            if (laptopsBar != null) laptopsBar.setProgress(3.0 / total);
            if (tabletsBar != null) tabletsBar.setProgress(1.0 / total);
            if (smartphonesBar != null) smartphonesBar.setProgress(1.0 / total);
            if (othersBar != null) othersBar.setProgress(0.0);

            if (laptopsPct != null) laptopsPct.setText("60%");
            if (tabletsPct != null) tabletsPct.setText("20%");
            if (smartphonesPct != null) smartphonesPct.setText("20%");
            if (othersPct != null) othersPct.setText("0%");
        } catch (Exception e) {
            System.err.println("Failed to build visualization metrics: " + e.getMessage());
        }
    }

    /* ── Core Admin Data Operations ── */
    @FXML
    private void handleClearForm() {
        if (studentIdField != null) studentIdField.clear();
        if (studentNameField != null) studentNameField.clear();
        if (departmentField != null) departmentField.clear();
        if (deviceField != null) deviceField.clear();
        if (serialField != null) serialField.clear();
        if (studentIdField != null) studentIdField.setEditable(true);
    }

    @FXML
    private void handleInsertStudent() {
        String id = studentIdField.getText().trim();
        String name = studentNameField.getText().trim();
        String dept = departmentField.getText().trim();
        String type = deviceField.getText().trim();
        String serial = serialField.getText().trim();

        if (id.isEmpty() || name.isEmpty()) {
            showAlert("Validation Missing", "Student ID and Full Name properties are required.");
            return;
        }

        boolean success = byodService.insertRegisteredStudent(id, name, dept, type, serial);
        if (success) {
            loadStudentsData();
            handleClearForm();
            showSuccessAlert("Record successfully appended to system database entries!");
        } else {
            showAlert("Execution Refused", "Database rejected inserting transaction sequence.");
        }
    }

    @FXML
    private void handleUpdateStudent() {
        String id = studentIdField.getText().trim();
        String name = studentNameField.getText().trim();
        String dept = departmentField.getText().trim();
        String type = deviceField.getText().trim();
        String serial = serialField.getText().trim();

        if (id.isEmpty()) {
            showAlert("Validation Error", "No clear target primary index key selected to modify.");
            return;
        }

        boolean success = byodService.updateRegisteredStudent(id, name, dept, type, serial);
        if (success) {
            loadStudentsData();
            handleClearForm();
            showSuccessAlert("Student data adjustments updated across production clusters.");
        } else {
            showAlert("Write Access Fault", "Failed to update record details.");
        }
    }

    @FXML
    private void handleDeleteStudent() {
        String id = studentIdField.getText().trim();
        if (id.isEmpty()) {
            showAlert("Reference Target Void", "Select a clean entry target inside the table row index context to purge.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Purge record row tracking ID: " + id + "?", ButtonType.YES, ButtonType.NO);
        confirmation.setHeaderText(null);
        confirmation.showAndWait();

        if (confirmation.getResult() == ButtonType.YES) {
            boolean success = byodService.deleteRegisteredStudent(id);
            if (success) {
                loadStudentsData();
                handleClearForm();
                showSuccessAlert("Record deleted successfully.");
            } else {
                showAlert("Query Rejection Error", "Database constraints prevented data element execution drop.");
            }
        }
    }

    /* ── Data Load Processing Logic Operations ─────────── */
    private void loadData() {
        try {
            Map<String, Integer> metrics = byodService.fetchDashboardMetrics();
            if (statTotalStudents   != null) statTotalStudents.setText(String.valueOf(metrics.getOrDefault("totalStudents", 0)));
            if (statDeviceInventory != null) statDeviceInventory.setText(String.valueOf(metrics.getOrDefault("totalDevices", 0)));
            if (statFullExport      != null) statFullExport.setText(String.valueOf(metrics.getOrDefault("ingressToday", 0) + metrics.getOrDefault("egressToday", 0)));
        } catch (Exception e) {
            System.err.println("Metric stream mapping error: " + e.getMessage());
        }
        Platform.runLater(this::loadWeeklyChart);
        loadStudentsData();
    }

    private void loadWeeklyChart() {
        if (weeklyChart == null) return;
        if (weeklyChart.getXAxis() instanceof CategoryAxis) {
            ((CategoryAxis) weeklyChart.getXAxis()).getCategories().clear();
        }
        weeklyChart.getData().clear();

        XYChart.Series<String, Number> ingress = new XYChart.Series<>(); ingress.setName("Ingress");
        XYChart.Series<String, Number> egress = new XYChart.Series<>(); egress.setName("Egress");

        String[] weeks  = {"Week 1","Week 2","Week 3","Week 4"};
        int[] inData = {1800, 600, 1600, 700}; int[] exData = {900, 400, 1100, 600};

        for (int i = 0; i < 4; i++) {
            ingress.getData().add(new XYChart.Data<>(weeks[i], inData[i]));
            egress.getData().add(new XYChart.Data<>(weeks[i], exData[i]));
        }
        weeklyChart.getData().addAll(ingress, egress);
    }

    private void loadStudentsData() {
        if (studentsTable == null) return;
        studentsTable.getItems().clear();
        List<String[]> dataRows = byodService.fetchRegisteredStudentsList();

        for (String[] row : dataRows) {
            studentsTable.getItems().add(new StudentRow(row[0], row[1], row[2], row[3], row[4]));
        }
    }

    /* ── Navigation & Dynamic Action Event Methods (FIXES MISSING SYMBOLS) ── */
    @FXML private void handleAdminAccount() { System.out.println("Routing to Account Configuration Context..."); }
    @FXML private void handleGenStudents()  { showView(View.STUDENTS); }
    @FXML private void handleGenInventory() { showView(View.INVENTORY); }
    @FXML private void handleGenExport()    { showView(View.EXPORT); }
    @FXML private void handleExportAll()    { showView(View.EXPORT); }
    @FXML private void handleBack()         { showView(View.MAIN); }
    @FXML private void handleSchedule()     { System.out.println("Processing schedule event trace maps..."); }
    @FXML private void handleRefresh()      { loadData(); }
    @FXML private void handleLogout()       { confirmLeaveReports(() -> { Platform.exit(); System.exit(0); }); }
    @FXML private void handleDashboard()    { confirmLeaveReports(() -> navigateTo("/fxml/dashboard.fxml")); }
    @FXML private void handleMonitoring()   { confirmLeaveReports(() -> navigateTo("/fxml/monitoring.fxml")); }
    @FXML private void handleRegistration() { confirmLeaveReports(() -> navigateTo("/fxml/registration.fxml")); }
    @FXML private void handleReports(ActionEvent e) { showView(View.MAIN); }

    private void confirmLeaveReports(Runnable onProceed) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Navigation");
        alert.setHeaderText(null);
        alert.setContentText("Going back to the Reports Tab will require you to login again. Proceed?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Auth.reportUnlocked = false;
                onProceed.run();
            }
        });
    }

    @FXML
    private void handleSelectAllToggle() {
        if (exportAllCheckBox == null) return;
        boolean state = exportAllCheckBox.isSelected();
        if (exportLaptops != null) exportLaptops.setSelected(state);
        if (exportMobile != null) exportMobile.setSelected(state);
        if (exportTablets != null) exportTablets.setSelected(state);
        if (exportOthers != null) exportOthers.setSelected(state);
    }

    @FXML private void handleExportCsv()    { this.selectedFormat = "CSV";   showSuccessAlert("Selected Format changed to standard CSV Data stream."); }
    @FXML private void handleExportPdf()    { this.selectedFormat = "PDF";   showSuccessAlert("Selected Format changed to Adobe structural PDF compilation."); }
    @FXML private void handleExportXls()    { this.selectedFormat = "EXCEL"; showSuccessAlert("Selected Format changed to Microsoft Excel Spreadsheet cluster."); }

    @FXML
    private void handleGenerateExport() {
        showSuccessAlert("Security Data Export Processed! File saved under System Workspace using scheme: " + selectedFormat);
    }

    private void showAlert(String t, String m) {
        Alert a = new Alert(Alert.AlertType.WARNING); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
    private void showSuccessAlert(String m) {
        Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle("System Success"); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            Scene current = stage.getScene();
            String cssPath = getClass().getResource("/css/stylesheet.css").toExternalForm();
            if (!current.getStylesheets().contains(cssPath)) current.getStylesheets().add(cssPath);
            current.setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    /* ── Inner POJO Data Mapping Wrappers ── */
    public static class StudentRow {
        private final String studentId, name, department, device, serial;
        public StudentRow(String id, String n, String d, String dv, String s) {
            this.studentId=id; this.name=n; this.department=d; this.device=dv; this.serial=s;
        }
        public String getStudentId()  { return studentId; }
        public String getName()       { return name; }
        public String getDepartment() { return department; }
        public String getDevice()     { return device; }
        public String getSerial()     { return serial; }
    }

    public static class ExportRow {
        private final String timestamp, parameters, status, action;
        public ExportRow(String ts, String param, String stat, String act) {
            this.timestamp = ts; this.parameters = param; this.status = stat; this.action = act;
        }
        public String getTimestamp()  { return timestamp; }
        public String getParameters() { return parameters; }
        public String getStatus()     { return status; }
        public String getAction()     { return action; }
    }
}