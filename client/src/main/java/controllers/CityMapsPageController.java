package controllers;

import common.content.City;
import common.content.GCMMap;
import common.dto.CatalogFilter;
import common.dto.CatalogResponse;
import common.enums.ActionType;
import common.messaging.Message;
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
import java.util.List;

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

    @FXML private Button btnShowMaps;
    @FXML private Button btnShowTours;

    @FXML
    public void initialize() {
        applyRolePermissions();
    }
    private City selectedCity;

    public void setCity(City city) {
        this.selectedCity = city;
        if (lblCityTitle != null) {
            lblCityTitle.setText("Welcome to " + city.getName());
        }
        if (cbMap != null && city.getMaps() != null) {
            cbMap.getItems().clear();
            for (GCMMap map : city.getMaps()) {
                cbMap.getItems().add(map.getName());
            }
        }
        loadMapsFromServer(city.getName());

    }
    private void loadMapsFromServer(String cityName) {
        new Thread(() -> {
            try {
                // יצירת פילטר לעיר ספציפית
                CatalogFilter filter = new CatalogFilter(cityName, null, null);
                Message request = new Message(ActionType.GET_CATALOG_REQUEST, filter);

                // שליחה וקבלת תשובה מהשרת
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getAction() == ActionType.GET_CATALOG_RESPONSE) {
                        CatalogResponse catalogResponse = (CatalogResponse) response.getMessage();

                        // עדכון רשימת המפות המקומית והצגת הכרטיסים
                        displayMaps(catalogResponse.getMaps());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void displayMaps(List<GCMMap> maps) {
        flowPaneMaps.getChildren().clear();

        if (maps == null || maps.isEmpty()) {
            Label noMaps = new Label("No maps available for this city.");
            flowPaneMaps.getChildren().add(noMaps);
            return;
        }
        selectedCity.setMaps(maps);
        for (GCMMap map : maps) {
            try {
                // טעינת ה-FXML של כרטיס המפה
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapCard.fxml"));
                Parent card = loader.load();

                // הזרקת הנתונים לכרטיס
                MapCardController controller = loader.getController();
                controller.setData(map);

                flowPaneMaps.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadMaps() {
        Platform.runLater(() -> {

            flowPaneMaps.getChildren().clear();

            if (selectedCity == null || selectedCity.getMaps() == null ) return;

            try {
                // 2. מעבר על רשימת המפות של העיר שנבחרה
                for (GCMMap map : selectedCity.getMaps()) {
                    // טעינת ה-FXML של כרטיס המפה הבודד (MapCard.fxml)
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapCard.fxml"));
                    Parent card = loader.load();


                    MapCardController cardController = loader.getController();
                    cardController.setData(map);

                    flowPaneMaps.getChildren().add(card);
                }
            } catch (Exception e) {
                System.err.println("Error rendering map cards: " + e.getMessage());
                e.printStackTrace();
            }
        });
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
                    setButtonState(btnPriceUpdate, false);
                    setButtonState(btnApprovals, false);
                    break;

                case CONTENT_MANAGER:
                    // מנהל תוכן רואה הכל כולל כפתור אישורים
                    setManagementButtonsVisible(true);
                    setButtonState(btnApprovals, true);
                    break;

                case COMPANY_MANAGER:
                    // מנהל חברה רואה רק אישורי מחירים
                    setManagementButtonsVisible(false);
                    setButtonState(btnApprovals, true); // עבור אישורי מחירים בלבד
                    break;

                default:
                    break;
            }
        }
    }

    private void setManagementButtonsVisible(boolean visible) {
        setButtonState(btnEditCity, visible);
        setButtonState(btnCreateTour, visible);
        setButtonState(btnAddMap, visible);
        setButtonState(btnUpdateMap, visible);
        setButtonState(btnDeleteMap, visible);
        setButtonState(btnPriceUpdate, visible);
    }
    private void setButtonState(Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
        }
    }

    @FXML
    private void onShowMapsView() {

        btnShowMaps.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5 5 0 0; -fx-font-weight: bold;");
        btnShowTours.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; -fx-background-radius: 5 5 0 0; -fx-border-color: #bdc3c7; -fx-font-weight: bold;");

        if (flowPaneMaps != null) {
            displayMaps(selectedCity.getMaps());
        }
    }

    @FXML
    private void onShowToursView() {

        btnShowTours.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-background-radius: 5 5 0 0; -fx-font-weight: bold;");
        btnShowMaps.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; -fx-background-radius: 5 5 0 0; -fx-border-color: #bdc3c7; -fx-font-weight: bold;");
        if (flowPaneMaps != null) {
            flowPaneMaps.getChildren().clear();
        }
        displayTours();
    }
    private void displayTours() {
        flowPaneMaps.getChildren().clear();
        if (selectedCity == null || selectedCity.getTours() == null) return;

        // לוגיקה זמנית עד שיהיה לנו TourCard.fxml
        for (common.content.Tour tour : selectedCity.getTours()) {
            Label tourLabel = new Label(tour.getName() + " (" + tour.getRecommendedDuration() + ")");
            flowPaneMaps.getChildren().add(tourLabel);
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
            txtSearch.clear();
        }
        loadMaps();
    }
    @FXML private void onUpdateMap() {}
    @FXML private void onAddMap() {}
    @FXML private void onDeleteMap() {}
    @FXML private void onPriceUpdate() {}
    @FXML private void onEditCity() {}
    @FXML private void onCreateTour() {}
    @FXML private void onApprovals() {}
}