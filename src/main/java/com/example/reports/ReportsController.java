package com.example.reports;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;

public class ReportsController {

    /* ── Navbar ─────────────────────────────────────────── */
    @FXML private Button logoutButton;
    @FXML private Button scheduleBtn;
    @FXML private Button exportAllBtn;

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

    /* ── Inventory view ─────────────────────────────────── */
    @FXML private Label totalDevicesLabel;
    @FXML private ProgressBar laptopsBar, tabletsBar, smartphonesBar, othersBar;
    @FXML private Label laptopsPct, tabletsPct, smartphonesPct, othersPct;
    @FXML private Label laptopsCount, tabletsCount, smartphonesCount, othersCount;

    /* ── Students view ──────────────────────────────────── */
    @FXML private Label panelTotalStudents, panelNewStudents, panelActiveDevices, panelTotalStudents2;
    @FXML private TableView<StudentRow> studentsTable;
    @FXML private TableColumn<StudentRow, String> colName, colStudentId, colDepartment, colDevice, colSerial;

    /* ── Export view ────────────────────────────────────── */
    @FXML private DatePicker exportFromDate, exportToDate;
    @FXML private TextField exportCourseField;
    @FXML private CheckBox exportLaptops, exportTablets, exportMobile, exportOthers;
    @FXML private Button exportCsvBtn, exportPdfBtn, exportXlsBtn, generateExportBtn;
    @FXML private TableView<ExportRow> exportsTable;
    @FXML private TableColumn<ExportRow, String> colExpId, colExpParams, colExpStatus, colExpAction;

    private enum View { MAIN, EXPORT, INVENTORY, STUDENTS }

    /* ════════════════════════════════════════════════════ */

    @FXML
    public void initialize() {
        if (periodCombo != null) {
            periodCombo.getItems().addAll("This Month", "Last Month", "Last 3 Months", "This Year");
            periodCombo.setValue("This Month");
        }
        setupTables();
        loadData();
        showView(View.MAIN);
    }

    /* ── Switch which full-page view is visible ─────────── */
    private void showView(View v) {
        setVisible(mainView,      v == View.MAIN);
        setVisible(exportView,    v == View.EXPORT);
        setVisible(inventoryView, v == View.INVENTORY);
        setVisible(studentsView,  v == View.STUDENTS);
    }

    private void setVisible(ScrollPane pane, boolean show) {
        if (pane == null) return;
        pane.setVisible(show);
        pane.setManaged(show);
    }

    /* ── Data loading ───────────────────────────────────── */
    private void loadData() {
        if (statTotalStudents   != null) statTotalStudents.setText("2680");
        if (statDeviceInventory != null) statDeviceInventory.setText("256");
        if (statFullExport      != null) statFullExport.setText("2313");
        // Defer chart loading — axes must be laid out before data can be added
        Platform.runLater(this::loadWeeklyChart);
        loadInventoryData();
        loadStudentsData();
        loadExportsData();
    }

    private void loadWeeklyChart() {
        if (weeklyChart == null) return;
        weeklyChart.getData().clear();
        weeklyChart.setLegendVisible(true);

        XYChart.Series<String, Number> ingress = new XYChart.Series<>();
        ingress.setName("Ingress");
        XYChart.Series<String, Number> egress = new XYChart.Series<>();
        egress.setName("Egress");

        String[] weeks  = {"Week 1","Week 2","Week 3","Week 4"};
        int[]    inData = {1800, 600, 1600, 700};
        int[]    exData = {900,  400, 1100, 600};

        for (int i = 0; i < 4; i++) {
            ingress.getData().add(new XYChart.Data<>(weeks[i], inData[i]));
            egress.getData().add(new XYChart.Data<>(weeks[i], exData[i]));
        }
        weeklyChart.getData().addAll(ingress, egress);
    }

    private void loadInventoryData() {
        int total = 2628, lap = 985, tab = 667, pho = 667, oth = 309;
        if (totalDevicesLabel != null) totalDevicesLabel.setText("Total: " + total + " Devices");
        setBar(laptopsBar,     laptopsPct,     laptopsCount,     lap, total, "58%", "985");
        setBar(tabletsBar,     tabletsPct,     tabletsCount,     tab, total, "42%", "667");
        setBar(smartphonesBar, smartphonesPct, smartphonesCount, pho, total, "67%", "667");
        setBar(othersBar,      othersPct,      othersCount,      oth, total, "20%", "667");
    }

    private void setBar(ProgressBar bar, Label pct, Label count, int val, int total, String pctStr, String countStr) {
        if (bar   != null) bar.setProgress((double) val / total);
        if (pct   != null) pct.setText(pctStr);
        if (count != null) count.setText(countStr);
    }

    private void loadStudentsData() {
        if (studentsTable == null) return;
        if (panelTotalStudents  != null) panelTotalStudents.setText("2,523");
        if (panelNewStudents    != null) panelNewStudents.setText("345");
        if (panelActiveDevices  != null) panelActiveDevices.setText("521");
        if (panelTotalStudents2 != null) panelTotalStudents2.setText("2,523");

        studentsTable.getItems().setAll(
            new StudentRow("John Michael Santos",  "2024-00125", "Information Technology",  "Laptop", "ASUS-G713QE-001"),
            new StudentRow("Maria Angela Cruz",    "2024-00126", "Computer Science",        "Laptop", "DELL-5420-145"),
            new StudentRow("Kevin Louis Reyes",    "2024-00127", "Business Administration", "Laptop", "HP-ELITE-552"),
            new StudentRow("Samantha Joy Lim",     "2024-00128", "Engineering Technology",  "Laptop", "LENOVO-T14-882"),
            new StudentRow("Daniel Castro",        "2024-00129", "Information Technology",  "Laptop", "ACER-A515-771"),
            new StudentRow("Patricia Anne Gomez",  "2024-00130", "Computer Science",        "Laptop", "MACBOOK-M2-110")
        );
    }

