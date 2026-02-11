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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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

    // --- FXML fields (existing) ---
    @FXML private ComboBox<City> cbCitySelect;
    @FXML private Label lblSubscriptionPrice;
    @FXML private TextField tfSubscriptionPrice;
    @FXML private VBox vboxMapPrices;
    @FXML private Button btnSubmit;
    @FXML private Button btnCancel;
    @FXML private ScrollPane scrollPaneMaps;

    // --- FXML fields (new) ---
    @FXML private VBox vboxSubscription;
    @FXML private Label lblSubCurrentPrice;
    @FXML private Button btnSubReset;
    @FXML private GridPane gridMapHeader;
    @FXML private Label lblChangesCounter;

    // --- Instance fields ---
    private City selectedCity;
    private final Map<Integer, TextField> mapPriceFields = new HashMap<>();
    private final GCMClient client = GCMClient.getInstance();

    private final Map<Integer, Double> originalMapPrices = new HashMap<>();
    private final Map<Integer, Button> mapResetButtons = new HashMap<>();
    private final Map<Integer, GridPane> mapRows = new HashMap<>();
    private double originalSubPrice;
    private int changeCount;

    // --- Style constants ---
    private static final String STYLE_ROW_NORMAL =
            "-fx-background-color: rgba(0,0,0,0.25); -fx-background-radius: 6; -fx-border-radius: 6; " +
            "-fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1;";
    private static final String STYLE_ROW_CHANGED =
            "-fx-background-color: rgba(230,126,34,0.20); -fx-background-radius: 6; -fx-border-radius: 6; " +
            "-fx-border-color: rgba(230,126,34,0.60); -fx-border-width: 1;";
    private static final String STYLE_TEXTFIELD =
            "-fx-control-inner-background: rgba(0,0,0,0.45); -fx-background-color: rgba(0,0,0,0.45); " +
            "-fx-text-fill: white; -fx-prompt-text-fill: rgba(255,255,255,0.45); " +
            "-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: rgba(255,255,255,0.18); -fx-padding: 5;";
    private static final String STYLE_RESET_BTN =
            "-fx-background-color: rgba(230,126,34,0.55); -fx-text-fill: white; " +
            "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: rgba(230,126,34,0.80); " +
            "-fx-padding: 2 7 2 7; -fx-font-size: 12; -fx-cursor: hand;";
    private static final String STYLE_SUB_NORMAL =
            "-fx-background-color: rgba(0,0,0,0.25); -fx-background-radius: 8; -fx-border-radius: 8; " +
            "-fx-border-color: rgba(255,255,255,0.10); -fx-padding: 10;";
    private static final String STYLE_SUB_CHANGED =
            "-fx-background-color: rgba(230,126,34,0.20); -fx-background-radius: 8; -fx-border-radius: 8; " +
            "-fx-border-color: rgba(230,126,34,0.60); -fx-padding: 10;";

    @FXML
    private void initialize() {
        btnSubmit.setDisable(true);
        vboxSubscription.setVisible(false);
        vboxSubscription.setManaged(false);
        gridMapHeader.setVisible(false);
        gridMapHeader.setManaged(false);
        scrollPaneMaps.setVisible(false);

        setupColumnConstraints(gridMapHeader);

        cbCitySelect.setOnAction(e -> onCitySelected());

        // Listen for subscription price changes
        tfSubscriptionPrice.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean changed = isFieldChanged(newVal, originalSubPrice);
            highlightSubscriptionSection(changed);
            btnSubReset.setVisible(changed);
            btnSubReset.setManaged(changed);
            recalculateChangeCount();
        });

        loadCitiesFromServer();
    }

    private void setupColumnConstraints(GridPane grid) {
        grid.getColumnConstraints().clear();

        ColumnConstraints colName = new ColumnConstraints(200);
        colName.setHgrow(Priority.SOMETIMES);
        ColumnConstraints colCurrent = new ColumnConstraints(80);
        ColumnConstraints colNew = new ColumnConstraints(120);
        ColumnConstraints colReset = new ColumnConstraints(35);

        grid.getColumnConstraints().addAll(colName, colCurrent, colNew, colReset);
    }

    private boolean isFieldChanged(String newText, double original) {
        if (newText == null || newText.trim().isEmpty()) return false;
        try {
            double val = Double.parseDouble(newText.trim());
            return Double.compare(val, original) != 0;
        } catch (NumberFormatException e) {
            return true; // non-numeric text counts as changed
        }
    }

    private void highlightSubscriptionSection(boolean changed) {
        vboxSubscription.setStyle(changed ? STYLE_SUB_CHANGED : STYLE_SUB_NORMAL);
    }

    private void recalculateChangeCount() {
        changeCount = 0;

        // Check subscription
        if (isFieldChanged(tfSubscriptionPrice.getText(), originalSubPrice)) {
            changeCount++;
        }

        // Check each map
        for (Map.Entry<Integer, TextField> entry : mapPriceFields.entrySet()) {
            Double orig = originalMapPrices.get(entry.getKey());
            if (orig != null && isFieldChanged(entry.getValue().getText(), orig)) {
                changeCount++;
            }
        }

        updateChangesCounter();
    }

    private void updateChangesCounter() {
        if (changeCount > 0) {
            String text = changeCount == 1 ? "1 price modified" : changeCount + " prices modified";
            lblChangesCounter.setText(text);
            lblChangesCounter.setVisible(true);
            lblChangesCounter.setManaged(true);
        } else {
            lblChangesCounter.setText("");
            lblChangesCounter.setVisible(false);
            lblChangesCounter.setManaged(false);
        }
    }

    @FXML
    private void onResetSubscriptionPrice() {
        tfSubscriptionPrice.setText(String.valueOf(originalSubPrice));
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
        City basicCity = cbCitySelect.getValue();
        if (basicCity == null) return;

        btnSubmit.setDisable(true);
        vboxMapPrices.getChildren().clear();
        mapPriceFields.clear();
        originalMapPrices.clear();
        mapResetButtons.clear();
        mapRows.clear();
        changeCount = 0;
        updateChangesCounter();

        vboxMapPrices.getChildren().add(new Label("Loading city data...") {{
            setStyle("-fx-text-fill: rgba(255,255,255,0.65);");
        }});
        scrollPaneMaps.setVisible(true);

        new Thread(() -> {
            try {
                Message request = new Message(ActionType.GET_CITY_DETAILS_REQUEST, basicCity.getId());
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getAction() == ActionType.GET_CITY_DETAILS_RESPONSE) {
                        selectedCity = (City) response.getMessage();
                        populateCityDetails();
                    } else {
                        vboxMapPrices.getChildren().clear();
                        vboxMapPrices.getChildren().add(new Label("Failed to load city details.") {{
                            setStyle("-fx-text-fill: rgba(255,255,255,0.65);");
                        }});
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    vboxMapPrices.getChildren().clear();
                    vboxMapPrices.getChildren().add(new Label("Error: " + e.getMessage()) {{
                        setStyle("-fx-text-fill: rgba(255,255,255,0.65);");
                    }});
                });
            }
        }).start();
    }

    private void populateCityDetails() {
        // Subscription price
        originalSubPrice = selectedCity.getPriceSub();
        lblSubCurrentPrice.setText("Current: " + originalSubPrice);
        tfSubscriptionPrice.setText(String.valueOf(originalSubPrice));
        vboxSubscription.setVisible(true);
        vboxSubscription.setManaged(true);
        vboxSubscription.setStyle(STYLE_SUB_NORMAL);
        btnSubReset.setVisible(false);
        btnSubReset.setManaged(false);

        // Build map price rows
        vboxMapPrices.getChildren().clear();
        mapPriceFields.clear();
        originalMapPrices.clear();
        mapResetButtons.clear();
        mapRows.clear();

        List<GCMMap> maps = selectedCity.getMaps();
        if (maps != null && !maps.isEmpty()) {
            gridMapHeader.setVisible(true);
            gridMapHeader.setManaged(true);

            for (GCMMap map : maps) {
                GridPane row = new GridPane();
                row.setHgap(10);
                row.setPadding(new Insets(5));
                row.setStyle(STYLE_ROW_NORMAL);
                setupColumnConstraints(row);

                // Column 0: Map name
                Label lblMapName = new Label(map.getName() + " (v" + map.getVersion() + ")");
                lblMapName.setStyle("-fx-text-fill: white;");
                lblMapName.setMinWidth(180);

                // Column 1: Current price
                Label lblCurrentPrice = new Label(String.valueOf(map.getPrice()));
                lblCurrentPrice.setStyle("-fx-text-fill: rgba(255,255,255,0.65);");

                // Column 2: New price field
                TextField tfPrice = new TextField(String.valueOf(map.getPrice()));
                tfPrice.setPrefWidth(110);
                tfPrice.setStyle(STYLE_TEXTFIELD);

                // Column 3: Reset button (hidden by default)
                Button btnReset = new Button("\u21BA");
                btnReset.setStyle(STYLE_RESET_BTN);
                btnReset.setVisible(false);
                btnReset.setManaged(false);

                double origPrice = map.getPrice();
                int mapId = map.getId();

                btnReset.setOnAction(e -> tfPrice.setText(String.valueOf(origPrice)));

                // Change listener
                tfPrice.textProperty().addListener((obs, oldVal, newVal) -> {
                    boolean changed = isFieldChanged(newVal, origPrice);
                    row.setStyle(changed ? STYLE_ROW_CHANGED : STYLE_ROW_NORMAL);
                    btnReset.setVisible(changed);
                    btnReset.setManaged(changed);
                    recalculateChangeCount();
                });

                row.add(lblMapName, 0, 0);
                row.add(lblCurrentPrice, 1, 0);
                row.add(tfPrice, 2, 0);
                row.add(btnReset, 3, 0);

                vboxMapPrices.getChildren().add(row);
                mapPriceFields.put(mapId, tfPrice);
                originalMapPrices.put(mapId, origPrice);
                mapResetButtons.put(mapId, btnReset);
                mapRows.put(mapId, row);
            }
        } else {
            gridMapHeader.setVisible(false);
            gridMapHeader.setManaged(false);
            vboxMapPrices.getChildren().add(new Label("No maps found for this city.") {{
                setStyle("-fx-text-fill: rgba(255,255,255,0.65);");
            }});
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
