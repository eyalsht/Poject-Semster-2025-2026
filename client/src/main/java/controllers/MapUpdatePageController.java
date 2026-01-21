package controllers;

import client.GCMClient;
import common.content.GCMMap;
import common.content.Site;
import common.dto.ContentChangeRequest;
import common.enums.ActionType;
import common.enums.ContentActionType;
import common.enums.ContentType;
import common.messaging.Message;
import common.user.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Controller for map update/price update dialog.
 * Supports three distinct modes with strict UI separation:
 * - "price": Only price editing (for Content Managers requesting price changes)
 * - "edit": Only description and sites editing (City/Price are read-only)
 * - "add": Full creation mode (all fields editable except price initially)
 */
public class MapUpdatePageController {

    // Current values (left side - read-only display)
    @FXML private Label lblCurCity;
    @FXML private Label lblCurMap;
    @FXML private Label lblCurPrice;
    @FXML private TextArea taCurDesc;
    @FXML private Label lblCurrentTitle;
    @FXML private Label lblUpdateTitle;

    // Labels that may be hidden based on mode
    @FXML private Label lblPriceLabel;
    @FXML private Label lblDescLabel;
    @FXML private Label lblNewCityLabel;
    @FXML private Label lblNewMapLabel;
    @FXML private Label lblNewPriceLabel;
    @FXML private Label lblNewDescLabel;

    // New values (right side - editable)
    @FXML private TextField tfNewCity;
    @FXML private TextField tfNewMap;
    @FXML private TextField tfNewPrice;
    @FXML private TextArea taNewDesc;

    // Container VBoxes for visibility control
    @FXML private VBox vboxCurrentValues;
    @FXML private VBox vboxNewValues;
    @FXML private VBox vboxCurrentSites;
    @FXML private VBox vboxSitesManagement;

    // Sites management
    @FXML private ListView<Site> lvCurrentSites;
    @FXML private ListView<Site> lvAvailableSites;
    @FXML private ListView<Site> lvSelectedSites;
    @FXML private Button btnAddSite;
    @FXML private Button btnRemoveSite;

    // Buttons
    @FXML private Button btnClose;
    @FXML private Button btnAddUpdate;
    @FXML private Button btnDeny;
    @FXML private Button btnApprove;

    private String mode;  // "add", "edit", or "price"
    private GCMMap selectedMap;
    private CatalogPageController catalogController;

    // Sites data
    private ObservableList<Site> availableSites = FXCollections.observableArrayList();
    private ObservableList<Site> selectedSites = FXCollections.observableArrayList();

    private final GCMClient client = GCMClient.getInstance();

    public void setMode(String mode) {
        this.mode = mode;
        updateUIForMode();
    }

    public void setSelectedMap(GCMMap map) {
        this.selectedMap = map;
        populateFields();
        // Don't try to load sites here - they're lazily loaded and will cause errors
        // Sites will be loaded from server when we implement GET_MAP_DETAILS_REQUEST
        if ("edit".equals(mode)) {
            // For now, just show empty sites lists
            // loadSitesForMap();  // Disabled until server-side fetch is implemented
        }
    }

    public void setCatalogController(CatalogPageController controller) {
        this.catalogController = controller;
    }

    @FXML
    public void initialize() {
        // Setup site list views
        if (lvAvailableSites != null) {
            lvAvailableSites.setItems(availableSites);
        }
        if (lvSelectedSites != null) {
            lvSelectedSites.setItems(selectedSites);
        }
    }

    /**
     * Configure UI visibility and editability based on mode.
     * Enforces strict separation between price update and content edit.
     */
    private void updateUIForMode() {
        // Hide all optional sections by default
        hideAllOptionalSections();

        switch (mode) {
            case "price":
                configurePriceMode();
                break;
            case "edit":
                configureEditMode();
                break;
            case "add":
                configureAddMode();
                break;
            case "approve":
                configureApproveMode();
                break;
        }
    }

    private void hideAllOptionalSections() {
        // Hide sites sections by default
        if (vboxCurrentSites != null) vboxCurrentSites.setVisible(false);
        if (vboxCurrentSites != null) vboxCurrentSites.setManaged(false);
        if (vboxSitesManagement != null) vboxSitesManagement.setVisible(false);
        if (vboxSitesManagement != null) vboxSitesManagement.setManaged(false);
        
        // Hide approve/deny buttons by default
        btnApprove.setVisible(false);
        btnDeny.setVisible(false);
    }

