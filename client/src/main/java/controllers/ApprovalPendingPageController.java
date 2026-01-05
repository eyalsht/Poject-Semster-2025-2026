package controllers;

import client.GCMClient;
import common.Message;
import common.PendingPriceUpdate;
import common.actionType;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class ApprovalPendingPageController {

    @FXML private TableView<PendingPriceUpdate> tblPending;
    @FXML private TableColumn<PendingPriceUpdate, String> colType;
    @FXML private TableColumn<PendingPriceUpdate, String> colTarget;
    @FXML private TableColumn<PendingPriceUpdate, String> colInfo;
    @FXML private Button btnChoose;

    private final GCMClient client = GCMClient.getInstance();

    @FXML
    public void initialize() {
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        colTarget.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTarget()));
        colInfo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getInfo()));

        btnChoose.disableProperty().bind(
                tblPending.getSelectionModel().selectedItemProperty().isNull()
        );

        loadPendingFromServer();
    }

    private void loadPendingFromServer() {
        new Thread(() -> {
            try {
                Message req = new Message(actionType.GET_PENDING_PRICE_APPROVALS_REQUEST);
                Object res = client.sendRequest(req);

                if (res instanceof Message msg &&
                        msg.getAction() == actionType.GET_PENDING_PRICE_APPROVALS_RESPONSE) {

                    @SuppressWarnings("unchecked")
                    List<PendingPriceUpdate> list = (List<PendingPriceUpdate>) msg.getMessage();

                    Platform.runLater(() ->
                            tblPending.setItems(FXCollections.observableArrayList(list))
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void onChoose() {
        PendingPriceUpdate selected = tblPending.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapUpdatePage.fxml"));
            Parent root = loader.load();

            MapUpdatePageController ctrl = loader.getController();
            ctrl.setApprovalContext(selected);

            Stage stage = new Stage();
            stage.setTitle("Approval Review");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            // refresh list after approve/deny
            loadPendingFromServer();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