    private void loadExportsData() {
        if (exportsTable == null) return;
        exportsTable.getItems().setAll(
            new ExportRow("#EXP-99231", "05/01/2026 14:22", "Q1 2024 Audit CSV\nAll Depts | Laptops", "Processing"),
            new ExportRow("#EXP-99231", "05/01/2026 14:22", "Q1 2024 Audit CSV\nAll Depts | Laptops", "Done"),
            new ExportRow("#EXP-99231", "05/01/2026 14:22", "Q1 2024 Audit CSV\nAll Depts | Laptops", "Processing"),
            new ExportRow("#EXP-99231", "05/01/2026 14:22", "Q1 2024 Audit CSV\nAll Depts | Laptops", "Processing"),
            new ExportRow("#EXP-99231", "05/01/2026 14:22", "Q1 2024 Audit CSV\nAll Depts | Laptops", "Processing")
        );
    }

    private void setupTables() {
        if (studentsTable != null) {
            colName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
            colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
            colDevice.setCellValueFactory(new PropertyValueFactory<>("device"));
            colSerial.setCellValueFactory(new PropertyValueFactory<>("serial"));
            studentsTable.setPlaceholder(new Label("No data."));
        }
        if (exportsTable != null) {
            colExpId.setCellValueFactory(new PropertyValueFactory<>("expId"));
            colExpParams.setCellValueFactory(new PropertyValueFactory<>("params"));
            colExpStatus.setCellValueFactory(c -> {
                SimpleStringProperty p = new SimpleStringProperty();
                p.set(c.getValue().getStatus());
                return p;
            });
            // Status badge cell
            colExpStatus.setCellFactory(col -> new TableCell<ExportRow, String>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setGraphic(null); setText(null); return; }
                    Label badge = new Label(item);
                    badge.setStyle("Done".equals(item)
                        ? "-fx-background-color:transparent;-fx-text-fill:#62b86f;-fx-font-weight:700;"
                        : "-fx-background-color:#f5f0e8;-fx-text-fill:#888;-fx-background-radius:6;-fx-padding:4 10;");
                    setGraphic(badge);
                    setText(null);
                }
            });
            if (colExpAction != null) {
                colExpAction.setCellFactory(col -> new TableCell<ExportRow, String>() {
                    @Override protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) { setGraphic(null); return; }
                        Button dl = new Button("⬇");
                        dl.setStyle("-fx-background-color:transparent;-fx-font-size:16;-fx-cursor:hand;");
                        setGraphic(dl);
                    }
                });
            }
        }
    }

    /* ── Generate & action handlers ─────────────────────── */
    @FXML private void handleGenStudents()  { showView(View.STUDENTS); }
    @FXML private void handleGenInventory() { showView(View.INVENTORY); }
    @FXML private void handleGenExport()    { showView(View.EXPORT); }
    @FXML private void handleExportAll()    { showView(View.EXPORT); }
    @FXML private void handleBack()         { showView(View.MAIN); }
    @FXML private void handleSchedule()     { System.out.println("Schedule clicked"); }
    @FXML private void handleRefresh()      { loadData(); }

    @FXML private void handleGenerateExport() {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Export Queued"); a.setHeaderText(null);
        a.setContentText("Secure export has been queued successfully!"); a.showAndWait();
    }

    @FXML private void handleExportCsv() { selectFormat(exportCsvBtn); }
    @FXML private void handleExportPdf() { selectFormat(exportPdfBtn); }
    @FXML private void handleExportXls() { selectFormat(exportXlsBtn); }

    private void selectFormat(Button chosen) {
        for (Button b : new Button[]{exportCsvBtn, exportPdfBtn, exportXlsBtn}) {
            if (b != null) b.getStyleClass().remove("format-selected");
        }
        if (chosen != null) chosen.getStyleClass().add("format-selected");
    }

    /* ── Navigation ─────────────────────────────────────── */
    @FXML private void handleLogout()       { navigateTo("/fxml/login.fxml"); }
    @FXML private void handleDashboard()    { navigateTo("/fxml/dashboard.fxml"); }
    @FXML private void handleMonitoring()   { navigateTo("/fxml/monitoring.fxml"); }
    @FXML private void handleRegistration() { navigateTo("/fxml/registration.fxml"); }
    @FXML private void handleReports()      { showView(View.MAIN); }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            Scene current = stage.getScene();
            current.getStylesheets().clear();
            current.getStylesheets().add(getClass().getResource("/css/stylesheet.css").toExternalForm());
            current.setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    /* ── Row models ─────────────────────────────────────── */
    public static class StudentRow {
        private final String name, studentId, department, device, serial;
        public StudentRow(String n, String i, String d, String dv, String s) {
            name=n; studentId=i; department=d; device=dv; serial=s;
        }
        public String getName()       { return name; }
        public String getStudentId()  { return studentId; }
        public String getDepartment() { return department; }
        public String getDevice()     { return device; }
        public String getSerial()     { return serial; }
    }

    public static class ExportRow {
        private final String expId, timestamp, params, status;
        public ExportRow(String id, String ts, String p, String s) {
            expId=id; timestamp=ts; params=p; status=s;
        }
        public String getExpId()     { return expId + "\n" + timestamp; }
        public String getParams()    { return params; }
        public String getStatus()    { return status; }
        public String getTimestamp() { return timestamp; }
    }
}
