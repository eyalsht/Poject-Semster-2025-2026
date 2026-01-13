package controllers;

import client.GCMClient;
import common.dto.PendingApprovalsResponse;
import common.enums.ActionType;
import common.messaging.Message;
import common.workflow.PendingPriceUpdate;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controller for the pending approvals page.
 */
public class ApprovalPendingPageController {

    @FXML private TableView<PendingPriceUpdate> tblPending;
    @FXML private TableColumn<PendingPriceUpdate, String> colTarget;
    @FXML private TableColumn<PendingPriceUpdate, String> colInfo;
    @FXML private TableColumn<PendingPriceUpdate, String> colType;

    @FXML private Button btnApprove;
    @FXML private Button btnDeny;
    @FXML private Label lblStatus;

    private final GCMClient client = GCMClient.getInstance();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupSelectionListener();
        loadPendingApprovals();
    }

    private void setupTableColumns() {
        colTarget.setCellValueFactory(new PropertyValueFactory<>("target"));
        colInfo.setCellValueFactory(new PropertyValueFactory<>("info"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
    }

    private void setupSelectionListener() {
        // Enable/disable buttons based on selection
        tblPending.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            btnApprove.setDisable(!hasSelection);
            btnDeny.setDisable(!hasSelection);
        });

        // Initially disable buttons
        btnApprove.setDisable(true);
        btnDeny.setDisable(true);
    }

    /**
     * Load pending approvals from server.
     */
    private void loadPendingApprovals() {
        setStatus("Loading...");

        new Thread(() -> {
            try {
                Message request = new Message(ActionType.GET_PENDING_APPROVALS_REQUEST, null);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> handlePendingResponse(response));

            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void handlePendingResponse(Message response) {
        if (response == null || response.getAction() == ActionType.ERROR) {
            setStatus("Failed to load approvals.");
            return;
        }

        if (response.getAction() == ActionType.GET_PENDING_APPROVALS_RESPONSE) {
            PendingApprovalsResponse approvals = (PendingApprovalsResponse) response.getMessage();
            List<PendingPriceUpdate> pending = approvals.getPendingUpdates();
            
            tblPending.setItems(FXCollections.observableArrayList(pending));
            setStatus("Found " + approvals.getTotalCount() + " pending approval(s).");
        }
    }

    @FXML
    private void onApprove() {
        PendingPriceUpdate selected = tblPending.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        processApproval(selected.getId(), true);
    }

    @FXML
    private void onDeny() {
        PendingPriceUpdate selected = tblPending.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        processApproval(selected.getId(), false);
    }

    /**
     * Send approve/deny request to server.
     */
    private void processApproval(int pendingId, boolean approve) {
        setStatus(approve ? "Approving..." : "Denying...");

        new Thread(() -> {
            try {
                ActionType action = approve ?
                    ActionType.APPROVE_PENDING_REQUEST :
                    ActionType.DENY_PENDING_REQUEST;

                Message request = new Message(action, pendingId);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> handleApprovalResponse(response, approve));

            } catch (Exception e) {
                Platform.runLater(() -> setStatus("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void handleApprovalResponse(Message response, boolean wasApprove) {
        if (response == null) {
            setStatus("No response from server.");
            return;
        }

        ActionType expectedResponse = wasApprove ?
            ActionType.APPROVE_PENDING_RESPONSE :
            ActionType.DENY_PENDING_RESPONSE;

        if (response.getAction() == expectedResponse) {
            boolean success = (Boolean) response.getMessage();
            if (success) {
                setStatus(wasApprove ? "Approved successfully!" : "Denied successfully!");
                loadPendingApprovals();  // Refresh the list
            } else {
                setStatus("Operation failed. Please try again.");
            }
        } else {
            setStatus("Unexpected response from server.");
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) tblPending.getScene().getWindow();
        stage.close();
    }

    private void setStatus(String message) {
        if (lblStatus != null) {
            lblStatus.setText(message);
        }
    }
}
