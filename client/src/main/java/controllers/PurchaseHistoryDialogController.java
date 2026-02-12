package controllers;

import client.GCMClient;
import common.dto.SubscriptionStatusDTO;
import common.enums.ActionType;
import common.messaging.Message;
import common.purchase.PurchasedMapSnapshot;
import common.user.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PurchaseHistoryDialogController {

    @FXML private Label lblTitle;
    @FXML private TableView<SubscriptionStatusDTO> tblSubscriptions;
    @FXML private TableColumn<SubscriptionStatusDTO, String> colSubCity;
    @FXML private TableColumn<SubscriptionStatusDTO, String> colSubExpiry;
    @FXML private TableColumn<SubscriptionStatusDTO, String> colSubStatus;
    @FXML private TableColumn<SubscriptionStatusDTO, String> colSubPrice;

    @FXML private TableView<PurchasedMapSnapshot> tblPurchases;
    @FXML private TableColumn<PurchasedMapSnapshot, String> colMapName;
    @FXML private TableColumn<PurchasedMapSnapshot, String> colMapCity;
    @FXML private TableColumn<PurchasedMapSnapshot, String> colMapVersion;
    @FXML private TableColumn<PurchasedMapSnapshot, String> colMapDate;
    @FXML private TableColumn<PurchasedMapSnapshot, String> colMapPrice;

    @FXML private Label lblNoSubs;
    @FXML private Label lblNoPurchases;
    @FXML private Button btnClose;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        setupSubscriptionTable();
        setupPurchaseTable();

        User user = GCMClient.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getFirstName() != null && !user.getFirstName().isBlank()
                    ? user.getFirstName() : user.getUsername();
            lblTitle.setText(name + "'s Purchase History");
        }

        loadData();
    }

    private void setupSubscriptionTable() {
        colSubCity.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCityName()));

        colSubExpiry.setCellValueFactory(d -> {
            LocalDate exp = d.getValue().getExpirationDate();
            return new SimpleStringProperty(exp != null ? dtf.format(exp) : "-");
        });

        colSubStatus.setCellValueFactory(d -> {
            SubscriptionStatusDTO dto = d.getValue();
            if (!dto.isActive()) return new SimpleStringProperty("Expired");
            if (dto.isExpiringSoon(7)) return new SimpleStringProperty("Expiring Soon");
            return new SimpleStringProperty("Active");
        });

        colSubPrice.setCellValueFactory(d ->
                new SimpleStringProperty(String.format("$%.2f/mo", d.getValue().getPricePerMonth())));

        // Color-code the status column
        colSubStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("Active".equals(item)) {
                        setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    } else if ("Expiring Soon".equals(item)) {
                        setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
                    } else if ("Expired".equals(item)) {
                        setStyle("-fx-text-fill: #e74c3c;");
                    } else {
                        setStyle("-fx-text-fill: #ecf0f1;");
                    }
                }
            }
        });
    }

    private void setupPurchaseTable() {
        colMapName.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getMapName() != null ? d.getValue().getMapName() : ""));
        colMapCity.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCityName() != null ? d.getValue().getCityName() : ""));
        colMapVersion.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPurchasedVersion() != null ? "v" + d.getValue().getPurchasedVersion() : ""));
        colMapDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPurchaseDate() != null ? dtf.format(d.getValue().getPurchaseDate()) : ""));
        colMapPrice.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("$%.2f", d.getValue().getPricePaid())));
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        User user = GCMClient.getInstance().getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            try {
                Message subReq = new Message(ActionType.GET_USER_SUBSCRIPTIONS_REQUEST, user.getId());
                Message subResp = (Message) GCMClient.getInstance().sendRequest(subReq);

                Message mapReq = new Message(ActionType.GET_USER_PURCHASED_MAPS_REQUEST, user.getId());
                Message mapResp = (Message) GCMClient.getInstance().sendRequest(mapReq);

                Platform.runLater(() -> {
                    // Subscriptions
                    if (subResp != null && subResp.getMessage() instanceof ArrayList<?> list && !list.isEmpty()) {
                        tblSubscriptions.setItems(FXCollections.observableArrayList((List<SubscriptionStatusDTO>) list));
                    } else {
                        tblSubscriptions.setVisible(false);
                        tblSubscriptions.setManaged(false);
                        lblNoSubs.setVisible(true);
                        lblNoSubs.setManaged(true);
                    }

                    // Purchased maps
                    if (mapResp != null && mapResp.getMessage() instanceof ArrayList<?> list2 && !list2.isEmpty()) {
                        tblPurchases.setItems(FXCollections.observableArrayList((List<PurchasedMapSnapshot>) list2));
                    } else {
                        tblPurchases.setVisible(false);
                        tblPurchases.setManaged(false);
                        lblNoPurchases.setVisible(true);
                        lblNoPurchases.setManaged(true);
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    @FXML
    private void onClose() {
        ((Stage) btnClose.getScene().getWindow()).close();
    }
}