    /**
     * Price Mode: ONLY show price-related fields.
     * Hide description, sites, city, map name editing.
     */
    private void configurePriceMode() {
        lblCurrentTitle.setText("Current Price");
        lblUpdateTitle.setText("New Price");
        
        // Show only price fields
        tfNewCity.setVisible(false);
        tfNewCity.setManaged(false);
        tfNewMap.setVisible(false);
        tfNewMap.setManaged(false);
        taNewDesc.setVisible(false);
        taNewDesc.setManaged(false);
        
        // Hide labels for hidden fields
        if (lblNewCityLabel != null) { lblNewCityLabel.setVisible(false); lblNewCityLabel.setManaged(false); }
        if (lblNewMapLabel != null) { lblNewMapLabel.setVisible(false); lblNewMapLabel.setManaged(false); }
        if (lblNewDescLabel != null) { lblNewDescLabel.setVisible(false); lblNewDescLabel.setManaged(false); }
        
        // Hide description on left side too
        if (taCurDesc != null) { taCurDesc.setVisible(false); taCurDesc.setManaged(false); }
        if (lblDescLabel != null) { lblDescLabel.setVisible(false); lblDescLabel.setManaged(false); }
        
        // Price field is editable
        tfNewPrice.setEditable(true);
        tfNewPrice.setVisible(true);
        
        btnAddUpdate.setText("Submit Price Change");
        btnAddUpdate.setVisible(true);
    }

    /**
     * Edit Mode: Edit description only for now.
     * City and Price are READ-ONLY (grayed out).
     * Sites management is disabled until server-side fetch is implemented.
     */
    private void configureEditMode() {
        lblCurrentTitle.setText("Current Map Details");
        lblUpdateTitle.setText("Edit Map");
        
        // City is read-only
        tfNewCity.setEditable(false);
        tfNewCity.setStyle("-fx-background-color: #e0e0e0;");
        
        // Map name is read-only
        tfNewMap.setEditable(false);
        tfNewMap.setStyle("-fx-background-color: #e0e0e0;");
        
        // Price is read-only (grayed out)
        tfNewPrice.setEditable(false);
        tfNewPrice.setStyle("-fx-background-color: #e0e0e0;");
        
        // Description IS editable
        taNewDesc.setEditable(true);
        
        // Hide sites management sections until fully implemented
        // TODO: Enable these when GET_MAP_SITES_REQUEST is implemented
        if (vboxCurrentSites != null) { vboxCurrentSites.setVisible(false); vboxCurrentSites.setManaged(false); }
        if (vboxSitesManagement != null) { vboxSitesManagement.setVisible(false); vboxSitesManagement.setManaged(false); }
        
        btnAddUpdate.setText("Submit Update");
        btnAddUpdate.setVisible(true);
    }

    /**
     * Add Mode: All content fields editable (except price which uses separate workflow).
     */
    private void configureAddMode() {
        lblCurrentTitle.setText("(New Map)");
        lblUpdateTitle.setText("New Map Details");
        
        // All content fields editable
        tfNewCity.setEditable(true);
        tfNewMap.setEditable(true);
        taNewDesc.setEditable(true);
        
        // Price is NOT editable in add mode (set later by Content Manager)
        tfNewPrice.setEditable(false);
        tfNewPrice.setStyle("-fx-background-color: #e0e0e0;");
        tfNewPrice.setPromptText("Set via Price Update");
        
        // Hide left side (no current values for new map)
        if (vboxCurrentValues != null) { vboxCurrentValues.setVisible(false); vboxCurrentValues.setManaged(false); }
        
        // Show sites management
        if (vboxSitesManagement != null) { vboxSitesManagement.setVisible(true); vboxSitesManagement.setManaged(true); }
        
        btnAddUpdate.setText("Submit New Map");
        btnAddUpdate.setVisible(true);
    }

    /**
     * Approve Mode: All fields read-only, show approve/deny buttons.
     */
    private void configureApproveMode() {
        lblCurrentTitle.setText("Pending Change");
        lblUpdateTitle.setText("Proposed Values");
        
        tfNewCity.setEditable(false);
        tfNewMap.setEditable(false);
        tfNewPrice.setEditable(false);
        taNewDesc.setEditable(false);
        
        btnAddUpdate.setVisible(false);
        btnApprove.setVisible(true);
        btnDeny.setVisible(true);
    }

    private void populateFields() {
        if (selectedMap != null) {
            // Populate current values (left side)
            lblCurCity.setText(selectedMap.getCityName() != null ? selectedMap.getCityName() : "-");
            lblCurMap.setText(selectedMap.getName() != null ? selectedMap.getName() : "-");
            lblCurPrice.setText(String.format("$%.2f", selectedMap.getPrice()));
            if (taCurDesc != null) {
                taCurDesc.setText(selectedMap.getDescription() != null ? selectedMap.getDescription() : "");
            }

            // Pre-fill new values based on mode
            if (!"price".equals(mode)) {
                tfNewCity.setText(selectedMap.getCityName());
                tfNewMap.setText(selectedMap.getName());
                taNewDesc.setText(selectedMap.getDescription());
            }
            tfNewPrice.setText(String.format("%.2f", selectedMap.getPrice()));
        } else {
            // Clear for add mode
            lblCurCity.setText("-");
            lblCurMap.setText("-");
            lblCurPrice.setText("-");
            if (taCurDesc != null) taCurDesc.setText("");
        }
    }

