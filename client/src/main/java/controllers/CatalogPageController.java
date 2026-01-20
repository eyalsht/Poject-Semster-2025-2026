package controllers;
import client.GCMClient;
import common.content.GCMMap;
import common.dto.CatalogFilter;
import common.dto.CatalogResponse;
import common.dto.ContentChangeRequest;
import common.dto.PendingApprovalsResponse;  // Add this import
import common.dto.PendingContentApprovalsResponse;
import common.enums.ContentActionType;
import common.enums.ContentType;
import common.enums.EmployeeRole;
import common.enums.ActionType;
import common.messaging.Message;
import common.user.Employee;
import common.user.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import common.content.City; // NEW - Fixes "Cannot resolve symbol 'City'"
import controllers.CityCardController; // NEW
import controllers.MapCardController;
/**
 * Controller for the catalog page.
 * Displays cities, maps, and allows filtering.
 */
public class CatalogPageController {

    @FXML private ComboBox<String> cbCity;
    @FXML private ComboBox<String> cbMap;
    @FXML private ComboBox<String> cbVersion;

    @FXML private ScrollPane scrollPaneCities;
    @FXML private FlowPane flowPaneCities;

    @FXML private Button btnUpdateMap;
    @FXML private Button btnAddMap;
    @FXML private Button btnDeleteMap;
    @FXML private Button btnPriceUpdate;
    @FXML private Button btnApprovals;
    @FXML private Button btnCreateCity;
    @FXML private Button btnEditCity;
    @FXML private Button btnCreateTour;
    @FXML private Button btnEditTour;

    private final GCMClient client = GCMClient.getInstance();
    private CatalogResponse lastCatalogResponse;     // Cache the last response for filter cascading
    private boolean isUpdatingComboBoxes = false;    // Flag to prevent recursive updates

    @FXML
    public void initialize() {
        setupComboBoxListeners();
        applyRolePermissions();
        scrollPaneCities.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPaneCities.setFitToWidth(true);
        loadCatalog(null, null, null);
        refreshPendingApprovalsCount();  // Updated method name
    }

