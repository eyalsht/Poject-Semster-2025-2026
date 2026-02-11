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
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import client.GCMClient;
import common.dto.PendingApprovalsResponse;
import common.dto.PendingContentApprovalsResponse;
import common.dto.SubscriptionStatusDTO;
import common.user.User;
import common.user.Client;
import common.user.Employee;
import common.enums.EmployeeRole;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class CityMapsPageController {

    @FXML private Label lblCityTitle;
    @FXML private Button btnBack;
    @FXML private FlowPane flowPaneMaps;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbMap;
    @FXML private ComboBox<String> cbCity;

    @FXML private Button btnUpdateMap;
    @FXML private Button btnPriceUpdate;
    @FXML private Button btnApprovals;
    @FXML private Button btnEditCity;
    @FXML private Button btnSubscribe;
    private boolean isLoading = false;
    private SubscriptionStatusDTO subscriptionStatus;

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
        if (city.getMaps() != null && !city.getMaps().isEmpty()) {
            displayMaps(city.getMaps());
        } else if (!isLoading) {
            loadMapsFromServer(city.getName());
        }

        // Check subscription status for clients
        checkSubscriptionStatus(city);
    }

    private void updateMapComboBox(List<GCMMap> maps) {
        if (cbMap != null && maps != null) {
            cbMap.getItems().clear();
            for (GCMMap map : maps) {
                cbMap.getItems().add(map.getName());
            }
        }
    }

    private void loadMapsFromServer(String cityName) {
        if (isLoading) return; // אם כבר יש טעינה בדרך, אל תתחיל חדשה

        isLoading = true;
        new Thread(() -> {
            try {
                CatalogFilter filter = new CatalogFilter(cityName, null, null);
                Message request = new Message(ActionType.GET_CATALOG_REQUEST, filter);
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                        if (response != null && response.getAction() == ActionType.GET_CATALOG_RESPONSE) {
                            CatalogResponse catalogResponse = (CatalogResponse) response.getMessage();
                            selectedCity.setMaps(catalogResponse.getMaps());
                            renderMapCards(selectedCity.getMaps());
                        }

                        isLoading = false; // שחרור המנעול לאחר העדכון
                });
            } catch (Exception e) {
                isLoading = false;
                e.printStackTrace();
            }
        }).start();
    }
    private void displayMaps(List<GCMMap> maps) {
        if (maps == null || selectedCity == null) return;
        selectedCity.setMaps(maps);
        List<Parent> cards = new java.util.ArrayList<>();
        java.util.Set<Integer> seenMapIds = new java.util.HashSet<>();


        // 2. הכנת רשימת הכרטיסים ב-Thread הנוכחי (לא ב-UI Thread) כדי לא לתקוע את המסך
        List<Parent> newCards = new java.util.ArrayList<>();
        for (GCMMap map : maps) {
            if (map != null && !seenMapIds.contains(map.getId())) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapCard.fxml"));
                    Parent card = loader.load();
                    MapCardController controller = loader.getController();
                    controller.setData(map);
                    newCards.add(card);
                    seenMapIds.add(map.getId());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        Platform.runLater(() -> {
            if (flowPaneMaps != null) {
                flowPaneMaps.getChildren().setAll(newCards);
                System.out.println("Displaying " + newCards.size() + " unique maps for " + selectedCity.getName());
            }
        });
      /*  Platform.runLater(() -> {
        flowPaneMaps.getChildren().clear();

        if (maps == null || maps.isEmpty()) {
            Label noMaps = new Label("No maps available for this city.");
            flowPaneMaps.getChildren().add(noMaps);
            return;
        }
        selectedCity.setMaps(maps);
        for (GCMMap map : maps) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapCard.fxml"));
                Parent card = loader.load();
                MapCardController controller = loader.getController();
                controller.setData(map);
                flowPaneMaps.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    });*/
    }

    private void renderMapCards(List<GCMMap> mapsToDisplay) {
        if (mapsToDisplay == null || flowPaneMaps == null) return;

        Platform.runLater(() -> {
            flowPaneMaps.getChildren().clear();
            Set<Integer> seenIds = new HashSet<>();
            List<Parent> newCards = new ArrayList<>();

            for (GCMMap map : mapsToDisplay) {
                if (map != null && !seenIds.contains(map.getId())) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapCard.fxml"));
                        Parent card = loader.load();
                        MapCardController controller = loader.getController();
                        controller.setData(map);

                        newCards.add(card);
                        seenIds.add(map.getId());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            flowPaneMaps.getChildren().setAll(newCards);
            System.out.println("Rendered " + newCards.size() + " unique maps for " + selectedCity.getName());
        });
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
                    btnApprovals.setText("Content Approvals");
                    refreshPendingApprovalsCount();
                    break;

                case COMPANY_MANAGER:
                    // מנהל חברה רואה רק אישורי מחירים
                    setManagementButtonsVisible(false);
                    setButtonState(btnApprovals, true); // עבור אישורי מחירים בלבד
                    btnApprovals.setText("Price Approvals");
                    refreshPendingApprovalsCount();
                    break;

                default:
                    break;
            }
        }
    }

    private void setManagementButtonsVisible(boolean visible) {
        setButtonState(btnEditCity, visible);
        setButtonState(btnUpdateMap, visible);
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

        if (selectedCity != null && flowPaneMaps != null) {
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
        Platform.runLater(() -> {
            flowPaneMaps.getChildren().clear();
            if (selectedCity == null || selectedCity.getTours() == null) return;

            for (common.content.Tour tour : selectedCity.getTours()) {
                Label tourLabel = new Label(tour.getName() + " (" + tour.getRecommendedDuration() + ")");
                flowPaneMaps.getChildren().add(tourLabel);
            }
        });
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

    private void updateCityCards(List<GCMMap> maps) {
        List<Parent> cityCards = new ArrayList<>();
        Set<Integer> seenCityIds = new HashSet<>();
        for (common.content.GCMMap gcmMap : maps) {
            City city = gcmMap.getCity();
            if (city != null && !seenCityIds.contains(city.getId())) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/CityCard.fxml"));
                    Parent card = loader.load();
                    CityCardController controller = loader.getController();
                    controller.setData(gcmMap, this);

                    cityCards.add(card);
                    seenCityIds.add(city.getId());
                } catch (Exception e) {
                    System.err.println("Error loading city card: " + e.getMessage());
                }
            }
        }
    }

    @FXML
    private void onSearch() {
        if (selectedCity == null || txtSearch == null) return;

        String query = txtSearch.getText().toLowerCase();
        List<GCMMap> filtered = selectedCity.getMaps().stream()
                .filter(map -> map.getName().toLowerCase().contains(query) ||
                        (map.getSites() != null && map.getSites().stream()
                                .anyMatch(s -> s.getName().toLowerCase().contains(query))))
                .toList();

        renderMapCards(filtered);
        /*
        flowPaneMaps.getChildren().clear(); // ניקוי התצוגה לפני הצגת התוצאות
        java.util.Set<Integer> addedMapIds = new java.util.HashSet<>();
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
        }*/
    }

    @FXML
    private void onClearSearch() {
        if (txtSearch != null) {
            txtSearch.clear();
        }
        if (selectedCity != null) renderMapCards(selectedCity.getMaps());
    }
    @FXML private void onUpdateMap() {}
    @FXML private void onAddMap() {}
    @FXML private void onDeleteMap() {}

    @FXML
    private void onPriceUpdate() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/PriceUpdateDialog.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Price Update Request");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Refresh maps after dialog closes
            if (selectedCity != null) {
                loadMapsFromServer(selectedCity.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open Price Update dialog: " + e.getMessage());
        }
    }

    @FXML private void onEditCity() {}
    @FXML private void onCreateTour() {}

    @FXML
    private void onApprovals() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/ApprovalPendingPage.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Pending Approvals");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

            // Refresh maps and approval count when the window is closed
            stage.setOnHidden(event -> {
                if (selectedCity != null) {
                    loadMapsFromServer(selectedCity.getName());
                }
                refreshPendingApprovalsCount();
            });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open approvals window: " + e.getMessage());
        }
    }

    private void refreshPendingApprovalsCount() {
        User user = GCMClient.getInstance().getCurrentUser();
        if (user == null || !(user instanceof Employee)) {
            return;
        }

        Employee employee = (Employee) user;
        EmployeeRole role = employee.getRole();

        ActionType requestType;
        if (role == EmployeeRole.COMPANY_MANAGER) {
            requestType = ActionType.GET_PENDING_APPROVALS_REQUEST;
        } else if (role == EmployeeRole.CONTENT_MANAGER) {
            requestType = ActionType.GET_PENDING_CONTENT_APPROVALS_REQUEST;
        } else {
            return;
        }

        final ActionType finalRequestType = requestType;

        new Thread(() -> {
            try {
                Message request = new Message(finalRequestType, null);
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

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

        String label = (role == EmployeeRole.COMPANY_MANAGER) ? "Price Approvals" : "Content Approvals";
        btnApprovals.setText(label + " (" + count + ")");
    }

    private void checkSubscriptionStatus(City city) {
        User user = GCMClient.getInstance().getCurrentUser();
        if (!(user instanceof Client) || btnSubscribe == null) return;

        new Thread(() -> {
            try {
                ArrayList<Integer> params = new ArrayList<>();
                params.add(user.getId());
                params.add(city.getId());

                Message request = new Message(ActionType.CHECK_SUBSCRIPTION_STATUS_REQUEST, params);
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

                if (response != null && response.getAction() == ActionType.CHECK_SUBSCRIPTION_STATUS_RESPONSE) {
                    SubscriptionStatusDTO status = (SubscriptionStatusDTO) response.getMessage();
                    this.subscriptionStatus = status;

                    Platform.runLater(() -> {
                        if (status.isActive()) {
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                            btnSubscribe.setText("Subscribed (expires " + dtf.format(status.getExpirationDate()) + ")");
                            btnSubscribe.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
                        } else {
                            btnSubscribe.setText(String.format("Subscribe ($%.2f/mo)", status.getPricePerMonth()));
                            btnSubscribe.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
                        }
                        btnSubscribe.setVisible(true);
                        btnSubscribe.setManaged(true);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void onSubscribe() {
        if (selectedCity == null || subscriptionStatus == null) return;

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/GUI/PurchaseConfirmationDialog.fxml"));
            javafx.scene.Parent root = loader.load();

            PurchaseConfirmationDialogController controller = loader.getController();
            controller.setSubscriptionData(
                selectedCity.getName(),
                selectedCity.getId(),
                subscriptionStatus.getPricePerMonth(),
                subscriptionStatus.isActive()
            );

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Subscribe - " + selectedCity.getName());
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Refresh subscription status after dialog
            if (controller.isPurchaseComplete()) {
                checkSubscriptionStatus(selectedCity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}