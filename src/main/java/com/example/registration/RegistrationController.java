package com.example.registration;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.example.service.BYODService;

public class RegistrationController {

    @FXML private Button anchorButton;
    @FXML private Label stepStudentInfo;
    @FXML private Label stepDeviceDetails;
    @FXML private Label stepReview;

    @FXML private VBox stepPanel1;
    @FXML private VBox stepPanel2;
    @FXML private VBox stepPanel3;

    @FXML private TextField lastNameField;
    @FXML private TextField firstNameField;
    @FXML private TextField studentIdField;
    @FXML private TextField yearSectionField;
    @FXML private TextField courseField;
    @FXML private TextField contactField;

    @FXML private ComboBox<String> deviceTypeCombo;
    @FXML private TextField        brandModelField;
    @FXML private TextField        colorDescField;

    @FXML private VBox  savedDevicesBox;
    @FXML private Label savedDevicesTitle;
    @FXML private VBox  savedDevicesList;

    @FXML private Label reviewLastName;
    @FXML private Label reviewFirstName;
    @FXML private Label reviewStudentId;
    @FXML private Label reviewYearSection;
    @FXML private Label reviewCourse;
    @FXML private Label reviewContact;
    @FXML private Label reviewDevicesTitle;
    @FXML private VBox  reviewDevicesList;

    @FXML private Button backBtn;
    @FXML private Button addAnotherDevBtn;
    @FXML private Button nextStepBtn;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Label  formIdLabel;

    private static final int TOTAL_STEPS = 3;
    private int currentStep = 1;
    private boolean step1Completed = false;
    private boolean step2Completed = false;

    private static class DeviceEntry {
        String type, brand, color;
        DeviceEntry(String t, String b, String c) { type=t; brand=b; color=c; }
        static String nvl(String s) { return (s==null||s.isBlank()) ? "—" : s; }
        @Override public String toString() {
            return nvl(type) + "  ·  " + nvl(brand) + "  ·  " + nvl(color);
        }
    }
    private final List<DeviceEntry> savedDevices = new ArrayList<>();

    @FXML
    public void initialize() {
        if (formIdLabel != null)
            formIdLabel.setText("Form ID: BYOD-2026-" + String.format("%05d", (int)(Math.random()*99999)));
        if (deviceTypeCombo != null)
            deviceTypeCombo.getItems().addAll("Laptop", "Tablet", "Smartphone", "Desktop", "Other");
        showStep(1);
    }

    private void showStep(int step) {
        currentStep = step;
        setPanel(stepPanel1, step == 1);
        setPanel(stepPanel2, step == 2);
        setPanel(stepPanel3, step == 3);

        setBtn(backBtn,          step > 1);
        setBtn(addAnotherDevBtn, step == 2);
        setBtn(nextStepBtn,      step < TOTAL_STEPS);
        setBtn(saveBtn,          step == TOTAL_STEPS);
        setBtn(cancelBtn,        step == TOTAL_STEPS);
        updateDots(step);
    }

    private void setPanel(Pane p, boolean show) {
        if (p == null) return;
        p.setVisible(show);
        p.setManaged(show);
    }

    private void setBtn(Button b, boolean show) {
        if (b == null) return;
        b.setVisible(show);
        b.setManaged(show);
    }

    private void updateDots(int active) {
        Label[] dots = { stepStudentInfo, stepDeviceDetails, stepReview };
        for (int i = 0; i < dots.length; i++) {
            Label d = dots[i];
            if (d == null) continue;
            d.getStyleClass().remove("step-active");
            d.getStyleClass().remove("step-complete");
            d.getStyleClass().remove("step-inactive");

            if (i + 1 < active) {
                boolean completed = (i == 0 && step1Completed) || (i == 1 && step2Completed);
                if (completed) {
                    d.setText("✔");
                    d.getStyleClass().add("step-complete");
                } else {
                    d.setText(String.valueOf(i+1));
                    d.getStyleClass().add("step-inactive");
                }
            } else if (i + 1 == active) {
                d.setText(String.valueOf(i+1));
                d.getStyleClass().add("step-active");
            } else {
                d.setText(String.valueOf(i+1));
                d.getStyleClass().add("step-inactive");
            }
        }
    }

