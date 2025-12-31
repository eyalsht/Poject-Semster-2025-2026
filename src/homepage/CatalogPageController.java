package homepage;

import client.GCMClient;
import entities.MapCatalogRow;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
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

        // Bind columns
        colCity.setCellValueFactory(new PropertyValueFactory<>("city"));
        colMap.setCellValueFactory(new PropertyValueFactory<>("map"));
        colVersion.setCellValueFactory(new PropertyValueFactory<>("version"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        // UX default
        cbMap.setDisable(true);
        cbVersion.setDisable(true);

        // Load initial data from DB (cities + full catalog)
        loadCitiesFromServer();
        loadCatalogFromServer(null, null, null);

        // Listeners
        cbCity.valueProperty().addListener((obs, oldV, newV) -> {
            cbMap.getItems().clear();
            cbVersion.getItems().clear();

            cbMap.setDisable(newV == null);
            cbVersion.setDisable(true);

            if (newV != null) {
                loadMapsForCityFromServer(newV);
            }
            // refresh table (filter by city only)
            loadCatalogFromServer(newV, null, null);
        });

        cbMap.valueProperty().addListener((obs, oldV, newV) -> {
            cbVersion.getItems().clear();
            cbVersion.setDisable(newV == null);

            String city = cbCity.getValue();

            if (city != null && newV != null) {
                loadVersionsForCityMapFromServer(city, newV);
            }
            // refresh table (filter by city + map)
            loadCatalogFromServer(city, newV, null);
        });

        cbVersion.valueProperty().addListener((obs, oldV, newV) -> {
            // refresh table (filter by city + map + version)
            loadCatalogFromServer(cbCity.getValue(), cbMap.getValue(), newV);
        });
    }

    // -------------------- SERVER CALLS (run in background thread) --------------------

    private void loadCitiesFromServer() {
        new Thread(() -> {
            Object resp = client.sendRequest("get_cities");
            List<String> cities = castStringList(resp);

            Platform.runLater(() -> {
                cbCity.setItems(FXCollections.observableArrayList(cities));
            });
        }).start();
    }

    private void loadMapsForCityFromServer(String city) {
        new Thread(() -> {
            Object resp = client.sendRequest("get_maps:" + city);
            List<String> maps = castStringList(resp);

            Platform.runLater(() -> {
                cbMap.setItems(FXCollections.observableArrayList(maps));
            });
        }).start();
    }

    private void loadVersionsForCityMapFromServer(String city, String map) {
        new Thread(() -> {
            Object resp = client.sendRequest("get_versions:" + city + ":" + map);
            List<String> versions = castStringList(resp);

            Platform.runLater(() -> {
                cbVersion.setItems(FXCollections.observableArrayList(versions));
            });
        }).start();
    }

    private void loadCatalogFromServer(String city, String map, String version) {
        new Thread(() -> {
            // empty fields allowed:
            String c = safe(city);
            String m = safe(map);
            String v = safe(version);

            Object resp = client.sendRequest("get_catalog:" + c + ":" + m + ":" + v);
            System.out.println("CLIENT resp type = " + (resp == null ? "null" : resp.getClass()));
            if (resp instanceof List<?> list) {
                System.out.println("CLIENT resp list size = " + list.size());
                if (!list.isEmpty() && list.get(0) != null) {
                    System.out.println("CLIENT first element class = " + list.get(0).getClass());
                }
            }
            List<MapCatalogRow> rows = castCatalogRows(resp);
            System.out.println("CLIENT castCatalogRows size = " + rows.size());

            Platform.runLater(() -> {
                tblCatalog.setItems(FXCollections.observableArrayList(rows));
            });
        }).start();
    }

    // -------------------- helpers --------------------

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object resp) {
        if (resp instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) out.add(o.toString());
            }
            return out;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<MapCatalogRow> castCatalogRows(Object resp) {
        if (resp instanceof List<?> list) {
            List<MapCatalogRow> out = new ArrayList<>();
            for (Object o : list) {
                if (o instanceof MapCatalogRow row) out.add(row);
            }
            return out;
        }
        return List.of();
    }

}
