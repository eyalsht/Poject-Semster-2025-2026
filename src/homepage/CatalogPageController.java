package homepage;
import client.GCMClient;
import entities.MapCatalogRow;
import entities.Message;
import entities.actionType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

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

    private final GCMClient client = GCMClient.getInstance();

    @FXML
    public void initialize() {
        // קישור עמודות הטבלה לשדות במחלקה MapCatalogRow
        colCity.setCellValueFactory(new PropertyValueFactory<>("city"));
        colMap.setCellValueFactory(new PropertyValueFactory<>("map"));
        colVersion.setCellValueFactory(new PropertyValueFactory<>("version"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        // הגדרת מצב התחלתי לפקדים
        cbMap.setDisable(true);
        cbVersion.setDisable(true);

        // טעינת נתונים ראשונית
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
    }


    private void loadCitiesFromServer() {
        new Thread(() -> {
            Message request = new Message(actionType.GET_CITY_NAMES_REQUEST);
            Object response = client.sendRequest(request);

            if (response instanceof Message msgResponse && msgResponse.getAction() == actionType.GET_CITY_NAMES_RESPONSE) {
                List<String> cities = (List<String>) msgResponse.getMessage();
                Platform.runLater(() -> cbCity.setItems(FXCollections.observableArrayList(cities)));
            }
        }).start();
    }

    private void loadMapsForCityFromServer(String city) {
        new Thread(() -> {
            Message request = new Message(actionType.GET_MAPS_REQUEST, city);
            Object response = client.sendRequest(request);

            if (response instanceof Message msgResponse && msgResponse.getAction() == actionType.GET_MAPS_RESPONSE) {
                List<String> maps = (List<String>) msgResponse.getMessage();
                Platform.runLater(() -> cbMap.setItems(FXCollections.observableArrayList(maps)));
            }
        }).start();
    }

    private void loadVersionsForCityMapFromServer(String city, String map) {
        new Thread(() -> {
            ArrayList<String> params = new ArrayList<>(Arrays.asList(city, map));
            Message request = new Message(actionType.GET_VERSIONS_REQUEST, params);
            Object response = client.sendRequest(request);

            if (response instanceof Message msgResponse && msgResponse.getAction() == actionType.GET_VERSIONS_RESPONSE) {
                List<String> versions = (List<String>) msgResponse.getMessage();
                Platform.runLater(() -> cbVersion.setItems(FXCollections.observableArrayList(versions)));
            }
        }).start();
    }

    private void loadCatalogFromServer(String city, String map, String version) {
        new Thread(() -> {

            String c = (city == null) ? "" : city;
            String m = (map == null) ? "" : map;
            String v = (version == null) ? "" : version;

            ArrayList<String> params = new ArrayList<>(Arrays.asList(c, m, v));
            Message request = new Message(actionType.GET_CATALOG_REQUEST, params);

            Object response = client.sendRequest(request);

            if (response instanceof Message msgResponse && msgResponse.getAction() == actionType.GET_CATALOG_RESPONSE) {
                List<MapCatalogRow> rows = (List<MapCatalogRow>) msgResponse.getMessage();
                Platform.runLater(() -> tblCatalog.setItems(FXCollections.observableArrayList(rows)));
            }
        }).start();
    }
}
