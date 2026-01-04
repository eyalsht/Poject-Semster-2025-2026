package controllers;
import client.GCMClient;
import common.MapCatalogRow;
import common.Message;
import common.actionType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import common.User;
import common.UserRole;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Button;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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


    private final GCMClient client = GCMClient.getInstance();

    @FXML
    public void initialize() {
        colCity.setCellValueFactory(new PropertyValueFactory<>("city"));
        colMap.setCellValueFactory(new PropertyValueFactory<>("map"));
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
        System.out.println("Approvals clicked");
    }

    private void openMapUpdateWindow(String mode, MapCatalogRow selected) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapUpdatePage.fxml"));
            Parent root = loader.load();

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


}
