package com.example.monitoring;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class QRRegistrationSuccessWindow {

    public static void show(Stage ownerStage, String studentId, String qrFilePath) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.WINDOW_MODAL);
        popupStage.initOwner(ownerStage);
        popupStage.setTitle("Registration Successful");
        popupStage.setResizable(false);

        // --- LEFT COLUMN: Message and Instructions ---
        Label headerLabel = new Label("🎉 Success!");
        headerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");

        Label messageLabel = new Label(
                "Device registered successfully!\n\n" +
                        "You may now enter the campus.\n" +
                        "Please take a clear photo of this QR code. \n" +
                        "and scan it when exiting the campus."
        );
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #333333; -fx-line-spacing: 5px;");

        Label idLabel = new Label("Student ID: " + studentId);
        idLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #666666;");

        Button closeButton = new Button("Done & Close");
        closeButton.setStyle(
                "-fx-background-color: #2e7d32; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 10 20 10 20; " +
                        "-fx-background-radius: 5; " +
                        "-fx-cursor: hand;"
        );
        closeButton.setOnAction(e -> popupStage.close());

        VBox leftColumn = new VBox(20, headerLabel, messageLabel, idLabel, closeButton);
        leftColumn.setAlignment(Pos.CENTER_LEFT);
        leftColumn.setPrefWidth(320);

        // --- RIGHT COLUMN: QR Code Container ---
        ImageView qrImageView = new ImageView();
        qrImageView.setFitWidth(260);
        qrImageView.setFitHeight(260);
        qrImageView.setPreserveRatio(true);

        try {
            File file = new File(qrFilePath);
            if (file.exists()) {
                Image qrImage = new Image(file.toURI().toString());
                qrImageView.setImage(qrImage);
            }
        } catch (Exception ex) {
            System.err.println("Failed to render QR image element: " + ex.getMessage());
        }

        VBox rightColumn = new VBox(qrImageView);
        rightColumn.setAlignment(Pos.CENTER);
        rightColumn.setStyle(
                "-fx-background-color: #ffffff; " +
                        "-fx-padding: 15; " +
                        "-fx-border-color: #e0e0e0; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8;"
        );

        // --- MAIN LAYOUT COMBINATION ---
        HBox mainLayout = new HBox(30, leftColumn, rightColumn);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setStyle("-fx-background-color: #fcfbf7;"); // Lighter aesthetic tint

        Scene scene = new Scene(mainLayout, 680, 340);
        popupStage.setScene(scene);
        popupStage.centerOnScreen();
        popupStage.showAndWait();
    }
}