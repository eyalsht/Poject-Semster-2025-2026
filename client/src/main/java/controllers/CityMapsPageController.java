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
import client.GCMClient;
import common.user.User;
import common.user.Employee;
import common.enums.EmployeeRole;

public class CityMapsPageController {

    @FXML private Label lblCityTitle;
    @FXML private Button btnBack;
    @FXML private FlowPane flowPaneMaps;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbMap;
    @FXML private ComboBox<String> cbCity;

    @FXML private Button btnUpdateMap;
    @FXML private Button btnAddMap;
    @FXML private Button btnDeleteMap;
    @FXML private Button btnPriceUpdate;
    @FXML private Button btnApprovals;
    @FXML private Button btnEditCity;
    @FXML private Button btnCreateTour;

    @FXML
    public void initialize() {
        applyRolePermissions();
    }
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

    private void applyRolePermissions() {
        User user = GCMClient.getInstance().getCurrentUser();

        // הסתרת כל כפתורי הניהול כברירת מחדל (לאורחים ולקוחות)
        setManagementButtonsVisible(false);
        setButtonState(btnApprovals, false);
        if (user instanceof Employee employee) {
            EmployeeRole role = employee.getRole();
            switch (role) {
                case CONTENT_WORKER:
                    // עובד תוכן יכול לערוך אך לא לאשר מחירים או גרסאות
                    setManagementButtonsVisible(true);
                    btnPriceUpdate.setVisible(false);
                    btnApprovals.setVisible(false);
                    break;

                case CONTENT_MANAGER:
                    // מנהל תוכן רואה הכל כולל כפתור אישורים
                    setManagementButtonsVisible(true);
                    btnApprovals.setVisible(true);
                    break;

                case COMPANY_MANAGER:
                    // מנהל חברה רואה רק אישורי מחירים
                    setManagementButtonsVisible(false);
                    btnApprovals.setVisible(true); // עבור אישורי מחירים בלבד
                    break;
            }
        }
    }

    private void setManagementButtonsVisible(boolean visible) {
        btnEditCity.setVisible(visible);
        btnCreateTour.setVisible(visible);
        btnAddMap.setVisible(visible);
        btnUpdateMap.setVisible(visible);
        btnDeleteMap.setVisible(visible);
        btnPriceUpdate.setVisible(visible);
    }
    private void setButtonState(Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
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