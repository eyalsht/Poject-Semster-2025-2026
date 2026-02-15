package controllers;

import client.GCMClient;
import common.content.GCMMap;
import common.dto.ContentChangeRequest;
import common.enums.ActionType;
import common.enums.ContentActionType;
import common.enums.ContentType;
import common.messaging.Message;
import common.user.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controller for the Import Map dialog.
 * Shows available external maps and allows importing them into GCM.
 */
public class ImportMapDialogController {

    @FXML private FlowPane flowPaneMaps;
    @FXML private ScrollPane scrollPane;
    @FXML private Label lblStatus;
    @FXML private Button btnClose;

    private final GCMClient client = GCMClient.getInstance();

    @FXML
    public void initialize() {
        loadExternalMaps();
    }

    private void loadExternalMaps() {
        new Thread(() -> {
            try {
                Message request = new Message(ActionType.GET_EXTERNAL_MAPS_REQUEST, null);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getAction() == ActionType.GET_EXTERNAL_MAPS_RESPONSE) {
                        @SuppressWarnings("unchecked")
                        List<GCMMap> maps = (List<GCMMap>) response.getMessage();
                        displayExternalMaps(maps);
                    } else {
                        lblStatus.setText("Failed to load external maps.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> lblStatus.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void displayExternalMaps(List<GCMMap> maps) {
        flowPaneMaps.getChildren().clear();

        if (maps == null || maps.isEmpty()) {
            lblStatus.setText("No external maps available for import.");
            return;
        }

        lblStatus.setText(maps.size() + " external map(s) available");

        for (GCMMap map : maps) {
            VBox card = createMapCard(map);
            flowPaneMaps.getChildren().add(card);
        }
    }

    private VBox createMapCard(GCMMap map) {
        VBox card = new VBox(5);
        card.setPrefWidth(200);
        card.setPrefHeight(150);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #3d566e; -fx-background-radius: 8; -fx-border-color: #4a6785; -fx-border-radius: 8;");

        Label nameLabel = new Label(map.getName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        nameLabel.setWrapText(true);

        String cityName = map.getCity() != null ? map.getCity().getName() : "Unknown";
        Label cityLabel = new Label("City: " + cityName);
        cityLabel.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 11px;");

        Label descLabel = new Label(map.getDescription() != null && !map.getDescription().isEmpty()
                ? map.getDescription() : "No description");
        descLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(40);

        // Spacer to push the button to the bottom
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button btnImport = new Button("Import");
        btnImport.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        btnImport.setMaxWidth(Double.MAX_VALUE);
        btnImport.setOnAction(e -> onImport(map, btnImport));

        HBox btnBox = new HBox(btnImport);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(5, 0, 0, 0));
        HBox.setHgrow(btnImport, javafx.scene.layout.Priority.ALWAYS);

        card.getChildren().addAll(nameLabel, cityLabel, descLabel, spacer, btnBox);
        return card;
    }

    private void onImport(GCMMap map, Button btnImport) {
        btnImport.setDisable(true);
        btnImport.setText("Submitting...");

        String cityName = map.getCity() != null ? map.getCity().getName() : "Unknown";
        String targetName = "Import: " + map.getName() + " -> " + cityName;
        String json = "{\"externalMapId\":" + map.getId() +
                ",\"mapName\":\"" + escapeJson(map.getName()) +
                "\",\"cityName\":\"" + escapeJson(cityName) + "\"}";

        User currentUser = client.getCurrentUser();
        Integer requesterId = currentUser != null ? currentUser.getId() : null;

        ContentChangeRequest changeRequest = new ContentChangeRequest(
                requesterId,
                ContentActionType.ADD,
                ContentType.MAP,
                map.getId(),
                targetName,
                json
        );

        new Thread(() -> {
            try {
                Message request = new Message(ActionType.SUBMIT_CONTENT_CHANGE_REQUEST, changeRequest);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getAction() == ActionType.SUBMIT_CONTENT_CHANGE_RESPONSE) {
                        boolean success = (Boolean) response.getMessage();
                        if (success) {
                            btnImport.setText("Submitted");
                            btnImport.setStyle("-fx-background-color: #7f8c8d; -fx-text-fill: white; -fx-background-radius: 5;");
                            showAlert("Success", "Import request for '" + map.getName() + "' submitted for approval.");
                        } else {
                            btnImport.setText("Import");
                            btnImport.setDisable(false);
                            showAlert("Error", "Failed to submit import request.");
                        }
                    } else {
                        btnImport.setText("Import");
                        btnImport.setDisable(false);
                        showAlert("Error", "Server error while submitting import.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnImport.setText("Import");
                    btnImport.setDisable(false);
                    showAlert("Error", "Error: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void onClose() {
        ((Stage) btnClose.getScene().getWindow()).close();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