    private boolean isStep1Valid() {
        String lastName = lastNameField != null ? lastNameField.getText().trim() : "";
        String firstName = firstNameField != null ? firstNameField.getText().trim() : "";
        String studentId = studentIdField != null ? studentIdField.getText().trim() : "";
        if (lastName.isEmpty() || firstName.isEmpty() || studentId.isEmpty()) {
            showAlert("Missing Information", "Please fill in Last name, First name and Student ID.");
            return false;
        }
        return true;
    }

    private boolean isStep2Valid() {
        if (savedDevices.isEmpty()) {
            showAlert("No Device Added", "Please add at least one device before continuing.");
            return false;
        }
        return true;
    }

    @FXML
    private void handleBack() {
        if (currentStep > 1) showStep(currentStep - 1);
    }

    @FXML
    private void handleAddAnotherDevice() {
        String type  = deviceTypeCombo != null ? deviceTypeCombo.getValue() : null;
        String brand = brandModelField  != null ? brandModelField.getText().trim()   : "";
        String color = colorDescField   != null ? colorDescField.getText().trim()    : "";

        if (type == null || type.isBlank()) {
            showAlert("Missing Device Type", "Please select a device type before adding.");
            return;
        }
        savedDevices.add(new DeviceEntry(type, brand, color));
        refreshSavedDevicesUI();

        if (deviceTypeCombo != null) deviceTypeCombo.setValue(null);
        if (brandModelField  != null) brandModelField.clear();
        if (colorDescField   != null) colorDescField.clear();
        if (deviceTypeCombo  != null) deviceTypeCombo.requestFocus();
    }

    private void refreshSavedDevicesUI() {
        if (savedDevicesBox == null || savedDevicesList == null) return;
        savedDevicesBox.setVisible(!savedDevices.isEmpty());
        savedDevicesBox.setManaged(!savedDevices.isEmpty());
        if (savedDevicesTitle != null)
            savedDevicesTitle.setText("Saved devices (" + savedDevices.size() + ")");

        savedDevicesList.getChildren().clear();
        for (int i = 0; i < savedDevices.size(); i++) {
            DeviceEntry e = savedDevices.get(i);
            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:#f7f3ec;-fx-background-radius:6;-fx-padding:8 12;");

            Label num  = new Label((i+1) + ".");
            num.setStyle("-fx-font-weight:700;-fx-text-fill:#888;-fx-min-width:20;");

            Label info = new Label(e.toString());
            info.setStyle("-fx-text-fill:#333;");
            HBox.setHgrow(info, Priority.ALWAYS);

            final int idx = i;
            Button rm = new Button("✕");
            rm.setStyle("-fx-background-color:transparent;-fx-text-fill:#c0392b;-fx-cursor:hand;-fx-font-weight:700;");
            rm.setOnAction(ev -> {
                savedDevices.remove(idx);
                refreshSavedDevicesUI();
                if (savedDevices.isEmpty()) {
                    step2Completed = false;
                    updateDots(currentStep);
                }
            });

            row.getChildren().addAll(num, info, rm);
            savedDevicesList.getChildren().add(row);
        }
    }

    @FXML
    private void handleNextStep() {
        if (currentStep == 1) {
            if (!isStep1Valid()) return;
            step1Completed = true;
            showStep(2);
        } else if (currentStep == 2) {
            String type  = deviceTypeCombo != null ? deviceTypeCombo.getValue() : null;
            String brand = brandModelField  != null ? brandModelField.getText().trim()   : "";
            String color = colorDescField   != null ? colorDescField.getText().trim()    : "";
            if (type != null && !type.isBlank()) {
                savedDevices.add(new DeviceEntry(type, brand, color));
                refreshSavedDevicesUI();
            }
            if (!isStep2Valid()) return;
            step2Completed = true;
            populateReview();
            showStep(3);
        }
    }

