package controllers;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import client.GCMClient;
import common.City;
import common.MapCatalogRow;
import common.MapStatus;
import common.Message;
import common.User;
import common.UserRole;
import common.actionType;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CatalogPageController {

    @FXML private ComboBox<String> cbCity;
    @FXML private ComboBox<String> cbMap;
    @FXML private ComboBox<String> cbVersion;

    @FXML private TableView<MapCatalogRow> tblCatalog;
    @FXML private TableColumn<MapCatalogRow, String> colCity;
    @FXML private TableColumn<MapCatalogRow, String> colMap;
    @FXML private TableColumn<MapCatalogRow, String> colVersion;
    @FXML private TableColumn<MapCatalogRow, Double> colPrice;
    @FXML private TableColumn<MapCatalogRow, String> colDesc;
    @FXML private Button btnUpdateMap;
    @FXML private Button btnAddMap;
    @FXML private Button btnDeleteMap;
    @FXML private Button btnPriceUpdate;
    @FXML private Button btnApprovals;
    
    // Purchase buttons
    @FXML private Button btnSubscribe;
    @FXML private Button btnBuyOneTime;


    private final GCMClient client = GCMClient.getInstance();

    @FXML
    public void initialize()
    {
        colCity.setCellValueFactory(new PropertyValueFactory<>("city"));
        colMap.setCellValueFactory(new PropertyValueFactory<>("mapName"));
        colVersion.setCellValueFactory(new PropertyValueFactory<>("version"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        cbMap.setDisable(true);
        cbVersion.setDisable(true);

        loadCitiesFromServer();
        loadCatalogFromServer(null, null, null);


        cbCity.valueProperty().addListener((obs, oldV, newV) -> {
            cbMap.getItems().clear();
            cbVersion.getItems().clear();

            cbMap.setDisable(newV == null);
            cbVersion.setDisable(true);

            if (newV != null) {
                loadMapsForCityFromServer(newV);
            }

            loadCatalogFromServer(newV, null, null);
        });

        cbMap.valueProperty().addListener((obs, oldV, newV) -> {
            cbVersion.getItems().clear();
            cbVersion.setDisable(newV == null);

            String city = cbCity.getValue();

            if (city != null && newV != null) {
                loadVersionsForCityMapFromServer(city, newV);
            }
            loadCatalogFromServer(city, newV, null);
        });

        cbVersion.valueProperty().addListener((obs, oldV, newV) -> {
            loadCatalogFromServer(cbCity.getValue(), cbMap.getValue(), newV);
        });
        applyRolePermissions();
        setupSelectionRules();
        refreshPriceApprovalsCount();
        
        // Add listener for purchase buttons - enable when city is selected
        tblCatalog.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            // Enable purchase buttons only if a city is selected (via ComboBox)
            String selectedCity = cbCity.getValue();
            boolean hasCity = (selectedCity != null && !selectedCity.isEmpty());
            
            btnSubscribe.setDisable(!hasCity);
            btnBuyOneTime.setDisable(!hasCity);
        });
    }


    private void loadCitiesFromServer() {
        new Thread(() -> {
            try {
                Message request = new Message(actionType.GET_CITY_NAMES_REQUEST);
                Object response = client.sendRequest(request);

                if (response instanceof Message msgResponse && msgResponse.getAction() == actionType.GET_CITY_NAMES_RESPONSE) {
                    List<String> cities = (List<String>) msgResponse.getMessage();
                    Platform.runLater(() -> {
                        if (cities != null) {
                            cbCity.setItems(FXCollections.observableArrayList(cities));
                            System.out.println("Loaded " + cities.size() + " cities");
                        } else {
                            cbCity.setItems(FXCollections.observableArrayList());
                        }
                    });
                } else {
                    System.err.println("Invalid response from server for city names request");
                }
            } catch (Exception e) {
                System.err.println("Error loading cities: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void loadMapsForCityFromServer(String city) {
        new Thread(() -> {
            try {
                Message request = new Message(actionType.GET_MAPS_REQUEST, city);
                Object response = client.sendRequest(request);

                if (response instanceof Message msgResponse && msgResponse.getAction() == actionType.GET_MAPS_RESPONSE) {
                    List<String> maps = (List<String>) msgResponse.getMessage();
                    Platform.runLater(() -> {
                        if (maps != null) {
                            cbMap.setItems(FXCollections.observableArrayList(maps));
                        } else {
                            cbMap.setItems(FXCollections.observableArrayList());
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Error loading maps for city: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void loadVersionsForCityMapFromServer(String city, String map) {
        new Thread(() -> {
            try {
                ArrayList<String> params = new ArrayList<>(Arrays.asList(city, map));
                Message request = new Message(actionType.GET_VERSIONS_REQUEST, params);
                Object response = client.sendRequest(request);

                if (response instanceof Message msgResponse && msgResponse.getAction() == actionType.GET_VERSIONS_RESPONSE) {
                    List<String> versions = (List<String>) msgResponse.getMessage();
                    Platform.runLater(() -> {
                        if (versions != null) {
                            cbVersion.setItems(FXCollections.observableArrayList(versions));
                        } else {
                            cbVersion.setItems(FXCollections.observableArrayList());
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Error loading versions: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void loadCatalogFromServer(String city, String map, String version) {
        new Thread(() -> {
            try {
                String c = (city == null || city.isEmpty()) ? "" : city;
                String m = (map == null || map.isEmpty()) ? "" : map;
                String v = (version == null || version.isEmpty()) ? "" : version;

                ArrayList<String> params = new ArrayList<>(Arrays.asList(c, m, v));
                Message request = new Message(actionType.GET_CATALOG_REQUEST, params);

                Object response = client.sendRequest(request);

                if (response instanceof Message msgResponse && msgResponse.getAction() == actionType.GET_CATALOG_RESPONSE) {
                    List<MapCatalogRow> rows = (List<MapCatalogRow>) msgResponse.getMessage();
                    Platform.runLater(() -> {
                        if (rows != null) {
                            tblCatalog.setItems(FXCollections.observableArrayList(rows));
                            System.out.println("Loaded " + rows.size() + " catalog items");
                        } else {
                            tblCatalog.setItems(FXCollections.observableArrayList());
                            System.out.println("No catalog items received");
                        }
                    });
                } else {
                    System.err.println("Invalid response from server for catalog request");
                    Platform.runLater(() -> tblCatalog.setItems(FXCollections.observableArrayList()));
                }
            } catch (Exception e) {
                System.err.println("Error loading catalog: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> tblCatalog.setItems(FXCollections.observableArrayList()));
            }
        }).start();
    }
    private void applyRolePermissions() {

        setAllButtonsVisible(true);


        setAllButtonsDisabled(true);

        User u = client.getCurrentUser();
        UserRole role = (u == null) ? null : u.getRole();

        if (role == null) {
            return;
        }

        switch (role) {

            case CLIENT -> {

            }

            case CONTENT_WORKER -> {

                btnAddMap.setDisable(false);
                btnUpdateMap.setDisable(false);
                btnDeleteMap.setDisable(false);
            }

            case CONTENT_MANAGER -> {

                btnApprovals.setDisable(false);
                btnPriceUpdate.setDisable(false);

                btnApprovals.setText("Approvals (0)");
            }

            case COMPANY_MANAGER -> {

                btnApprovals.setDisable(false);
                btnApprovals.setText("Approvals (0)");
            }

            default -> {

            }
        }
    }


    private void setupSelectionRules() {

        btnUpdateMap.disableProperty().unbind();
        btnDeleteMap.disableProperty().unbind();

        btnUpdateMap.disableProperty().bind(
                Bindings.createBooleanBinding(() -> {
                    User u = client.getCurrentUser();
                    UserRole role = (u == null) ? null : u.getRole();

                    boolean isWorker = (role == UserRole.CONTENT_WORKER);
                    boolean hasSelection = tblCatalog.getSelectionModel().getSelectedItem() != null;

                    // only content_worker can update, and only with a selected row
                    return !(isWorker && hasSelection);
                }, tblCatalog.getSelectionModel().selectedItemProperty())
        );

        btnDeleteMap.disableProperty().bind(
                Bindings.createBooleanBinding(() -> {
                    User u = client.getCurrentUser();
                    UserRole role = (u == null) ? null : u.getRole();

                    boolean isWorker = (role == UserRole.CONTENT_WORKER);
                    boolean hasSelection = tblCatalog.getSelectionModel().getSelectedItem() != null;

                    // only content_worker can delete, and only with a selected row
                    return !(isWorker && hasSelection);
                }, tblCatalog.getSelectionModel().selectedItemProperty())
        );
    }


    private void setAllButtonsDisabled(boolean disabled) {
        btnUpdateMap.setDisable(disabled);
        btnAddMap.setDisable(disabled);
        btnDeleteMap.setDisable(disabled);
        btnPriceUpdate.setDisable(disabled);
        btnApprovals.setDisable(disabled);
    }

    private void setAllButtonsVisible(boolean visible) {
        btnUpdateMap.setVisible(visible);
        btnAddMap.setVisible(visible);
        btnDeleteMap.setVisible(visible);
        btnPriceUpdate.setVisible(visible);
        btnApprovals.setVisible(visible);
    }
    @FXML
    private void onPriceUpdate() {
        MapCatalogRow selected = tblCatalog.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.out.println("Price Update clicked but no row selected");
            return;
        }

        openMapUpdateWindow("PRICE_UPDATE", selected);
    }

    @FXML
    private void onAddMap() {
        openMapUpdateWindow("ADD", null);
    }

    @FXML
    private void onUpdateMap() {
        MapCatalogRow selected = tblCatalog.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        openMapUpdateWindow("UPDATE", selected);
    }

    @FXML
    private void onDeleteMap() {
        MapCatalogRow selected = tblCatalog.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        System.out.println("Delete clicked for: " + selected);
    }

    @FXML
    private void onApprovals()
    {
        openApprovalsWindow();
        refreshPriceApprovalsCount();
    }

    private void openApprovalsWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/ApprovalPendingPage.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Pending Approvals");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openMapUpdateWindow(String mode, MapCatalogRow selected) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapUpdatePage.fxml"));
            Parent root = loader.load();

            MapUpdatePageController ctrl = loader.getController();

            MapStatus m = switch (mode) {
                case "ADD" -> MapStatus.ADD;
                case "UPDATE" -> MapStatus.UPDATE;
                case "PRICE_UPDATE" -> MapStatus.PRICE_UPDATE_REQUEST;
                case "APPROVAL" -> MapStatus.APPROVAL_REVIEW;
                default -> MapStatus.UPDATE;
            };

            ctrl.setContext(m, selected);

            Stage stage = new Stage();
            stage.setTitle("Map Update");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void refreshPriceApprovalsCount() {
        new Thread(() -> {
            try {
                Object res = GCMClient.getInstance().sendRequest(
                        new Message(actionType.GET_PENDING_PRICE_APPROVALS_COUNT_REQUEST, null)
                );

                if (res instanceof Message msg &&
                        msg.getAction() == actionType.GET_PENDING_PRICE_APPROVALS_COUNT_RESPONSE) {

                    int count = (int) msg.getMessage();

                    Platform.runLater(() ->
                            btnApprovals.setText("Approvals (" + count + ")")
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Handler for Subscribe to City button.
     * Opens the PurchaseDialog with SUBSCRIPTION type.
     */
    @FXML
    private void handleSubscribeAction() {
        String cityName = cbCity.getValue();
        if (cityName == null || cityName.isEmpty()) {
            System.err.println("No city selected for subscription");
            return;
        }
        
        // Fetch city data from server
        fetchCityAndOpenDialog(cityName, "SUBSCRIPTION");
    }

    /**
     * Handler for Buy Collection button.
     * Opens the PurchaseDialog with ONE_TIME type.
     */
    @FXML
    private void handleBuyOneTimeAction() {
        String cityName = cbCity.getValue();
        if (cityName == null || cityName.isEmpty()) {
            System.err.println("No city selected for one-time purchase");
            return;
        }
        
        // Fetch city data from server
        fetchCityAndOpenDialog(cityName, "ONE_TIME");
    }

    /**
     * Fetches city data from the server and opens the purchase dialog.
     */
    private void fetchCityAndOpenDialog(String cityName, String purchaseType) {
        new Thread(() -> {
            try {
                // Request all cities from server
                Message request = new Message(actionType.GET_ALL_CITIES_REQUEST);
                Object response = client.sendRequest(request);

                if (response instanceof Message msgResponse && 
                    msgResponse.getAction() == actionType.GET_ALL_CITIES_RESPONSE) {
                    
                    ArrayList<City> cities = (ArrayList<City>) msgResponse.getMessage();
                    
                    if (cities != null) {
                        // Find the city by name
                        City city = null;
                        for (City c : cities) {
                            if (c.getName().equals(cityName)) {
                                city = c;
                                break;
                            }
                        }
                        
                        if (city != null) {
                            final City foundCity = city;
                            Platform.runLater(() -> openPurchaseDialog(foundCity, purchaseType));
                        } else {
                            System.err.println("City not found: " + cityName);
                        }
                    } else {
                        System.err.println("No cities received from server");
                    }
                } else {
                    System.err.println("Invalid response from server for cities request");
                }
            } catch (Exception e) {
                System.err.println("Error fetching city data: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void openPurchaseDialog(City city, String purchaseType) {
        try {
            // 1. Get User ID (Safety Check)
            int userId = 1; // Default dummy for testing
            User currentUser = client.getCurrentUser();
            if (currentUser != null) {
                userId = currentUser.getId();
            } else {
                System.err.println("!!! WARNING: No logged-in user found. Using Dummy ID: 1 !!!");
            }

            // 2. Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/PurchaseDialog.fxml"));
            Parent root = loader.load();

            // 3. Get Controller
            PurchaseDialogController controller = loader.getController();
            
            // 4. Calculate base price (Safe check for nulls)
            double basePrice;
            if ("SUBSCRIPTION".equals(purchaseType)) {
                basePrice = city.getPriceSub(); // Monthly subscription price
            } else {
                basePrice = city.getPriceOneTime(); // One-time purchase price
            }
            
            // Fallback if price is missing or invalid
            if (basePrice <= 0) {
                System.err.println("WARNING: Invalid price (" + basePrice + ") for city " + city.getName() + ". Using fallback price: 100.0");
                basePrice = 100.0;
            }

            // 5. Initialize the dialog with data
            controller.initData(
                userId,                    // userId
                city.getID(),              // cityId
                null,                      // mapId (not used for city purchases)
                city.getName(),            // itemName
                purchaseType,              // "SUBSCRIPTION" or "ONE_TIME"
                basePrice                  // price
            );

            // 6. Create and show the dialog stage
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Secure Purchase - " + city.getName());
            dialogStage.setScene(new Scene(root));
            dialogStage.initModality(Modality.WINDOW_MODAL);
            
            // Set owner to current window (prevents dialog from appearing behind main window)
            if (btnSubscribe != null && btnSubscribe.getScene() != null && btnSubscribe.getScene().getWindow() != null) {
                dialogStage.initOwner(btnSubscribe.getScene().getWindow());
            }
            
            dialogStage.setResizable(false);
            
            // Pass stage to controller so it can close itself
            controller.setDialogStage(dialogStage);
            
            dialogStage.showAndWait();

        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to open purchase dialog!");
            e.printStackTrace(); // This will show us WHY it crashes
            
            // Show alert to user
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not open purchase window");
            alert.setContentText("Check console for details.\n" + e.getMessage());
            alert.showAndWait();
        }
    }




}
