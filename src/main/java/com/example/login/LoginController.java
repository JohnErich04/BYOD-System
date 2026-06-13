package com.example.login;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    private static final String STYLESHEET_PATH = "/css/stylesheet.css";
    private static final String DASHBOARD_FXML = "/fxml/dashboard.fxml";

    // Hardcoded credentials – replace with real authentication
    private static final String VALID_USERNAME = "admin";
    private static final String VALID_PASSWORD = "password";

    @FXML private ImageView logoImage;
    @FXML private Label appTitleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label usernameLabel;
    @FXML private TextField usernameField;
    @FXML private Label passwordLabel;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button cancelButton;

    @FXML
    public void initialize() {
        addStylesheetToScene();
        setupEventHandlers();
        setupInputValidation();
        Platform.runLater(() -> usernameField.requestFocus());
    }

    private void addStylesheetToScene() {
        Scene scene = usernameField.getScene();
        if (scene != null) {
            String css = getClass().getResource(STYLESHEET_PATH).toExternalForm();
            if (!scene.getStylesheets().contains(css)) {
                scene.getStylesheets().add(css);
            }
        } else {
            LOGGER.warning("Scene not available for stylesheet injection");
        }
    }

    private void setupEventHandlers() {
        loginButton.setOnAction(event -> handleLogin());
        cancelButton.setOnAction(event -> handleCancel());
        passwordField.setOnAction(event -> handleLogin());
        usernameField.setOnAction(event -> handleLogin());
    }

    private void setupInputValidation() {
        usernameField.textProperty().addListener((obs, old, newVal) ->
                usernameField.getStyleClass().remove("error-field"));
        passwordField.textProperty().addListener((obs, old, newVal) ->
                passwordField.getStyleClass().remove("error-field"));
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty()) {
            showError("Validation Error", "Invalid Input", "Please enter username.");
            usernameField.getStyleClass().add("error-field");
            usernameField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("Validation Error", "Invalid Input", "Please enter password.");
            passwordField.getStyleClass().add("error-field");
            passwordField.requestFocus();
            return;
        }

        if (authenticate(username, password)) {
            loginSuccess();
        } else {
            showError("Authentication Failed", "Login Error",
                    "Invalid username or password. Please try again.");
        }
    }

    private boolean authenticate(String username, String password) {
        return VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
    }

    private void loginSuccess() {
        clearErrorStyles();
        LOGGER.info("Login successful for user: " + usernameField.getText());

        Stage loginStage = (Stage) cancelButton.getScene().getWindow();
        loginStage.close();

        openDashboard();
    }

    private void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(DASHBOARD_FXML));
            Parent root = loader.load();

            Stage dashboardStage = new Stage();
            dashboardStage.setTitle("BYOD Monitoring System - Dashboard");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(STYLESHEET_PATH).toExternalForm());
            dashboardStage.setScene(scene);
            dashboardStage.centerOnScreen();
            dashboardStage.setMaximized(false);
            dashboardStage.setResizable(true);

            dashboardStage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load dashboard", e);
            showError("Loading Error", "Failed to Load Dashboard",
                    "Unable to load the main dashboard.\n\nDetails: " + e.getMessage());
        }
    }

    // Standard error alert (replaces custom dialog)
    private void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void clearErrorStyles() {
        usernameField.getStyleClass().remove("error-field");
        passwordField.getStyleClass().remove("error-field");
    }

    @FXML
    private void handleCancel() {
        usernameField.clear();
        passwordField.clear();
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}