    private void populateReview() {
        set(reviewLastName,    lastNameField);
        set(reviewFirstName,   firstNameField);
        set(reviewStudentId,   studentIdField);
        set(reviewYearSection, yearSectionField);
        set(reviewCourse,      courseField);
        set(reviewContact,     contactField);

        if (reviewDevicesTitle != null)
            reviewDevicesTitle.setText("Registered Devices (" + savedDevices.size() + ")");

        if (reviewDevicesList != null) {
            reviewDevicesList.getChildren().clear();
            for (int i = 0; i < savedDevices.size(); i++) {
                DeviceEntry e = savedDevices.get(i);
                VBox card = new VBox(4);
                card.setStyle("-fx-background-color:#f7f3ec;-fx-background-radius:6;-fx-padding:10 14;");

                Label title = new Label("Device " + (i+1) + " — " + DeviceEntry.nvl(e.type));
                title.setStyle("-fx-font-weight:700;-fx-text-fill:#333;");

                Label detail = new Label("Brand/Model: " + DeviceEntry.nvl(e.brand) + "    Color: " + DeviceEntry.nvl(e.color));
                detail.setStyle("-fx-text-fill:#666;-fx-font-size:12;");

                card.getChildren().addAll(title, detail);
                reviewDevicesList.getChildren().add(card);
            }
        }
    }

    private void set(Label lbl, TextField tf) {
        if (lbl != null && tf != null)
            lbl.setText(tf.getText().trim().isEmpty() ? "-" : tf.getText().trim());
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private final BYODService byodService = new BYODService();

    @FXML
    private void handleSave() {
        String sid = studentIdField.getText().trim();
        String validation = byodService.validate(sid, firstNameField.getText().trim(),
                lastNameField.getText().trim(), contactField.getText().trim(),
                yearSectionField.getText().trim());

        if (!validation.equals("VALID")) {
            showAlert("Invalid Input", validation);
            return;
        }

        try {
            for (DeviceEntry d : savedDevices) {
                byodService.registerStudent(
                        sid,
                        lastNameField.getText().trim(),
                        firstNameField.getText().trim(),
                        yearSectionField.getText().trim(),
                        courseField.getText().trim(),
                        contactField.getText().trim(),
                        d.type,
                        d.brand,
                        d.color
                );
            }

            String payload = String.join("|", sid, lastNameField.getText(), firstNameField.getText(),
                    yearSectionField.getText(), courseField.getText(), contactField.getText());
            String qrPath = byodService.generateQR(payload, sid, System.getProperty("user.dir"));

            // HOOKED UP: Spawns the clean side-by-side aesthetic instructions + layout QR window
            Stage activeStage = (Stage) anchorButton.getScene().getWindow();
            com.example.monitoring.QRRegistrationSuccessWindow.show(activeStage, sid, qrPath);

            // Navigate back to tracking list dashboard logs automatically
            navigateTo("/fxml/monitoring.fxml");

        } catch (Exception ex) {
            showAlert("Database Error", ex.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        navigateTo("/fxml/monitoring.fxml");
    }

    @FXML private void handleDashboard()    { navigateTo("/fxml/dashboard.fxml"); }
    @FXML private void handleMonitoring()   { navigateTo("/fxml/monitoring.fxml"); }
    @FXML private void handleReports()      { requireLoginThenOpenReports(); }

    private void requireLoginThenOpenReports() {
        // Reuse DashboardController's static pending-action so LoginController
        // knows to open Reports (and fire Export if needed) after a successful login.
        try {
            java.lang.reflect.Field f = com.example.dashboard.DashboardController.class
                    .getDeclaredField("pendingAction");
            f.setAccessible(true);
            f.set(null, com.example.dashboard.DashboardController.PendingAction.REPORTS);
        } catch (Exception ignored) {}

        try {
            FXMLLoader loginLoader = new FXMLLoader(
                    getClass().getResource("/fxml/login.fxml"));
            Parent loginRoot = loginLoader.load();
            Scene loginScene = new Scene(loginRoot);
            loginScene.getStylesheets().add(
                    getClass().getResource("/css/stylesheet.css").toExternalForm());

            // Tell LoginController which stage to replace with Reports on success
            Stage currentStage = (Stage) anchorButton.getScene().getWindow();
            com.example.login.LoginController.setTargetStage(currentStage);

            Stage loginStage = new Stage();
            loginStage.setScene(loginScene);
            loginStage.setTitle("BYOD Monitoring System - Login Required");
            loginStage.setResizable(false);
            loginStage.centerOnScreen();
            loginStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML private void handleRegistration() { /* already here */ }

    private void navigateTo(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml.toLowerCase()));
            Stage stage = (Stage) anchorButton.getScene().getWindow();
            Scene current = stage.getScene();
            current.getStylesheets().clear();
            current.getStylesheets().add(getClass().getResource("/css/stylesheet.css").toExternalForm());
            current.setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}