    public void showCityMaps(City city) {
        Platform.runLater(() -> {
            flowPaneCities.getChildren().clear(); // Clear city cards

            // Add a "Back" button to return to City Catalog
            Button btnBack = new Button("← Back to Catalog");
            btnBack.setOnAction(e -> loadCatalog(null, null, null));
            flowPaneCities.getChildren().add(btnBack);

            // Display maps of the selected city
            if (city.getMaps() != null) {
                for (GCMMap map : city.getMaps()) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapCard.fxml"));
                        Parent mapCard = loader.load();

                        // You will need a MapCardController for this
                        MapCardController controller = loader.getController();
                        controller.setData(map);

                        flowPaneCities.getChildren().add(mapCard);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void updateCityCards(List<GCMMap> maps) {
        Platform.runLater(() -> {
            flowPaneCities.getChildren().clear(); // clean old card
            for (GCMMap map : maps) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/CityCard.fxml"));
                    Parent card = loader.load();

                    CityCardController controller = loader.getController();
                    controller.setData(map, this); // Injecting map and city data

                    flowPaneCities.getChildren().add(card);
                } catch (Exception e) {
                    System.err.println("Error loading city card: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
    /**
     * Setup table column bindings.
     * Using GCMMap entity directly instead of MapCatalogRow DTO.
     */
  /*  private void setupTableColumns() {
        // Use property names from GCMMap entity
        colCity.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCityName()));
        colMap.setCellValueFactory(new PropertyValueFactory<>("name"));
        colVersion.setCellValueFactory(new PropertyValueFactory<>("version"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Format price column
        colPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? "" : String.format("$%.2f", price));
            }
        });
    }*/

    /**
     * Setup combo box change listeners for cascading filters.
     */
    private void setupComboBoxListeners() {
        cbCity.setOnAction(e -> {
            if (isUpdatingComboBoxes) return;  // Prevent recursive calls
            String selectedCity = cbCity.getValue();
            updateMapComboBox(selectedCity);
            cbVersion.getItems().clear();
            loadCatalog(selectedCity, null, null);
        });

        cbMap.setOnAction(e -> {
            if (isUpdatingComboBoxes) return;  // Prevent recursive calls
            String selectedCity = cbCity.getValue();
            String selectedMap = cbMap.getValue();
            updateVersionComboBox(selectedCity, selectedMap);
            loadCatalog(selectedCity, selectedMap, null);
        });

        cbVersion.setOnAction(e -> {
            if (isUpdatingComboBoxes) return;  // Prevent recursive calls
            loadCatalog(cbCity.getValue(), cbMap.getValue(), cbVersion.getValue());
        });
    }

    /**
     * Load catalog data from server with filters.
     * Single request replaces the old 4 separate requests!
     */
    private void loadCatalog(String city, String map, String version) {
        new Thread(() -> {
            try {
                CatalogFilter filter = new CatalogFilter(city, map, version);
                Message request = new Message(ActionType.GET_CATALOG_REQUEST, filter);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> handleCatalogResponse(response));

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Failed to load catalog: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Handle catalog response from server.
     */
    private void handleCatalogResponse(Message response) {
        if (response == null || response.getAction() == ActionType.ERROR) {
            showAlert("Error", "Failed to load catalog data.");
            return;
        }

        if (response.getAction() == ActionType.GET_CATALOG_RESPONSE) {
            CatalogResponse catalogResponse = (CatalogResponse) response.getMessage();
            this.lastCatalogResponse = catalogResponse;

            // Using updateCityCards instead of TableView.setItems
            updateCityCards(catalogResponse.getMaps());

            // Set flag to prevent listener recursion
            isUpdatingComboBoxes = true;
            try {
                // Update city dropdown (only on first load or if empty)
                if (cbCity.getItems().isEmpty() && !catalogResponse.getAvailableCities().isEmpty()) {
                    List<String> cities = new ArrayList<>();
                    cities.add("");  // Empty option for "All"
                    cities.addAll(catalogResponse.getAvailableCities());
                    cbCity.setItems(FXCollections.observableArrayList(cities));
                }

                // Update map dropdown if available
                if (!catalogResponse.getAvailableMapNames().isEmpty()) {
                    updateMapComboBoxFromResponse(catalogResponse.getAvailableMapNames());
                }

                // Update version dropdown if available
                if (!catalogResponse.getAvailableVersions().isEmpty()) {
                    updateVersionComboBoxFromResponse(catalogResponse.getAvailableVersions());
                }
            } finally {
                isUpdatingComboBoxes = false;
            }
        }
    }

    public CatalogResponse getLastCatalogResponse() {return this.lastCatalogResponse;}
    /**
     * Update map combo box based on selected city.
     */
    private void updateMapComboBox(String selectedCity) {
        cbMap.getItems().clear();
        if (selectedCity == null || selectedCity.isEmpty()) {
            return;
        }

        // If we have cached data, filter from it
        if (lastCatalogResponse != null && lastCatalogResponse.getAvailableMapNames() != null) {
            updateMapComboBoxFromResponse(lastCatalogResponse.getAvailableMapNames());
        }
    }

    private void updateMapComboBoxFromResponse(List<String> mapNames) {
        List<String> maps = new ArrayList<>();
        maps.add("");  // Empty option for "All"
        maps.addAll(mapNames);
        cbMap.setItems(FXCollections.observableArrayList(maps));
    }

    /**
     * Update version combo box based on selected city and map.
     */
    private void updateVersionComboBox(String selectedCity, String selectedMap) {
        cbVersion.getItems().clear();
        if (selectedCity == null || selectedMap == null ||
                selectedCity.isEmpty() || selectedMap.isEmpty()) {
            return;
        }

        if (lastCatalogResponse != null && lastCatalogResponse.getAvailableVersions() != null) {
            updateVersionComboBoxFromResponse(lastCatalogResponse.getAvailableVersions());
        }
    }

    private void updateVersionComboBoxFromResponse(List<String> versions) {
        List<String> versionList = new ArrayList<>();
        versionList.add("");  // Empty option for "All"
        versionList.addAll(versions);
        cbVersion.setItems(FXCollections.observableArrayList(versionList));
    }

    /**
     * Apply visibility/permissions based on user role.
     *
     * Role permissions:
     * - Content Worker: Add/Edit/Delete (creates pending requests), NO price updates, NO approvals
     * - Content Manager: Add/Edit/Delete + Price Updates + Content Approvals
     * - Company Manager: ONLY Price Approvals + View Reports (NO content editing)
     */
    private void applyRolePermissions() {
        User user = client.getCurrentUser();

        // Hide all management buttons by default
        setManagementButtonsVisible(false);
        btnApprovals.setVisible(false);

        if (user == null) {
            return;  // Guest - no management buttons
        }

        if (user instanceof Employee employee) {
            EmployeeRole role = employee.getRole();
            if (role != null) {
                switch (role) {
                    case CONTENT_WORKER:
                        // Can add/edit/delete content (as pending requests)
                        // CANNOT approve or change prices
                        btnAddMap.setVisible(true);
                        btnUpdateMap.setVisible(true);
                        btnDeleteMap.setVisible(true);
                        btnCreateCity.setVisible(true);
                        btnEditCity.setVisible(true);
                        btnCreateTour.setVisible(true);
                        btnEditTour.setVisible(true);
                        btnPriceUpdate.setVisible(false);
                        btnApprovals.setVisible(false);
                        break;

                    case CONTENT_MANAGER:
                        // Can do everything a worker does
                        // PLUS: approve content, request price changes
                        btnAddMap.setVisible(true);
                        btnUpdateMap.setVisible(true);
                        btnDeleteMap.setVisible(true);
                        btnPriceUpdate.setVisible(true);
                        btnCreateCity.setVisible(true);
                        btnEditCity.setVisible(true);
                        btnCreateTour.setVisible(true);
                        btnEditTour.setVisible(true);
                        btnApprovals.setVisible(true);  // Shows content approvals
                        break;

                    case COMPANY_MANAGER:
                        // CANNOT edit content
                        // ONLY approves price changes
                        btnAddMap.setVisible(false);
                        btnUpdateMap.setVisible(false);
                        btnDeleteMap.setVisible(false);
                        btnPriceUpdate.setVisible(false);
                        btnApprovals.setVisible(true);  // Shows price approvals only
                        break;

                    default:
                        break;
                }
            }
        }
    }

    // ==================== BUTTON HANDLERS ====================

    /*@FXML   Using Table info - which no longer exist
    private void onPriceUpdate() {
        GCMMap selected = tblCatalog.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a map to update price.");
            return;
        }
        openMapUpdateWindow("price", selected);
    }*/
    @FXML
    private void onPriceUpdate() {
        // NEW: Placeholder method to resolve FXML error
        showAlert("Info", "Selection logic needs to be updated for city cards.");
    }



 /*   @FXML Using Table info - which no longer exist
    private void onUpdateMap() {
        GCMMap selected = tblCatalog.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a map to edit.");
            return;
        }
        openMapUpdateWindow("edit", selected);
    }*/
 @FXML
 private void onUpdateMap() {
     // NEW: Placeholder method to resolve FXML error
     showAlert("Info", "Selection logic needs to be updated for city cards.");
 }

    @FXML
    private void onCreateCity() { OpenCityUpdateWindow("create");}
    @FXML
    private void onEditCity() {
        OpenCityUpdateWindow("add");
    }
    @FXML
    private void onAddMap() {
        openMapUpdateWindow("add", null);
    }
    @FXML
    private void onCreateTour() { OpenCityUpdateWindow("create");}
    @FXML
    private void onEditTour() {
        OpenCityUpdateWindow("edit");
    }

  /*  @FXML Using Table info - which no longer exist
    private void onDeleteMap() {
        GCMMap selected = tblCatalog.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a map to delete.");
            return;
        }

        // Confirm deletion
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion Request");
        confirm.setHeaderText("Request to Delete Map");
        confirm.setContentText("This will submit a deletion request for:\n" +
                selected.getCityName() + " - " + selected.getName() +
                "\n\nThe request will need to be approved by a Content Manager.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                submitDeleteRequest(selected);
            }
        });
    }*/
  @FXML
  private void onDeleteMap() {
      // NEW: Placeholder method to resolve FXML error
      showAlert("Info", "Selection logic needs to be updated for city cards.");
  }

    private void submitDeleteRequest(GCMMap map) {
        new Thread(() -> {
            try {
                User currentUser = client.getCurrentUser();
                Integer requesterId = currentUser != null ? currentUser.getId() : null;

                String targetName = map.getCityName() + " - " + map.getName();
                String contentJson = "{\"mapId\":" + map.getId() +
                        ",\"mapName\":\"" + map.getName() +
                        "\",\"version\":\"" + map.getVersion() + "\"}";

                ContentChangeRequest changeRequest = new ContentChangeRequest(
                        requesterId,
                        ContentActionType.DELETE,
                        ContentType.MAP,
                        map.getId(),
                        targetName,
                        contentJson
                );

                Message request = new Message(ActionType.SUBMIT_CONTENT_CHANGE_REQUEST, changeRequest);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> handleDeleteResponse(response));

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Error: " + e.getMessage()));
            }
        }).start();
    }

    private void handleDeleteResponse(Message response) {
        if (response != null && response.getAction() == ActionType.SUBMIT_CONTENT_CHANGE_RESPONSE) {
            boolean success = (Boolean) response.getMessage();
            if (success) {
                showAlert("Success", "Deletion request submitted for approval!");
            } else {
                showAlert("Error", "Failed to submit deletion request.");
            }
        } else {
            showAlert("Error", "Failed to submit deletion request.");
        }
    }

    @FXML
    private void onApprovals() {
        openApprovalsWindow();
    }

    /**
     * Load the pending approvals count and update the button text.
     * Role-based: Company Manager sees price approvals, Content Manager sees content approvals.
     * Public so it can be called from ApprovalPendingPageController.
     */
    public void refreshPendingApprovalsCount() {
        User user = client.getCurrentUser();
        if (user == null || !(user instanceof Employee)) {
            return;
        }

        Employee employee = (Employee) user;
        EmployeeRole role = employee.getRole();

        // Determine which type of approvals to count based on role
        ActionType requestType;
        if (role == EmployeeRole.COMPANY_MANAGER) {
            // Company Manager sees ONLY price approvals
            requestType = ActionType.GET_PENDING_APPROVALS_REQUEST;
        } else if (role == EmployeeRole.CONTENT_MANAGER) {
            // Content Manager sees ONLY content approvals
            requestType = ActionType.GET_PENDING_CONTENT_APPROVALS_REQUEST;
        } else {
            // Other roles don't see approvals
            return;
        }

        final ActionType finalRequestType = requestType;

        new Thread(() -> {
            try {
                Message request = new Message(finalRequestType, null);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> updateApprovalButtonCount(response, role));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateApprovalButtonCount(Message response, EmployeeRole role) {
        int count = 0;

        if (response != null) {
            if (role == EmployeeRole.COMPANY_MANAGER &&
                    response.getAction() == ActionType.GET_PENDING_APPROVALS_RESPONSE) {
                PendingApprovalsResponse approvals = (PendingApprovalsResponse) response.getMessage();
                count = approvals.getTotalCount();
            } else if (role == EmployeeRole.CONTENT_MANAGER &&
                    response.getAction() == ActionType.GET_PENDING_CONTENT_APPROVALS_RESPONSE) {
                PendingContentApprovalsResponse approvals = (PendingContentApprovalsResponse) response.getMessage();
                count = approvals.getTotalCount();
            }
        }

        // Update button text with appropriate label
        String label = (role == EmployeeRole.COMPANY_MANAGER) ? "Price Approvals" : "Content Approvals";
        btnApprovals.setText(label + " (" + count + ")");
    }

    // ==================== HELPER METHODS ====================

    /**
     * Helper to hide/show all management buttons at once.
     */
    private void setManagementButtonsVisible(boolean visible) {
        btnAddMap.setVisible(visible);
        btnUpdateMap.setVisible(visible);
        btnDeleteMap.setVisible(visible);
        btnPriceUpdate.setVisible(visible);
        btnCreateTour.setVisible(visible);
        btnCreateCity.setVisible(visible);
        btnEditCity.setVisible(visible);
        btnEditTour.setVisible(visible);

    }

    private void openMapUpdateWindow(String mode, GCMMap selected) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapUpdatePage.fxml"));
            Parent root = loader.load();

            MapUpdatePageController controller = loader.getController();
            controller.setMode(mode);
            if (selected != null) {
                controller.setSelectedMap(selected);
            }
            controller.setCatalogController(this);

            Stage stage = new Stage();
            stage.setTitle(mode.equals("add") ? "Add New Map" : "Edit Map");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Refresh after closing
            loadCatalog(cbCity.getValue(), cbMap.getValue(), cbVersion.getValue());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open map editor.");
        }
    }
    // In CatalogPageController
    private void OpenCityUpdateWindow(String mode) {
        try {
            System.out.println(">>> [CATALOG] OpenCityUpdateWindow START - mode=" + mode);
            System.out.println(">>> [CATALOG] After resetState");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/CityUpdatePage.fxml"));
            Parent root = loader.load();
            System.out.println(">>> [CATALOG] FXML loaded");

            CityUpdatePageController controller = loader.getController();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);

            controller.setMode(mode);
            stage.setOnHidden(event -> {
                if (GCMClient.isClientConnected()) {
                    refreshCatalog();
                }
            });

            System.out.println(">>> [CATALOG] Calling stage.show()");
            stage.show();
            System.out.println(">>> [CATALOG] stage.show() completed");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open city editor.");
        }
    }

    private void openApprovalsWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/ApprovalPendingPage.fxml"));
            Parent root = loader.load();

            // Pass reference to this controller
            ApprovalPendingPageController controller = loader.getController();
            controller.setCatalogController(this);

            Stage stage = new Stage();
            stage.setTitle("Pending Approvals");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

            // Refresh after closing
            loadCatalog(cbCity.getValue(), cbMap.getValue(), cbVersion.getValue());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open approvals window.");
        }
    }

    /**
     * Public method to refresh catalog (called from other controllers).
     */
    public void refreshCatalog() {
        loadCatalog(cbCity.getValue(), cbMap.getValue(), cbVersion.getValue());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

