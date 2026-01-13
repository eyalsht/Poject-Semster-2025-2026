package controllers;

import client.GCMClient;
import common.content.GCMMap;
import common.enums.ActionType;
import common.messaging.Message;
import common.user.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.ArrayList;

/**
 * Controller for map update/price update dialog.
 */
public class MapUpdatePageController {

    // Current values (left side - read-only display)
    @FXML private Label lblCurCity;
    @FXML private Label lblCurMap;
    @FXML private Label lblCurPrice;
    @FXML private TextArea taCurDesc;

    // New values (right side - editable)
    @FXML private TextField tfNewCity;
    @FXML private TextField tfNewMap;
    @FXML private TextField tfNewPrice;
    @FXML private TextArea taNewDesc;

    // Buttons
    @FXML private Button btnClose;
    @FXML private Button btnAddUpdate;
    @FXML private Button btnDeny;
    @FXML private Button btnApprove;

    private String mode;  // "add", "edit", or "price"
    private GCMMap selectedMap;
    private CatalogPageController catalogController;

    private final GCMClient client = GCMClient.getInstance();

    public void setMode(String mode) {
        this.mode = mode;
        updateUIForMode();
    }

    public void setSelectedMap(GCMMap map) {
        this.selectedMap = map;
        populateFields();
    }

    public void setCatalogController(CatalogPageController controller) {
        this.catalogController = controller;
    }

    @FXML
    public void initialize() {
        // Initial setup if needed
    }

    private void updateUIForMode() {
        switch (mode) {
            case "add":
                setFieldsEditable(true, true, true, true);
                btnAddUpdate.setText("Add");
                btnApprove.setVisible(false);
                btnDeny.setVisible(false);
                break;
            case "edit":
                setFieldsEditable(false, true, true, true);
                btnAddUpdate.setText("Update");
                btnApprove.setVisible(false);
                btnDeny.setVisible(false);
                break;
            case "price":
                setFieldsEditable(false, false, true, false);
                btnAddUpdate.setText("Submit Price Update");
                btnApprove.setVisible(false);
                btnDeny.setVisible(false);
                break;
            case "approve":
                // For approval workflow
                setFieldsEditable(false, false, false, false);
                btnAddUpdate.setVisible(false);
                btnApprove.setVisible(true);
                btnDeny.setVisible(true);
                break;
        }
    }

    private void setFieldsEditable(boolean city, boolean mapName, boolean price, boolean description) {
        tfNewCity.setEditable(city);
        tfNewMap.setEditable(mapName);
        tfNewPrice.setEditable(price);
        taNewDesc.setEditable(description);
    }

    private void populateFields() {
        if (selectedMap != null) {
            // Populate current values (left side)
            lblCurCity.setText(selectedMap.getCityName());
            lblCurMap.setText(selectedMap.getName());
            lblCurPrice.setText(String.valueOf(selectedMap.getPrice()));
            taCurDesc.setText(selectedMap.getDescription());

            // Pre-fill new values (right side) with current values
            tfNewCity.setText(selectedMap.getCityName());
            tfNewMap.setText(selectedMap.getName());
            tfNewPrice.setText(String.valueOf(selectedMap.getPrice()));
            taNewDesc.setText(selectedMap.getDescription());
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void onAddUpdate() {
        if ("price".equals(mode)) {
            submitPriceUpdate();
        } else if ("add".equals(mode)) {
            // TODO: Implement add map
            showAlert("Info", "Add map feature is not yet implemented.");
        } else if ("edit".equals(mode)) {
            // TODO: Implement edit map
            showAlert("Info", "Edit map feature is not yet implemented.");
        }
    }

    @FXML
    private void onApprove() {
        // TODO: Implement approval logic
        showAlert("Info", "Approval feature is not yet implemented.");
    }

    @FXML
    private void onDeny() {
        // TODO: Implement deny logic
        showAlert("Info", "Deny feature is not yet implemented.");
    }

    private void submitPriceUpdate() {
        String priceText = tfNewPrice.getText().trim();

        if (priceText.isEmpty()) {
            showAlert("Validation Error", "Please enter a price.");
            return;
        }

        double newPrice;
        try {
            newPrice = Double.parseDouble(priceText);
            if (newPrice < 0) {
                showAlert("Validation Error", "Price cannot be negative.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Invalid price format.");
            return;
        }

        new Thread(() -> {
            try {
                User currentUser = client.getCurrentUser();
                Integer requesterId = currentUser != null ? currentUser.getId() : null;

                ArrayList<Object> params = new ArrayList<>();
                params.add(selectedMap.getCityName());
                params.add(selectedMap.getName());
                params.add(selectedMap.getVersion());
                params.add(newPrice);
                params.add(requesterId);

                Message request = new Message(ActionType.UPDATE_PRICE_REQUEST, params);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> handlePriceUpdateResponse(response));

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Error: " + e.getMessage()));
            }
        }).start();
    }

    private void handlePriceUpdateResponse(Message response) {
        if (response == null) {
            showAlert("Error", "No response from server.");
            return;
        }

        if (response.getAction() == ActionType.UPDATE_PRICE_RESPONSE) {
            boolean success = (Boolean) response.getMessage();
            if (success) {
                showAlert("Success", "Price update submitted for approval!");
                // Close after showing the message
                onClose();
            } else {
                showAlert("Error", "Failed to submit price update.");
            }
        } else {
            showAlert("Error", "Unexpected response from server.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
