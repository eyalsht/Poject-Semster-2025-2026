package controllers;

import client.GCMClient;
import common.dto.PendingApprovalsResponse;
import common.dto.PendingContentApprovalsResponse;
import common.enums.ActionType;
import common.enums.EmployeeRole;
import common.messaging.Message;
import common.user.Employee;
import common.user.User;
import common.workflow.PendingContentRequest;
import common.workflow.PendingPriceUpdate;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controller for the pending approvals page.
 * Shows different approvals based on user role:
 * - Company Manager: Price approvals only
 * - Content Manager: Content approvals only
 */
public class ApprovalPendingPageController {

    @FXML private TableView<Object> tblPending;  // Generic to hold either type
    @FXML private TableColumn<Object, String> colTarget;
    @FXML private TableColumn<Object, String> colInfo;
    @FXML private TableColumn<Object, String> colType;

    @FXML private Button btnApprove;
    @FXML private Button btnDeny;
    @FXML private Label lblStatus;
    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;

    private final GCMClient client = GCMClient.getInstance();
    private CatalogPageController catalogController;
    
    private EmployeeRole currentUserRole;
    private boolean isPriceApprovalMode;

    public void setCatalogController(CatalogPageController controller) {
        this.catalogController = controller;
    }

    @FXML
    public void initialize() {
        determineApprovalMode();
        setupTableColumns();
        setupSelectionListener();
        loadPendingApprovals();
    }

    private void determineApprovalMode() {
        User user = client.getCurrentUser();
        if (user instanceof Employee employee) {
            currentUserRole = employee.getRole();
            isPriceApprovalMode = (currentUserRole == EmployeeRole.COMPANY_MANAGER);
        }
        
        // Update title and subtitle based on mode
        if (lblTitle != null) {
            lblTitle.setText(isPriceApprovalMode ?
                "Pending Price Approvals" : "Pending Content Approvals");
        }
        if (lblSubtitle != null) {
            lblSubtitle.setText(isPriceApprovalMode ?
                "As Company Manager, review price change requests" :
                "As Content Manager, review content change requests");
        }
    }

    private void setupTableColumns() {
        colTarget.setCellValueFactory(new PropertyValueFactory<>("target"));
        colInfo.setCellValueFactory(new PropertyValueFactory<>("info"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
    }

    private void setupSelectionListener() {
        tblPending.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            btnApprove.setDisable(!hasSelection);
            btnDeny.setDisable(!hasSelection);
        });
        btnApprove.setDisable(true);
        btnDeny.setDisable(true);
    }

    private void loadPendingApprovals() {
        setStatus("Loading...");

        new Thread(() -> {
            try {
                ActionType requestType = isPriceApprovalMode 
                    ? ActionType.GET_PENDING_PRICE_APPROVALS_REQUEST
                    : ActionType.GET_PENDING_CONTENT_APPROVALS_REQUEST;
                    
                // Fallback to legacy action type if new ones aren't implemented
                if (isPriceApprovalMode) {
                    requestType = ActionType.GET_PENDING_APPROVALS_REQUEST;
                }

                Message request = new Message(requestType, null);
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

        if (isPriceApprovalMode) {
            // Handle price approvals (Company Manager)
            if (response.getMessage() instanceof PendingApprovalsResponse approvals) {
                List<PendingPriceUpdate> pending = approvals.getPendingUpdates();
                tblPending.setItems(FXCollections.observableArrayList(pending));
                setStatus("Found " + approvals.getTotalCount() + " pending price approval(s).");
            }
        } else {
            // Handle content approvals (Content Manager)
            if (response.getMessage() instanceof PendingContentApprovalsResponse approvals) {
                List<PendingContentRequest> pending = approvals.getPendingRequests();
                tblPending.setItems(FXCollections.observableArrayList(pending));
                setStatus("Found " + approvals.getTotalCount() + " pending content approval(s).");
            } else if (response.getMessage() instanceof PendingApprovalsResponse approvals) {
                // Fallback for legacy response
                List<PendingPriceUpdate> pending = approvals.getPendingUpdates();
                tblPending.setItems(FXCollections.observableArrayList(pending));
                setStatus("Found " + approvals.getTotalCount() + " pending approval(s).");
            }
        }
    }

    @FXML
    private void onApprove() {
        Object selected = tblPending.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (selected instanceof PendingPriceUpdate priceUpdate) {
            processPriceApproval(priceUpdate.getId(), true);
        } else if (selected instanceof PendingContentRequest contentRequest) {
            processContentApproval(contentRequest.getId(), true);
        }
    }

    @FXML
    private void onDeny() {
        Object selected = tblPending.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (selected instanceof PendingPriceUpdate priceUpdate) {
            processPriceApproval(priceUpdate.getId(), false);
        } else if (selected instanceof PendingContentRequest contentRequest) {
            processContentApproval(contentRequest.getId(), false);
        }
    }

    private void processPriceApproval(int pendingId, boolean approve) {
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

    private void processContentApproval(int pendingId, boolean approve) {
        setStatus(approve ? "Approving..." : "Denying...");

        new Thread(() -> {
            try {
                ActionType action = approve ?
                    ActionType.APPROVE_CONTENT_REQUEST :
                    ActionType.DENY_CONTENT_REQUEST;

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

        boolean success = false;
        if (response.getMessage() instanceof Boolean) {
            success = (Boolean) response.getMessage();
        }
        
        if (success)
        {
            setStatus(wasApprove ? "Approved successfully!" : "Denied successfully!");
            loadPendingApprovals();
            
            if (catalogController != null) {
                catalogController.refreshPendingApprovalsCount();
            }
        }
        else
        {
            setStatus("Operation failed. Please try again.");
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
            if (message.toLowerCase().contains("success")) {
                lblStatus.setTextFill(Color.web("#4caf50"));
            } else if (message.toLowerCase().contains("error") || message.toLowerCase().contains("failed")) {
                lblStatus.setTextFill(Color.web("#ff6b6b"));
            } else {
                lblStatus.setTextFill(Color.WHITE);
            }
        }
    }
}
