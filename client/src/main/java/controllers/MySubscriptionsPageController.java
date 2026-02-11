package controllers;

import client.GCMClient;
import common.dto.SubscriptionStatusDTO;
import common.enums.ActionType;
import common.messaging.Message;
import common.user.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public class MySubscriptionsPageController {

    @FXML private TableView<SubscriptionStatusDTO> tblSubscriptions;
    @FXML private TableColumn<SubscriptionStatusDTO, String> colCity;
    @FXML private TableColumn<SubscriptionStatusDTO, String> colExpiry;
    @FXML private TableColumn<SubscriptionStatusDTO, String> colStatus;
    @FXML private TableColumn<SubscriptionStatusDTO, String> colPrice;
    @FXML private TableColumn<SubscriptionStatusDTO, String> colAction;
    @FXML private Label lblEmpty;
    @FXML private Button btnRefresh;

    private final ObservableList<SubscriptionStatusDTO> subscriptions = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        tblSubscriptions.setItems(subscriptions);

        colCity.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCityName()));

        colExpiry.setCellValueFactory(d -> {
            LocalDate exp = d.getValue().getExpirationDate();
            return new SimpleStringProperty(exp != null ? dtf.format(exp) : "-");
        });

        colStatus.setCellValueFactory(d -> {
            SubscriptionStatusDTO dto = d.getValue();
            if (!dto.isActive()) return new SimpleStringProperty("Expired");
            LocalDate exp = dto.getExpirationDate();
            if (exp != null && ChronoUnit.DAYS.between(LocalDate.now(), exp) <= 7) {
                return new SimpleStringProperty("Expiring Soon");
            }
            return new SimpleStringProperty("Active");
        });

        colPrice.setCellValueFactory(d ->
            new SimpleStringProperty(String.format("$%.2f/mo", d.getValue().getPricePerMonth())));

        colAction.setCellFactory(new Callback<>() {
            @Override
            public TableCell<SubscriptionStatusDTO, String> call(TableColumn<SubscriptionStatusDTO, String> param) {
                return new TableCell<>() {
                    private final Button btnExtend = new Button("Extend");
                    {
                        btnExtend.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-background-radius: 5;");
                        btnExtend.setOnAction(e -> {
                            SubscriptionStatusDTO dto = getTableView().getItems().get(getIndex());
                            openExtendDialog(dto);
                        });
                    }

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btnExtend);
                        }
                    }
                };
            }
        });

        loadSubscriptions();
    }

    private void loadSubscriptions() {
        User user = GCMClient.getInstance().getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            try {
                Message request = new Message(ActionType.GET_USER_SUBSCRIPTIONS_REQUEST, user.getId());
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    subscriptions.clear();
                    if (response != null && response.getAction() == ActionType.GET_USER_SUBSCRIPTIONS_RESPONSE) {
                        ArrayList<SubscriptionStatusDTO> list = (ArrayList<SubscriptionStatusDTO>) response.getMessage();
                        if (list != null && !list.isEmpty()) {
                            subscriptions.addAll(list);
                            lblEmpty.setVisible(false);
                            lblEmpty.setManaged(false);
                        } else {
                            lblEmpty.setVisible(true);
                            lblEmpty.setManaged(true);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void openExtendDialog(SubscriptionStatusDTO dto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/PurchaseConfirmationDialog.fxml"));
            Parent root = loader.load();

            PurchaseConfirmationDialogController controller = loader.getController();
            controller.setSubscriptionData(dto.getCityName(), dto.getCityId(), dto.getPricePerMonth(), true);

            Stage stage = new Stage();
            stage.setTitle("Extend Subscription - " + dto.getCityName());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            if (controller.isPurchaseComplete()) {
                loadSubscriptions();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onRefresh() {
        loadSubscriptions();
    }
}
