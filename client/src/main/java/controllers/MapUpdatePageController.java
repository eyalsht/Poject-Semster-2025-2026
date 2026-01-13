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

    @FXML private TextField txtCity;
    @FXML private TextField txtMapName;
    @FXML private TextField txtVersion;
    @FXML private TextField txtPrice;
    @FXML private TextArea txtDescription;
    @FXML private Label lblTitle;
    @FXML private Label lblStatus;
    @FXML private Button btnSave;

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
        clearStatus();
    }

    private void updateUIForMode() {
        switch (mode) {
            case "add":
                lblTitle.setText("Add New Map");
                setFieldsEditable(true, true, true, true, true);
                break;
            case "edit":
                lblTitle.setText("Edit Map");
                setFieldsEditable(false, true, false, true, true);
                break;
            case "price":
                lblTitle.setText("Update Price");
                setFieldsEditable(false, false, false, true, false);
                break;
        }
    }

    private void setFieldsEditable(boolean city, boolean mapName, boolean version, 
                                    boolean price, boolean description) {
        txtCity.setEditable(city);
        txtMapName.setEditable(mapName);
        txtVersion.setEditable(version);
        txtPrice.setEditable(price);
        txtDescription.setEditable(description);
    }

    private void populateFields() {
        if (selectedMap != null) {
            txtCity.setText(selectedMap.getCityName());
            txtMapName.setText(selectedMap.getName());
            txtVersion.setText(selectedMap.getVersion());
            txtPrice.setText(String.valueOf(selectedMap.getPrice()));
            txtDescription.setText(selectedMap.getDescription());
        }
    }

    @FXML
    private void onSave() {
        if ("price".equals(mode)) {
            submitPriceUpdate();
        } else {
            // TODO: Implement add/edit map
            showStatus("This feature is not yet implemented.", true);
        }
    }

    private void submitPriceUpdate() {
        String priceText = txtPrice.getText().trim();
        
        if (priceText.isEmpty()) {
            showStatus("Please enter a price.", true);
            return;
        }

        double newPrice;
        try {
            newPrice = Double.parseDouble(priceText);
            if (newPrice < 0) {
                showStatus("Price cannot be negative.", true);
                return;
            }
        } catch (NumberFormatException e) {
            showStatus("Invalid price format.", true);
            return;
        }

        showStatus("Submitting price update...", false);

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
                Platform.runLater(() -> showStatus("Error: " + e.getMessage(), true));
            }
        }).start();
    }

    private void handlePriceUpdateResponse(Message response) {
        if (response == null) {
            showStatus("No response from server.", true);
            return;
        }

        if (response.getAction() == ActionType.UPDATE_PRICE_RESPONSE) {
            boolean success = (Boolean) response.getMessage();
            if (success) {
                showStatus("Price update submitted for approval!", false);
                
                // Close after a short delay
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        Platform.runLater(this::onCancel);
                    } catch (InterruptedException ignored) {}
                }).start();
            } else {
                showStatus("Failed to submit price update.", true);
            }
        } else {
            showStatus("Unexpected response from server.", true);
        }
    }

    @FXML
    private void onCancel() {
        Stage stage = (Stage) btnSave.getScene().getWindow();
        stage.close();
    }

    private void showStatus(String message, boolean isError) {
        if (lblStatus != null) {
            lblStatus.setText(message);
            lblStatus.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
        }
    }

    private void clearStatus() {
        if (lblStatus != null) {
            lblStatus.setText("");
        }
    }
}
