package com.example.reports;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.awt.Color;

public class ReportsController {

    // Tracks if the admin is currently authenticated locally
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
    @FXML private CheckBox exportAllCheckBox;
    @FXML private Button exportCsvBtn, exportPdfBtn, exportXlsBtn, generateExportBtn;
    @FXML private TableView<ExportRow> exportsTable;
    @FXML private TableColumn<ExportRow, String> colExpId, colExpParams, colExpStatus, colExpAction;

    private enum View { MAIN, EXPORT, INVENTORY, STUDENTS }

    // Service layer instance connected to XAMPP MariaDB
    private final BYODService byodService = new BYODService();

    // Tracks the actively toggled format state
    private String selectedFormat = "CSV";

    /* ════════════════════════════════════════════════════ */

    @FXML
    public void initialize() {
        System.out.println("DEBUG: ReportsController initialized successfully!"); // Add this line
        if (periodCombo != null) {
            periodCombo.getItems().addAll("This Month", "Last Month", "Last 3 Months", "This Year");
            periodCombo.setValue("This Month");
        }

        if (reportsButton != null) {
            reportsButton.setOnAction(this::handleReports); // Wires the button to your method
            reportsButton.getStyleClass().add("active");
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
        try {
            Map<String, Integer> metrics = byodService.fetchDashboardMetrics();

            if (statTotalStudents   != null) statTotalStudents.setText(String.valueOf(metrics.getOrDefault("totalStudents", 0)));
            if (statDeviceInventory != null) statDeviceInventory.setText(String.valueOf(metrics.getOrDefault("totalDevices", 0)));
            if (statFullExport      != null) statFullExport.setText(String.valueOf(metrics.getOrDefault("ingressToday", 0) + metrics.getOrDefault("egressToday", 0)));
        } catch (Exception e) {
            System.err.println("Failed to load metrics in reports: " + e.getMessage());
        }

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
        Map<String, Integer> breakdown = byodService.fetchInventoryBreakdown();

        int lap = breakdown.getOrDefault("Laptop", 0);
        int tab = breakdown.getOrDefault("Tablet", 0);
        int pho = breakdown.getOrDefault("Smartphone", 0);
        int oth = breakdown.getOrDefault("Others", 0);

        int total = lap + tab + pho + oth;

        if (totalDevicesLabel != null) {
            totalDevicesLabel.setText("Total: " + total + " Devices");
        }

        if (total == 0) {
            setBar(laptopsBar,     laptopsPct,     laptopsCount,     0, 1, "0%", "0");
            setBar(tabletsBar,     tabletsPct,     tabletsCount,     0, 1, "0%", "0");
            setBar(smartphonesBar, smartphonesPct, smartphonesCount, 0, 1, "0%", "0");
            setBar(othersBar,      othersPct,      othersCount,      0, 1, "0%", "0");
            return;
        }

        String lapPctStr = String.format("%d%%", Math.round(((double) lap / total) * 100));
        String tabPctStr = String.format("%d%%", Math.round(((double) tab / total) * 100));
        String phoPctStr = String.format("%d%%", Math.round(((double) pho / total) * 100));
        String othPctStr = String.format("%d%%", Math.round(((double) oth / total) * 100));

        setBar(laptopsBar,     laptopsPct,     laptopsCount,     lap, total, lapPctStr, String.valueOf(lap));
        setBar(tabletsBar,     tabletsPct,     tabletsCount,     tab, total, tabPctStr, String.valueOf(tab));
        setBar(smartphonesBar, smartphonesPct, smartphonesCount, pho, total, phoPctStr, String.valueOf(pho));
        setBar(othersBar,      othersPct,      othersCount,      oth, total, othPctStr, String.valueOf(oth));
    }

    private void setBar(ProgressBar bar, Label pct, Label count, int val, int total, String pctStr, String countStr) {
        if (bar   != null) bar.setProgress((double) val / total);
        if (pct   != null) pct.setText(pctStr);
        if (count != null) count.setText(countStr);
    }

    private void loadStudentsData() {
        if (studentsTable == null) return;

        studentsTable.getItems().clear();
        List<String[]> dataRows = byodService.fetchRegisteredStudentsList();

        for (String[] row : dataRows) {
            studentsTable.getItems().add(new StudentRow(
                    row[0], // Full Name
                    row[1], // Student ID
                    row[2], // Course/Program
                    row[3], // Device Type
                    row[4]  // Brand Model
            ));
        }

        int totalCount = dataRows.size();
        if (panelTotalStudents  != null) panelTotalStudents.setText(String.format("%,d", totalCount));
        if (panelNewStudents    != null) panelNewStudents.setText(String.valueOf(totalCount));
        if (panelActiveDevices  != null) panelActiveDevices.setText(String.valueOf(totalCount));
        if (panelTotalStudents2 != null) panelTotalStudents2.setText(String.format("%,d", totalCount));
    }

    private void loadExportsData() {
        if (exportsTable == null) return;
        exportsTable.getItems().setAll(
                new ExportRow("#EXP-99231", "05/01/2026 14:22", "Q1 2026 Audit CSV\nAll Depts | Laptops", "Processing"),
                new ExportRow("#EXP-99232", "05/01/2026 14:25", "Q1 2026 Audit CSV\nAll Depts | Laptops", "Done")
        );
    }

    private void setupTables() {
        if (studentsTable != null) {
            colName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
            colDepartment.setCellValueFactory(new PropertyValueFactory<>("department"));
            colDevice.setCellValueFactory(new PropertyValueFactory<>("device"));
            colSerial.setCellValueFactory(new PropertyValueFactory<>("serial"));
            studentsTable.setPlaceholder(new Label("No registered data records available."));
        }
        if (exportsTable != null) {
            colExpId.setCellValueFactory(new PropertyValueFactory<>("expId"));
            colExpParams.setCellValueFactory(new PropertyValueFactory<>("params"));
            colExpStatus.setCellValueFactory(c -> {
                SimpleStringProperty p = new SimpleStringProperty();
                p.set(c.getValue().getStatus());
                return p;
            });
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

    @FXML
    private void handleSelectAllToggle() {
        boolean isSelected = exportAllCheckBox != null && exportAllCheckBox.isSelected();

        if (exportLaptops != null) exportLaptops.setSelected(isSelected);
        if (exportTablets != null) exportTablets.setSelected(isSelected);
        if (exportMobile  != null) exportMobile.setSelected(isSelected);
        if (exportOthers  != null) exportOthers.setSelected(isSelected);
    }

    @FXML private void handleExportCsv() { selectedFormat = "CSV"; selectFormat(exportCsvBtn); }
    @FXML private void handleExportPdf() { selectedFormat = "PDF"; selectFormat(exportPdfBtn); }
    @FXML private void handleExportXls() { selectedFormat = "XLS"; selectFormat(exportXlsBtn); }

    private void selectFormat(Button chosen) {
        for (Button b : new Button[]{exportCsvBtn, exportPdfBtn, exportXlsBtn}) {
            if (b != null) b.getStyleClass().remove("format-selected");
        }
        if (chosen != null) chosen.getStyleClass().add("format-selected");
    }

    /**
     * Captures UI check boxes, prompts the OS save window, and pipes out actual log files.
     */
    @FXML
    private void handleGenerateExport() {
        boolean lap = exportLaptops != null && exportLaptops.isSelected();
        boolean tab = exportTablets != null && exportTablets.isSelected();
        boolean mob = exportMobile != null && exportMobile.isSelected();
        boolean oth = exportOthers != null && exportOthers.isSelected();

        if (!lap && !tab && !mob && !oth) {
            lap = tab = mob = oth = true;
        }

        String courseFilter = exportCourseField != null ? exportCourseField.getText() : "";
        List<String[]> exportRows = byodService.fetchFilteredExportData(courseFilter, lap, tab, mob, oth);

        if (exportRows.isEmpty()) {
            showAlert("Export Notice", "No matching data rows found for the specified filters.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Secure Campus Export Log");

        if ("PDF".equalsIgnoreCase(selectedFormat)) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Portable Document Format (*.pdf)", "*.pdf"));
            fileChooser.setInitialFileName("BYOD_Campus_Report.pdf");
        } else if ("XLS".equalsIgnoreCase(selectedFormat)) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Spreadsheet (*.xls)", "*.xls"));
            fileChooser.setInitialFileName("BYOD_Campus_Report.xls");
        } else {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Comma Delimited (*.csv)", "*.csv"));
            fileChooser.setInitialFileName("BYOD_Campus_Report.csv");
        }

        File file = fileChooser.showSaveDialog(logoutButton.getScene().getWindow());

        if (file != null) {
            boolean success;
            if (file.getName().endsWith(".pdf")) {
                success = writePdfFile(file, exportRows);
            } else {
                success = writeCsvFile(file, exportRows);
            }

            if (success) {
                showSuccessAlert("Report generated and saved successfully to:\n" + file.getAbsolutePath());
                if (exportsTable != null) {
                    String cleanTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"));
                    String summaryText = String.format("Format: %s | Params: %s", selectedFormat, courseFilter.isBlank() ? "All Depts" : courseFilter);
                    exportsTable.getItems().add(0, new ExportRow("#EXP-" + (int)(Math.random() * 90000 + 10000), cleanTime, summaryText, "Done"));
                }
            } else {
                showAlert("Write File Failure", "Could not complete disk streaming loop access routines securely.");
            }
        }
    }

    private boolean writeCsvFile(File targetFile, List<String[]> recordDataset) {
        try (FileWriter writer = new FileWriter(targetFile)) {
            writer.write("Student ID,Full Name,Year & Section,Course/Program,Device Type,Brand & Model,Ingress Time,Egress Time\n");

            for (String[] rowData : recordDataset) {
                StringBuilder line = new StringBuilder();
                for (int i = 0; i < rowData.length; i++) {
                    String cell = rowData[i] == null ? "" : rowData[i];
                    if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
                        cell = "\"" + cell.replace("\"", "\"\"") + "\"";
                    }
                    line.append(cell);
                    if (i < rowData.length - 1) {
                        line.append(",");
                    }
                }
                writer.write(line.toString() + "\n");
            }
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean writePdfFile(File targetFile, List<String[]> recordDataset) {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, new FileOutputStream(targetFile));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

            document.add(new Paragraph("BYOD Security Tracker - Campus Report", titleFont));
            document.add(new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), metaFont));
            document.add(new Paragraph("Total Records: " + recordDataset.size() + "\n\n"));

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.2f, 2.0f, 1.0f, 1.2f, 1.2f, 1.8f, 1.8f, 1.8f});

            String[] headers = {"Student ID", "Full Name", "Sec", "Course", "Device", "Model", "Ingress", "Egress"};
            for (String headerText : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(headerText, headerFont));
                cell.setBackgroundColor(new Color(40, 44, 52));
                cell.setPadding(6);
                table.addCell(cell);
            }

            for (String[] rowData : recordDataset) {
                for (String field : rowData) {
                    PdfPCell cell = new PdfPCell(new Phrase(field != null ? field : "", bodyFont));
                    cell.setPadding(5);
                    table.addCell(cell);
                }
            }

            document.add(table);
            document.close();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            if (document.isOpen()) {
                document.close();
            }
            return false;
        }
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Status");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /* ── Helper: Modal Verification Window ──────────────── */
    private void showLoginPopup(String targetView) {
        try {
            // Double-check this path. Does /fxml/login.fxml exist in your project?
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage popupStage = new Stage();
            popupStage.setTitle("Admin Verification Required");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(logoutButton.getScene().getWindow());
            popupStage.setScene(new Scene(root));

            popupStage.showAndWait();

        } catch (Exception e) {
            // THIS WILL PRINT THE ERROR TO YOUR CONSOLE
            System.err.println("CRITICAL ERROR: Could not load /fxml/login.fxml");
            e.printStackTrace();
        }
    }

    /* ── Navigation ─────────────────────────────────────── */
    @FXML
    private void handleLogout() {
        if (!confirmLeaveReports()) {
            return;
        }
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        stage.close();
    }
    @FXML
    private void handleDashboard() {
        if (!confirmLeaveReports()) {
            return;
        }
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleMonitoring() {
        if (!confirmLeaveReports()) {
            return;
        }
        navigateTo("/fxml/monitoring.fxml");
    }

    @FXML
    private void handleRegistration() {
        if (!confirmLeaveReports()) {
            return;
        }
        navigateTo("/fxml/registration.fxml");
    }

    @FXML
    private void handleReports(ActionEvent event) {
        showView(View.MAIN);
    }

    private boolean confirmLeaveReports() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Leave Reports");
        alert.setHeaderText("You will be logged out of Reports");
        alert.setContentText("Leaving this page will end your Reports session. "
                + "You'll need to log in again to access Reports.\n\nProceed?");

        ButtonType proceedButton = new ButtonType("Proceed");
        ButtonType stayButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(proceedButton, stayButton);

        return alert.showAndWait().filter(b -> b == proceedButton).isPresent();
    }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            Scene current = stage.getScene();

            String cssPath = getClass().getResource("/css/stylesheet.css").toExternalForm();
            if (!current.getStylesheets().contains(cssPath)) {
                current.getStylesheets().add(cssPath);
            }
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