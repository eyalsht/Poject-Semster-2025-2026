package controllers;

import client.GCMClient;
import common.enums.ActionType;
import common.messaging.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the temporary Add External Map dialog.
 * Allows adding maps directly to the external repository for testing.
 */
public class AddExternalMapDialogController {

    @FXML private TextField txtMapName;
    @FXML private TextField txtCityName;
    @FXML private TextArea txtDescription;
    @FXML private Button btnUploadImage;
    @FXML private Button btnSubmit;
    @FXML private Button btnCancel;
    @FXML private Label lblImageStatus;
    @FXML private Label lblError;
    @FXML private ImageView imgPreview;

    private byte[] selectedImageBytes;
    private final GCMClient client = GCMClient.getInstance();

    @FXML
    public void initialize() {
        // Enable submit button only when required fields are filled and image is selected
        txtMapName.textProperty().addListener((obs, o, n) -> validateForm());
        txtCityName.textProperty().addListener((obs, o, n) -> validateForm());
    }

    private void validateForm() {
        boolean valid = !txtMapName.getText().isBlank()
                && !txtCityName.getText().isBlank()
                && selectedImageBytes != null;
        btnSubmit.setDisable(!valid);
    }

    @FXML
    private void onUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Map Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fileChooser.showOpenDialog(btnUploadImage.getScene().getWindow());
        if (file != null) {
            try {
                long fileSize = file.length();
                if (fileSize > 5 * 1024 * 1024) {
                    lblError.setText("Image must be under 5 MB. Selected: " +
                            String.format("%.1f", fileSize / (1024.0 * 1024.0)) + " MB.");
                    return;
                }
                selectedImageBytes = Files.readAllBytes(file.toPath());
                Image img = new Image(new ByteArrayInputStream(selectedImageBytes));
                imgPreview.setImage(img);
                lblImageStatus.setText(file.getName());
                lblImageStatus.setStyle("-fx-text-fill: #27ae60;");
                lblError.setText("");
                validateForm();
            } catch (Exception ex) {
                lblError.setText("Could not load image: " + ex.getMessage());
            }
        }
    }

    @FXML
    private void onSubmit() {
        String mapName = txtMapName.getText().trim();
        String cityName = txtCityName.getText().trim();
        String description = txtDescription.getText() != null ? txtDescription.getText().trim() : "";

        if (mapName.isEmpty() || cityName.isEmpty() || selectedImageBytes == null) {
            lblError.setText("Map name, city name, and image are required.");
            return;
        }

        btnSubmit.setDisable(true);
        btnSubmit.setText("Submitting...");
        lblError.setText("");

        List<Object> params = new ArrayList<>();
        params.add(mapName);
        params.add(description);
        params.add(cityName);
        params.add(selectedImageBytes);

        new Thread(() -> {
            try {
                Message request = new Message(ActionType.ADD_EXTERNAL_MAP_REQUEST, params);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getAction() == ActionType.ADD_EXTERNAL_MAP_RESPONSE) {
                        boolean success = (Boolean) response.getMessage();
                        if (success) {
                            showAlert("Success", "External map '" + mapName + "' added successfully.");
                            ((Stage) btnSubmit.getScene().getWindow()).close();
                        } else {
                            lblError.setText("Failed to add external map.");
                            btnSubmit.setDisable(false);
                            btnSubmit.setText("Submit");
                        }
                    } else {
                        String errMsg = response != null ? String.valueOf(response.getMessage()) : "Unknown error";
                        lblError.setText("Error: " + errMsg);
                        btnSubmit.setDisable(false);
                        btnSubmit.setText("Submit");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblError.setText("Error: " + e.getMessage());
                    btnSubmit.setDisable(false);
                    btnSubmit.setText("Submit");
                });
            }
        }).start();
    }

    @FXML
    private void onCancel() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
