package controllers;

import client.GCMClient;
import common.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.ArrayList;

/**
 * Map update / price update / approval review popup.
 *
 * LEFT side: current (read-only)
 */
public class MapUpdatePageController {

    // LEFT (current)
    @FXML private Label lblCurCity;
    @FXML private Label lblCurMap;
    @FXML private Label lblCurPrice;
    @FXML private TextArea taCurDesc;

    // RIGHT (new values)
    @FXML private TextField tfNewCity;
    @FXML private TextField tfNewMap;
    @FXML private TextField tfNewPrice;
    @FXML private TextArea taNewDesc;

    // Buttons
    @FXML private Button btnAddUpdate;
    @FXML private Button btnApprove;
    @FXML private Button btnDeny;
    @FXML private Button btnClose;

    private final GCMClient client = GCMClient.getInstance();

    private MapStatus mode = MapStatus.UPDATE;

    // For ADD/UPDATE/PRICE_UPDATE_REQUEST we keep the selected catalog row
    private MapCatalogRow currentRow;

    // For APPROVAL_REVIEW we keep the pending request
    private PendingPriceUpdate pending;

    @FXML
    public void initialize() {
        // left is always read-only
        if (taCurDesc != null) {
            taCurDesc.setDisable(true);
            taCurDesc.setFocusTraversable(false);
        }
        // start with everything locked down, then enable based on mode+role
        setRightEditable(false, false, false, false);
        show(btnApprove, false);
        show(btnDeny, false);
        show(btnAddUpdate, true);
    }

    /** Existing API used from CatalogPageController */
    public void setContext(MapStatus mode, MapCatalogRow selectedRow) {
        this.pending = null;
        this.mode = mode;
        this.currentRow = selectedRow;

        fillLeftFromCatalogRow(selectedRow);
        fillRightDefaultsFromCatalogRow(mode, selectedRow);
        applyPermissions();
    }

    /** New API used from ApprovalPendingPageController */
    public void setApprovalContext(PendingPriceUpdate pending) {
        this.currentRow = null;
        this.pending = pending;
        this.mode = MapStatus.APPROVAL_REVIEW;

        fillLeftFromPending(pending);
        fillRightDefaultsFromPending(pending);
        applyPermissions();
    }

    // ---------------- UI helpers ----------------

    private void fillLeftFromCatalogRow(MapCatalogRow row) {
        if (row == null) {
            lblCurCity.setText("-");
            lblCurMap.setText("-");
            lblCurPrice.setText("-");
            taCurDesc.setText("-");
            return;
        }
        lblCurCity.setText(row.getCity());
        lblCurMap.setText(row.getMapName() + " (" + row.getVersion() + ")");
        lblCurPrice.setText(String.valueOf(row.getPrice()));
        taCurDesc.setText(row.getDescription());
    }

    private void fillRightDefaultsFromCatalogRow(MapStatus mode, MapCatalogRow row) {
        if (mode == MapStatus.ADD || row == null) {
            tfNewCity.clear();
            tfNewMap.clear();
            tfNewPrice.clear();
            taNewDesc.clear();
            return;
        }

        // copy current to right by default
        tfNewCity.setText(row.getCity());
        tfNewMap.setText(row.getMapName());
        tfNewPrice.setText(String.valueOf(row.getPrice()));
        taNewDesc.setText(row.getDescription());
    }

    private void fillLeftFromPending(PendingPriceUpdate p) {
        if (p == null) {
            lblCurCity.setText("-");
            lblCurMap.setText("-");
            lblCurPrice.setText("-");
            taCurDesc.setText("-");
            return;
        }
        lblCurCity.setText(p.getCity());
        lblCurMap.setText(p.getMapName() + " (" + p.getVersion() + ")");
        lblCurPrice.setText(String.valueOf(p.getOldPrice()));
        taCurDesc.setText("(approval review)");
    }

    private void fillRightDefaultsFromPending(PendingPriceUpdate p) {
        tfNewCity.setText(p.getCity());
        tfNewMap.setText(p.getMapName());
        tfNewPrice.setText(String.valueOf(p.getNewPrice()));
        taNewDesc.clear();
    }

