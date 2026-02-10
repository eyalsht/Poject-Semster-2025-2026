package controllers;

import client.GCMClient;
import common.content.City;
import common.content.GCMMap;
import common.dto.PriceChangeRequestDTO;
import common.enums.ActionType;
import common.messaging.Message;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for the Price Update Request dialog.
 * Allows a Content Manager to select a city,
 * view/edit subscription + map prices, then submit a change request
 * for Company Manager approval.
 */
public class PriceUpdateDialogController {

    @FXML private ComboBox<City> cbCitySelect;
    @FXML private Label lblSubscriptionPrice;
    @FXML private TextField tfSubscriptionPrice;
    @FXML private VBox vboxMapPrices;
    @FXML private Button btnSubmit;
    @FXML private Button btnCancel;
    @FXML private ScrollPane scrollPaneMaps;

    private City selectedCity;
    private final Map<Integer, TextField> mapPriceFields = new HashMap<>();
    private final GCMClient client = GCMClient.getInstance();

    @FXML
    private void initialize() {
        btnSubmit.setDisable(true);
        tfSubscriptionPrice.setVisible(false);
        lblSubscriptionPrice.setVisible(false);
        scrollPaneMaps.setVisible(false);

        cbCitySelect.setOnAction(e -> onCitySelected());

        // Load cities from server when dialog opens
        loadCitiesFromServer();
    }

    /**
     * Fetch all cities directly from the server using GET_CITIES_REQUEST.
     */
    private void loadCitiesFromServer() {
        new Thread(() -> {
            try {
                Message request = new Message(ActionType.GET_CITIES_REQUEST, null);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getAction() == ActionType.GET_CITIES_RESPONSE) {
                        @SuppressWarnings("unchecked")
                        List<City> cities = (List<City>) response.getMessage();
                        if (cities != null && !cities.isEmpty()) {
                            cbCitySelect.setItems(FXCollections.observableArrayList(cities));
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    showAlert("Error", "Failed to load cities: " + e.getMessage())
                );
            }
        }).start();
    }

    private void onCitySelected() {
        selectedCity = cbCitySelect.getValue();
        if (selectedCity == null) return;

        // Show and populate subscription price
        lblSubscriptionPrice.setVisible(true);
        tfSubscriptionPrice.setVisible(true);
        tfSubscriptionPrice.setText(String.valueOf(selectedCity.getPriceSub()));

        // Build map price fields
        vboxMapPrices.getChildren().clear();
        mapPriceFields.clear();
        scrollPaneMaps.setVisible(true);

        List<GCMMap> maps = selectedCity.getMaps();
        if (maps != null && !maps.isEmpty()) {
            for (GCMMap map : maps) {
                GridPane row = new GridPane();
                row.setHgap(10);
                row.setPadding(new Insets(5));

                Label lblMapName = new Label(map.getName() + " (v" + map.getVersion() + "):");
                lblMapName.setMinWidth(200);

                TextField tfPrice = new TextField(String.valueOf(map.getPrice()));
                tfPrice.setPrefWidth(120);

                row.add(lblMapName, 0, 0);
                row.add(tfPrice, 1, 0);

                vboxMapPrices.getChildren().add(row);
                mapPriceFields.put(map.getId(), tfPrice);
            }
        } else {
            vboxMapPrices.getChildren().add(new Label("No maps found for this city."));
        }

        btnSubmit.setDisable(false);
    }

    @FXML
    private void onSubmit() {
        if (selectedCity == null) {
            showAlert("Validation Error", "Please select a city first.");
            return;
        }

        // Validate subscription price
        double newSubPrice;
        try {
            newSubPrice = Double.parseDouble(tfSubscriptionPrice.getText().trim());
            if (newSubPrice < 0) {
                showAlert("Validation Error", "Subscription price cannot be negative.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Invalid subscription price format.");
            return;
        }

        // Validate all map prices
        Map<Integer, Double> newMapPrices = new HashMap<>();
        Map<Integer, Double> oldMapPrices = new HashMap<>();
        for (GCMMap map : selectedCity.getMaps()) {
            TextField tf = mapPriceFields.get(map.getId());
            if (tf == null) continue;

            double newMapPrice;
            try {
                newMapPrice = Double.parseDouble(tf.getText().trim());
                if (newMapPrice < 0) {
                    showAlert("Validation Error",
                            "Price for map '" + map.getName() + "' cannot be negative.");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Validation Error",
                        "Invalid price format for map '" + map.getName() + "'.");
                return;
            }
            newMapPrices.put(map.getId(), newMapPrice);
            oldMapPrices.put(map.getId(), map.getPrice());
        }

        // Check if anything actually changed
        boolean subChanged = newSubPrice != selectedCity.getPriceSub();
        boolean anyMapChanged = false;
        for (Map.Entry<Integer, Double> entry : newMapPrices.entrySet()) {
            Double oldPrice = oldMapPrices.get(entry.getKey());
            if (oldPrice != null && Double.compare(oldPrice, entry.getValue()) != 0) {
                anyMapChanged = true;
                break;
            }
        }

        if (!subChanged && !anyMapChanged) {
            showAlert("No Changes", "No prices were modified. Nothing to submit.");
            return;
        }

        // Build the DTO
        Integer requesterId = null;
        if (client.getCurrentUser() != null) {
            requesterId = client.getCurrentUser().getId();
        }

        PriceChangeRequestDTO dto = new PriceChangeRequestDTO(
                selectedCity.getId(), selectedCity.getName(), requesterId);
        dto.setOldSubscriptionPrice(selectedCity.getPriceSub());
        dto.setNewSubscriptionPrice(newSubPrice);
        dto.setMapPriceChanges(newMapPrices);
        dto.setMapOldPrices(oldMapPrices);

        // Send using the EXISTING action type
        btnSubmit.setDisable(true);

        new Thread(() -> {
            try {
                Message request = new Message(ActionType.UPDATE_PRICE_REQUEST, dto);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> handleResponse(response));

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to submit price change request: " + e.getMessage());
                    btnSubmit.setDisable(false);
                });
            }
        }).start();
    }

    private void handleResponse(Message response) {
        if (response == null) {
            showAlert("Error", "No response from server.");
            btnSubmit.setDisable(false);
            return;
        }

        if (response.getAction() == ActionType.UPDATE_PRICE_RESPONSE) {
            boolean success = (Boolean) response.getMessage();
            if (success) {
                showAlert("Success",
                        "Price change request submitted successfully!\n" +
                        "The Company Manager will review and approve it.");
                closeDialog();
            } else {
                showAlert("Error", "Failed to submit price update request.");
                btnSubmit.setDisable(false);
            }
        } else {
            showAlert("Error", "Unexpected response from server.");
            btnSubmit.setDisable(false);
        }
    }

    @FXML
    private void onCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
