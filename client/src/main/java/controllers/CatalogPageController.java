package controllers;
import client.GCMClient;
import common.content.GCMMap;
import common.dto.CatalogFilter;
import common.dto.CatalogResponse;
import common.dto.PendingApprovalsResponse;  // Add this import
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
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the catalog page.
 * Displays cities, maps, and allows filtering.
 */
public class CatalogPageController {

    @FXML private ComboBox<String> cbCity;
    @FXML private ComboBox<String> cbMap;
    @FXML private ComboBox<String> cbVersion;

    @FXML private TableView<GCMMap> tblCatalog;
    @FXML private TableColumn<GCMMap, String> colCity;
    @FXML private TableColumn<GCMMap, String> colMap;
    @FXML private TableColumn<GCMMap, String> colVersion;
    @FXML private TableColumn<GCMMap, Double> colPrice;
    @FXML private TableColumn<GCMMap, String> colDesc;

    @FXML private Button btnUpdateMap;
    @FXML private Button btnAddMap;
    @FXML private Button btnDeleteMap;
    @FXML private Button btnPriceUpdate;
    @FXML private Button btnApprovals;

    private final GCMClient client = GCMClient.getInstance();
    
    // Cache the last response for filter cascading
    private CatalogResponse lastCatalogResponse;
    
    // Flag to prevent recursive updates
    private boolean isUpdatingComboBoxes = false;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupComboBoxListeners();
        applyRolePermissions();
        loadCatalog(null, null, null);
        refreshPendingApprovalsCount();  // Updated method name
    }

    /**
     * Setup table column bindings.
     * Using GCMMap entity directly instead of MapCatalogRow DTO.
     */
    private void setupTableColumns() {
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
    }

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

            // Update table
            tblCatalog.setItems(FXCollections.observableArrayList(catalogResponse.getMaps()));

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
     */
    private void applyRolePermissions() {
        User user = client.getCurrentUser();
        
        // Hide all management buttons by default
        setManagementButtonsVisible(false);

        if (user == null) {
            return;  // Guest - no management buttons
        }

        // Show buttons based on user type/role
        if (user instanceof Employee employee) {
            EmployeeRole role = employee.getRole();
            if (role != null) {
                switch (role) {
                    case COMPANY_MANAGER:
                        setManagementButtonsVisible(true);
                        break;
                    case CONTENT_MANAGER:
                    case CONTENT_WORKER:
                        btnAddMap.setVisible(true);
                        btnUpdateMap.setVisible(true);
                        btnPriceUpdate.setVisible(true);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void setManagementButtonsVisible(boolean visible) {
        btnAddMap.setVisible(visible);
        btnUpdateMap.setVisible(visible);
        btnDeleteMap.setVisible(visible);
        btnPriceUpdate.setVisible(visible);
        btnApprovals.setVisible(visible);
    }

    // ==================== BUTTON HANDLERS ====================

    @FXML
    private void onPriceUpdate() {
        GCMMap selected = tblCatalog.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a map to update price.");
            return;
        }
        openMapUpdateWindow("price", selected);
    }

    @FXML
    private void onAddMap() {
        openMapUpdateWindow("add", null);
    }

    @FXML
    private void onUpdateMap() {
        GCMMap selected = tblCatalog.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a map to update.");
            return;
        }
        openMapUpdateWindow("edit", selected);
    }

    @FXML
    private void onDeleteMap() {
        GCMMap selected = tblCatalog.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a map to delete.");
            return;
        }
        // TODO: Implement delete confirmation and request
    }

    @FXML
    private void onApprovals() {
        openApprovalsWindow();
    }

    /**
     * Load the pending approvals count and update the button text.
     * Public so it can be called from ApprovalPendingPageController.
     */
    public void refreshPendingApprovalsCount() {
        new Thread(() -> {
            try {
                Message request = new Message(ActionType.GET_PENDING_APPROVALS_REQUEST, null);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getAction() == ActionType.GET_PENDING_APPROVALS_RESPONSE) {
                        PendingApprovalsResponse approvals = (PendingApprovalsResponse) response.getMessage();
                        int count = approvals.getTotalCount();
                        btnApprovals.setText("Approvals (" + count + ")");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ==================== HELPER METHODS ====================

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
            stage.setTitle(mode.equals("add") ? "Add New Map" : "Update Map");
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
            stage.showAndWait();

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