    private void applyPermissions() {
        User u = client.getCurrentUser();
        UserRole role = (u == null) ? null : u.getRole();

        // safe defaults
        setRightEditable(false, false, false, false);

        show(btnAddUpdate, false);
        if (btnAddUpdate != null) btnAddUpdate.setDisable(true);

        show(btnApprove, false);
        if (btnApprove != null) btnApprove.setDisable(true);

        show(btnDeny, false);
        if (btnDeny != null) btnDeny.setDisable(true);

        if (role == null) return;

        switch (mode) {

            case ADD -> {
                if (role == UserRole.CONTENT_WORKER) {
                    setRightEditable(true, true, true, true);
                    show(btnAddUpdate, true);
                    btnAddUpdate.setText("Add");
                    btnAddUpdate.setDisable(false);
                }
            }

            case UPDATE -> {
                if (role == UserRole.CONTENT_WORKER) {
                    setRightEditable(true, true, true, true);
                    show(btnAddUpdate, true);
                    btnAddUpdate.setText("Update");
                    btnAddUpdate.setDisable(false);
                }
            }

            case PRICE_UPDATE_REQUEST -> {
                if (role == UserRole.CONTENT_MANAGER) {
                    setRightEditable(false, false, true, false);
                    show(btnAddUpdate, true);
                    btnAddUpdate.setText("Send Price Request");
                    btnAddUpdate.setDisable(false);
                }
            }

            case APPROVAL_REVIEW -> {
                if (role == UserRole.COMPANY_MANAGER ) {
                    setRightEditable(false, false, false, false);

                    show(btnApprove, true);
                    btnApprove.setDisable(false);

                    show(btnDeny, true);
                    btnDeny.setDisable(false);
                }
            }
        }
    }

    private void setRightEditable(boolean city, boolean map, boolean price, boolean desc) {
        if (tfNewCity != null) tfNewCity.setDisable(!city);
        if (tfNewMap != null) tfNewMap.setDisable(!map);
        if (tfNewPrice != null) tfNewPrice.setDisable(!price);
        if (taNewDesc != null) taNewDesc.setDisable(!desc);
    }

    private void show(Button b, boolean v) {
        if (b == null) return;
        b.setVisible(v);
        b.setManaged(v);
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
        });
    }

    private void showInfo(String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
        });
    }

    private void closeWindow() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }

    // ---------------- Actions ----------------

    @FXML
    private void onAddUpdate() {
        // Only implemented right now for PRICE_UPDATE_REQUEST
        if (mode != MapStatus.PRICE_UPDATE_REQUEST) {
            closeWindow();
            return;
        }

        if (currentRow == null) {
            showError("No map selected.");
            return;
        }

        double newPrice;
        try {
            newPrice = Double.parseDouble(tfNewPrice.getText().trim());
        } catch (Exception e) {
            showError("Price must be a number.");
            return;
        }
        if (newPrice < 0) {
            showError("Price must be >= 0.");
            return;
        }

        Integer requesterId = null;
        User u = client.getCurrentUser();
        if (u != null) requesterId = u.getId();

        ArrayList<Object> payload = new ArrayList<>();
        payload.add(currentRow.getCity());
        payload.add(currentRow.getMapName());
        payload.add(currentRow.getVersion());
        payload.add(newPrice);
        payload.add(requesterId);

        new Thread(() -> {
            try {
                Object res = client.sendRequest(new Message(actionType.UPDATE_PRICE_REQUEST, payload));
                boolean ok = (res instanceof Message msg
                        && msg.getAction() == actionType.UPDATE_PRICE_SUCCESS
                        && msg.getMessage() instanceof Boolean b
                        && b);

                if (ok) {
                    showInfo("Price update request sent for approval.");
                    Platform.runLater(this::closeWindow);
                } else {
                    showError("Failed to send price request.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error sending request: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void onApprove() {
        if (pending == null) { closeWindow(); return; }

        Integer reviewerId = null;
        User u = client.getCurrentUser();
        if (u != null) reviewerId = u.getId();

        ArrayList<Object> payload = new ArrayList<>();
        payload.add(pending.getId());
        payload.add(reviewerId);

        new Thread(() -> {
            try {
                Object res = client.sendRequest(new Message(actionType.APPROVE_PENDING_PRICE_REQUEST, payload));
                boolean ok = (res instanceof Message msg
                        && msg.getAction() == actionType.APPROVE_PRICE_SUCCESS
                        && msg.getMessage() instanceof Boolean b
                        && b);

                if (ok) {
                    showInfo("Approved. Map price updated.");
                    Platform.runLater(this::closeWindow);
                } else {
                    showError("Approve failed.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error approving: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void onDeny() {
        if (pending == null) { closeWindow(); return; }

        Integer reviewerId = null;
        User u = client.getCurrentUser();
        if (u != null) reviewerId = u.getId();

        ArrayList<Object> payload = new ArrayList<>();
        payload.add(pending.getId());
        payload.add(reviewerId);

        new Thread(() -> {
            try {
                Object res = client.sendRequest(new Message(actionType.DENY_PENDING_PRICE_REQUEST, payload));
                boolean ok = (res instanceof Message msg
                        && msg.getAction() == actionType.DENY_PRICE_SUCCESS
                        && msg.getMessage() instanceof Boolean b
                        && b);

                if (ok) {
                    showInfo("Denied.");
                    Platform.runLater(this::closeWindow);
                } else {
                    showError("Deny failed.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Error denying: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void onClose() {
        closeWindow();
    }
}
