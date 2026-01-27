package controllers;

import common.content.City;
import common.content.GCMMap;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.control.Label;
import java.io.IOException;

public class CityMapsPageController {

    @FXML private Label lblCityTitle;
    @FXML private Button btnBack;
    @FXML private FlowPane flowPaneMaps;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbMap;
    @FXML private ComboBox<String> cbCity;
    private City selectedCity;

    public void setCity(City city) {
        this.selectedCity = city;
        if (lblCityTitle != null) {
            lblCityTitle.setText("City: " + city.getName());
        }
        loadMaps();
    }

    private void loadMaps() {
        flowPaneMaps.getChildren().clear();

        try {
            if (selectedCity == null || selectedCity.getMaps() == null || selectedCity.getMaps().isEmpty()) {
                return;
            }
            for (GCMMap map : selectedCity.getMaps()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapCard.fxml"));
                Parent card = loader.load();
                MapCardController cardController = loader.getController();
                cardController.setData(map);
                flowPaneMaps.getChildren().add(card);
            }
        } catch (Exception e) {
            System.err.println("Hibernate Lazy error caught: " + e.getMessage());
        }
    }

    @FXML
    private void onBackToCatalog() {
    try{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/CatalogPage.fxml"));
        Parent catalogView = loader.load();
        AnchorPane centerHost = (AnchorPane) btnBack.getScene().lookup("#centerHost");
        if (centerHost != null) {

            AnchorPane.setTopAnchor(catalogView, 0.0);
            AnchorPane.setRightAnchor(catalogView, 0.0);
            AnchorPane.setBottomAnchor(catalogView, 0.0);
            AnchorPane.setLeftAnchor(catalogView, 0.0);


            centerHost.getChildren().setAll(catalogView);
            System.out.println("Returned to main city catalog cleanly.");
        } else {
            System.err.println("Error: Could not find centerHost to return to catalog.");
        }
        }
    catch (IOException e) {
        System.err.println("Failed to load CatalogPage.fxml: " + e.getMessage());
        e.printStackTrace();
    }
    }

    private void loadCatalog(String city, String map, String version) {
        new Thread(() -> {
            try {
                common.dto.CatalogFilter filter = new common.dto.CatalogFilter(city, map, version);
                common.messaging.Message request = new common.messaging.Message(common.enums.ActionType.GET_CATALOG_REQUEST, filter);
                common.messaging.Message response = (common.messaging.Message) client.GCMClient.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getAction() == common.enums.ActionType.GET_CATALOG_RESPONSE) {
                        common.dto.CatalogResponse catalogResponse = (common.dto.CatalogResponse) response.getMessage();
                        flowPaneMaps.getChildren().clear();

                        updateCityCards(catalogResponse.getMaps());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateCityCards(java.util.List<common.content.GCMMap> maps) {
        java.util.Set<Integer> displayedCityIds = new java.util.HashSet<>();
        for (common.content.GCMMap gcmMap : maps) {
            common.content.City city = gcmMap.getCity();
            if (city != null && !displayedCityIds.contains(city.getId())) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/CityCard.fxml"));
                    Parent card = loader.load();
                    controllers.CityCardController controller = loader.getController();
                    controller.setData(gcmMap, null);
                    flowPaneMaps.getChildren().add(card);
                    displayedCityIds.add(city.getId());
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    @FXML
    private void onSearch() {
        if (selectedCity == null || txtSearch == null) return;

        String query = txtSearch.getText().toLowerCase();
        flowPaneMaps.getChildren().clear(); // ניקוי התצוגה לפני הצגת התוצאות

        for (GCMMap map : selectedCity.getMaps()) {
            // בדיקה אם השאילתה מופיעה בשם המפה או בשם של אחד האתרים שבתוכה
            boolean nameMatch = map.getName().toLowerCase().contains(query);
            boolean siteMatch = map.getSites() != null && map.getSites().stream()
                    .anyMatch(s -> s.getName().toLowerCase().contains(query));

            if (nameMatch || siteMatch) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapCard.fxml"));
                    Parent card = loader.load();
                    MapCardController cardController = loader.getController();
                    cardController.setData(map);
                    flowPaneMaps.getChildren().add(card);
                } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    @FXML
    private void onClearSearch() {
        if (txtSearch != null) {
            txtSearch.clear(); // איפוס תיבת הטקסט
        }
        loadMaps(); // טעינה מחדש של כל המפות
    }
    @FXML private void onUpdateMap() {}
    @FXML private void onAddMap() {}
    @FXML private void onDeleteMap() {}
    @FXML private void onPriceUpdate() {}
    @FXML private void onEditCity() {}
    @FXML private void onCreateTour() {}
    @FXML private void onApprovals() {}
}