    /**
     * Load sites for the current map's city.
     * This requires a server call because sites are lazily loaded.
     * For now, this is a placeholder until we implement the server handler.
     */
    private void loadSitesForMap() {
        // Sites are lazily loaded on the entity, so we can't access them directly
        // We need to fetch them from the server with a dedicated request
        
        // For now, just clear the lists - sites management will be implemented
        // when we add GET_MAP_SITES_REQUEST / GET_CITY_SITES_REQUEST handlers
        selectedSites.clear();
        availableSites.clear();
        
        if (lvCurrentSites != null) {
            lvCurrentSites.setItems(FXCollections.observableArrayList());
        }

        // TODO: Implement server call to get sites
        // loadAvailableSitesFromServer();
    }

    private void loadAvailableSitesFromServer() {
        // This would make a server request to get available sites
        // For now, placeholder - you'll need to implement the server handler
        new Thread(() -> {
            try {
                // TODO: Implement GET_AVAILABLE_SITES_REQUEST
                // Message request = new Message(ActionType.GET_AVAILABLE_SITES_REQUEST, selectedMap.getCityName());
                // Message response = (Message) client.sendRequest(request);
                // Platform.runLater(() -> handleAvailableSitesResponse(response));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void onAddSite() {
        Site selected = lvAvailableSites.getSelectionModel().getSelectedItem();
        if (selected != null) {
            availableSites.remove(selected);
            selectedSites.add(selected);
        }
    }

    @FXML
    private void onRemoveSite() {
        Site selected = lvSelectedSites.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectedSites.remove(selected);
            availableSites.add(selected);
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
            submitContentChange(ContentActionType.ADD);
        } else if ("edit".equals(mode)) {
            submitContentChange(ContentActionType.EDIT);
        }
    }

    /**
     * Submit content change as a pending request (for Content Workers/Managers).
     * The change will require approval from a Content Manager.
     */
    private void submitContentChange(ContentActionType actionType) {
        // Validate inputs
        String mapName = tfNewMap.getText().trim();
        String description = taNewDesc.getText().trim();
        String cityName = tfNewCity.getText().trim();
        
        if (actionType == ContentActionType.ADD) {
            if (mapName.isEmpty()) {
                showAlert("Validation Error", "Map name is required.");
                return;
            }
            if (cityName.isEmpty()) {
                showAlert("Validation Error", "City name is required for new maps.");
                return;
            }
        }

        new Thread(() -> {
            try {
                User currentUser = client.getCurrentUser();
                Integer requesterId = currentUser != null ? currentUser.getId() : null;

                // Build JSON content details including sites
                String contentJson = buildMapContentJson(cityName, mapName, description);
                
                // Determine target name for display
                String targetName = actionType == ContentActionType.ADD 
                    ? cityName + " - " + mapName 
                    : selectedMap.getCityName() + " - " + selectedMap.getName();
                
                Integer targetId = actionType == ContentActionType.ADD 
                    ? null 
                    : selectedMap.getId();

                ContentChangeRequest changeRequest = new ContentChangeRequest(
                    requesterId,
                    actionType,
                    ContentType.MAP,
                    targetId,
                    targetName,
                    contentJson
                );

                Message request = new Message(ActionType.SUBMIT_CONTENT_CHANGE_REQUEST, changeRequest);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> handleContentChangeResponse(response, actionType));

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Error: " + e.getMessage()));
            }
        }).start();
    }

    private String buildMapContentJson(String cityName, String mapName, String description) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"cityName\":\"").append(escapeJson(cityName)).append("\",");
        json.append("\"mapName\":\"").append(escapeJson(mapName)).append("\",");
        json.append("\"description\":\"").append(escapeJson(description)).append("\"");
        
        // Include selected site IDs
        if (!selectedSites.isEmpty()) {
            json.append(",\"siteIds\":[");
            json.append(selectedSites.stream()
                .map(s -> String.valueOf(s.getId()))
                .collect(Collectors.joining(",")));
            json.append("]");
        }
        
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }

    private void handleContentChangeResponse(Message response, ContentActionType actionType) {
        if (response == null) {
            showAlert("Error", "No response from server.");
            return;
        }

        if (response.getAction() == ActionType.SUBMIT_CONTENT_CHANGE_RESPONSE) {
            boolean success = (Boolean) response.getMessage();
            if (success) {
                String actionName = actionType == ContentActionType.ADD ? "addition" : "update";
                showAlert("Success", "Map " + actionName + " submitted for approval!");
                onClose();
            } else {
                showAlert("Error", "Failed to submit content change.");
            }
        } else if (response.getAction() == ActionType.ERROR) {
            showAlert("Error", response.getMessage().toString());
        } else {
            showAlert("Error", "Unexpected response from server.");
        }
    }

    @FXML
    private void onApprove() {
        showAlert("Info", "Approval feature is not yet implemented.");
    }

    @FXML
    private void onDeny() {
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
