package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ApprovalPendingPageController {

    @FXML private TableView<String> tblPending;
    @FXML private TableColumn<String, String> colType;
    @FXML private TableColumn<String, String> colTarget;
    @FXML private Button btnChoose;

    @FXML
    public void initialize()
    {
        // JUST A DEMO!!!!!
        tblPending.setItems(FXCollections.observableArrayList(
                "PRICE | Haifa - Downtown",
                "UPDATE | Tel Aviv - Center"
        ));

        btnChoose.disableProperty().bind(
                tblPending.getSelectionModel().selectedItemProperty().isNull()
        );
    }

    @FXML
    private void onChoose() {
        String selected = tblPending.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        System.out.println("Chosen pending approval: " + selected);

        // later you will open MapUpdatePage in APPROVAL_REVIEW with the real PendingUpdate object
        Stage stage = (Stage) btnChoose.getScene().getWindow();
        stage.close();
    }
}
