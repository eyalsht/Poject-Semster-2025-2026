package controllers;

import client.GCMClient;
import common.MapCatalogRow;
import common.MapUpdateMode;
import common.User;
import common.UserRole;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class MapUpdatePageController {

    // LEFT (current)
    @FXML private Label lblCurCity;
    @FXML private Label lblCurMap;
    @FXML private Label lblCurPrice;
    @FXML private TextArea taCurDesc;

    // RIGHT (update)
    @FXML private TextField tfNewCity;
    @FXML private TextField tfNewMap;
    @FXML private TextField tfNewPrice;
    @FXML private TextArea taNewDesc;

    // Buttons
    @FXML private Button btnClose;
    @FXML private Button btnAddUpdate;
    @FXML private Button btnApprove;

    private final GCMClient client = GCMClient.getInstance();

    private MapUpdateMode mode = MapUpdateMode.UPDATE;
    private MapCatalogRow currentRow;

    @FXML
    public void initialize()
    {
        setRightEditable(false, false, false, false);
        if (btnApprove != null) btnApprove.setVisible(false);
    }


    public void setContext(MapUpdateMode mode, MapCatalogRow selectedRow) {
        this.mode = mode;
        this.currentRow = selectedRow;

        fillLeft(selectedRow);
        fillRightDefaults(mode, selectedRow);
        applyPermissions(mode);
    }

    private void fillLeft(MapCatalogRow row) {
        if (row == null) {
            lblCurCity.setText("-");
            lblCurMap.setText("-");
            lblCurPrice.setText("-");
            taCurDesc.setText("-");
            return;
        }
        lblCurCity.setText(row.getCity());
        lblCurMap.setText(row.getMap());
        lblCurPrice.setText(String.valueOf(row.getPrice()));
        taCurDesc.setText(row.getDescription());
    }

    private void fillRightDefaults(MapUpdateMode mode, MapCatalogRow row) {
        if (mode == MapUpdateMode.ADD || row == null) {
            tfNewCity.clear();
            tfNewMap.clear();
            tfNewPrice.clear();
            taNewDesc.clear();
            return;
        }

        // copy current to right by default
        tfNewCity.setText(row.getCity());
        tfNewMap.setText(row.getMap());
        tfNewPrice.setText(String.valueOf(row.getPrice()));
        taNewDesc.setText(row.getDescription());
    }

    private void applyPermissions(MapUpdateMode mode) {
        User u = client.getCurrentUser();
        UserRole role = (u == null) ? null : u.getRole();


        setRightEditable(false, false, false, false);

        btnAddUpdate.setDisable(true);
        btnAddUpdate.setVisible(false);

        btnApprove.setDisable(true);
        btnApprove.setVisible(false);


        if (role == null) return;

        switch (mode) {
            case ADD -> {
                // content_worker can add full map
                if (role == UserRole.CONTENT_WORKER) {
                    setRightEditable(true, true, true, true);
                    btnAddUpdate.setVisible(true);
                    btnAddUpdate.setText("Add");
                    btnAddUpdate.setDisable(false);
                }
            }

            case UPDATE -> {
                // content_worker can update everything
                if (role == UserRole.CONTENT_WORKER) {
                    setRightEditable(true, true, true, true);
                    btnAddUpdate.setVisible(true);
                    btnAddUpdate.setText("Update");
                    btnAddUpdate.setDisable(false);
                }
            }

            case PRICE_UPDATE_REQUEST -> {
                // content_manager can change ONLY price
                if (role == UserRole.CONTENT_MANAGER) {
                    setRightEditable(false, false, true, false);
                    btnAddUpdate.setVisible(true);
                    btnAddUpdate.setText("Request Price Update");
                    btnAddUpdate.setDisable(false);
                }
            }

            case APPROVAL_REVIEW -> {
                // company_manager approves/denies (for now only approve button exists)
                if (role == UserRole.COMPANY_MANAGER) {
                    // keep fields locked
                    btnApprove.setVisible(true);
                    btnApprove.setDisable(false);
                }
            }
        }
    }

    private void setRightEditable(boolean city, boolean map, boolean price, boolean desc) {
        tfNewCity.setDisable(!city);
        tfNewMap.setDisable(!map);
        tfNewPrice.setDisable(!price);
        taNewDesc.setDisable(!desc);
    }

    @FXML
    private void onAddUpdate() {
        // later we’ll send Message to server based on mode
        // For now: just close, so UI flow works.
        closeWindow();
    }

    @FXML
    private void onApprove() {
        // later: send approve/deny messages
        closeWindow();
    }

    @FXML
    private void onClose() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
}
