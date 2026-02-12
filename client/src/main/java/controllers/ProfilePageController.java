package controllers;

import java.util.Comparator;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import client.GCMClient;
import common.enums.ActionType;
import common.enums.EmployeeRole;
import common.messaging.Message;
import common.support.ListClientSupportRepliesRequest;
import common.support.ListClientSupportRepliesResponse;
import common.support.MarkSupportReplyReadRequest;
import common.support.SupportTicketRowDTO;
import common.purchase.PaymentDetails;
import common.user.Client;
import common.user.Employee;
import common.user.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import util.TableRowHighlighter;

public class ProfilePageController {

    @FXML private Label lblWelcome;
    @FXML private Label lblName;
    @FXML private Label lblEmail;
    @FXML private Label lblPaymentLabel;
    @FXML private Label lblPayment;
    @FXML private Label lblRoleLabel;
    @FXML private Label lblRoleValue;
    @FXML private Label lblInbox;
    @FXML private Button btnOpenInbox;
    @FXML private Button btnChangePayment;
    @FXML private Button btnPurchaseHistory;

    // Inbox table
    @FXML private TableView<SupportTicketRowDTO> tblInbox;
    @FXML private TableColumn<SupportTicketRowDTO, String> colFrom;
    @FXML private TableColumn<SupportTicketRowDTO, String> colSubject;
    @FXML private TableColumn<SupportTicketRowDTO, String> colDate;
    @FXML private TableColumn<SupportTicketRowDTO, String> colPreview;

    private final ObservableList<SupportTicketRowDTO> inboxRows = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        if (tblInbox != null) {
            tblInbox.setItems(inboxRows);

            colFrom.setCellValueFactory(d ->
                    new SimpleStringProperty(
                            d.getValue().getAgentName() == null ? "Support" : d.getValue().getAgentName()
                    )
            );

            colSubject.setCellValueFactory(d ->
                    new SimpleStringProperty(
                            "Ticket #" + d.getValue().getTicketId() + " - " + d.getValue().getTopic()
                    )
            );

            colDate.setCellValueFactory(d ->
                    new SimpleStringProperty(
                            d.getValue().getRepliedAt() == null ? "" : dtf.format(d.getValue().getRepliedAt())
                    )
            );

            colPreview.setCellValueFactory(d -> {
                String reply = d.getValue().getAgentReply();
                if (reply == null) reply = "";
                reply = reply.replace("\n", " ").trim();
                if (reply.length() > 45) reply = reply.substring(0, 45) + "...";
                return new SimpleStringProperty(reply);
            });

            TableRowHighlighter.apply(tblInbox, row -> !row.isReadByClient());

            tblInbox.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    SupportTicketRowDTO selected = tblInbox.getSelectionModel().getSelectedItem();
                    if (selected != null) openMessage(selected);
                }
            });
        }
    }

    public void setUser(User user) {
        if (user == null) return;

        String first = user.getFirstName();
        String last  = user.getLastName();
        String fullName = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
        if (fullName.isBlank()) fullName = user.getUsername();

        lblWelcome.setText("Welcome " + (first != null && !first.isBlank() ? first : user.getUsername()));
        lblName.setText(fullName);
        lblEmail.setText(user.getEmail() != null ? user.getEmail() : user.getUsername());

        if (user instanceof Client client) {
            // Payment details
            PaymentDetails pd = client.getPaymentDetails();
            if (pd != null && pd.getCreditCardNumber() != null && !pd.getCreditCardNumber().isBlank()) {
                String card = pd.getCreditCardNumber();
                String masked = "**** **** **** " + card.substring(Math.max(0, card.length() - 4));
                String expiry = (pd.getExpiryMonth() != null ? pd.getExpiryMonth() : "??")
                        + "/" + (pd.getExpiryYear() != null ? pd.getExpiryYear() : "??");
                lblPayment.setText(masked + " (exp: " + expiry + ")");
            } else {
                lblPayment.setText("Not set");
            }

            // Show client-specific elements
            setNodeVisible(btnChangePayment, true);
            setNodeVisible(btnPurchaseHistory, true);
            setNodeVisible(lblPaymentLabel, true);
            setNodeVisible(lblPayment, true);

            // Hide role (not relevant for clients)
            setNodeVisible(lblRoleLabel, false);
            setNodeVisible(lblRoleValue, false);

        } else if (user instanceof Employee employee) {
            // Show role
            setNodeVisible(lblRoleLabel, true);
            setNodeVisible(lblRoleValue, true);
            lblRoleValue.setText(formatRole(employee.getRole()));

            // Hide client-specific elements
            setNodeVisible(lblPaymentLabel, false);
            setNodeVisible(lblPayment, false);
            setNodeVisible(btnChangePayment, false);
            setNodeVisible(btnPurchaseHistory, false);
        }

        refreshInbox(user);
    }

    private String formatRole(EmployeeRole role) {
        if (role == null) return "Unknown";
        return switch (role) {
            case CONTENT_WORKER -> "Content Worker";
            case CONTENT_MANAGER -> "Content Manager";
            case COMPANY_MANAGER -> "Company Manager";
            case SUPPORT_AGENT -> "Support Agent";
        };
    }

    private void setNodeVisible(Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    // ==================== PURCHASE HISTORY ====================

    @FXML
    private void onPurchaseHistory() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/GUI/PurchaseHistoryDialog.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Purchase History");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== PAYMENT ====================

    @FXML
    private void onChangePayment() {
        openPaymentUpdateDialog();
    }

    private void openPaymentUpdateDialog() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/GUI/PaymentUpdateDialog.fxml"));
            javafx.scene.Parent root = loader.load();

            PaymentUpdateDialogController controller = loader.getController();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Update Payment Details");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Refresh payment display if saved
            if (controller.isSaved()) {
                User user = GCMClient.getInstance().getCurrentUser();
                if (user instanceof Client client) {
                    PaymentDetails pd = client.getPaymentDetails();
                    if (pd != null && pd.getCreditCardNumber() != null && !pd.getCreditCardNumber().isBlank()) {
                        String card = pd.getCreditCardNumber();
                        String masked = "**** **** **** " + card.substring(Math.max(0, card.length() - 4));
                        String expiry = (pd.getExpiryMonth() != null ? pd.getExpiryMonth() : "??")
                                + "/" + (pd.getExpiryYear() != null ? pd.getExpiryYear() : "??");
                        lblPayment.setText(masked + " (exp: " + expiry + ")");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== INBOX ====================

    @FXML
    private void onOpenInbox() {
        SupportTicketRowDTO selected = (tblInbox == null ? null : tblInbox.getSelectionModel().getSelectedItem());
        if (selected == null) return;
        openMessage(selected);
    }

    private void refreshInbox(User user) {
        runAsync(
                () -> {
                    Message req = new Message(
                            ActionType.LIST_CLIENT_SUPPORT_REPLIES,
                            new ListClientSupportRepliesRequest(user.getId())
                    );
                    return GCMClient.getInstance().sendMessage(req);
                },
                (Message resp) -> {
                    inboxRows.clear();

                    if (resp != null && resp.getMessage() instanceof ListClientSupportRepliesResponse r) {
                        inboxRows.addAll(r.getRows());

                        inboxRows.sort(Comparator
                                .comparing(SupportTicketRowDTO::isReadByClient)
                                .thenComparing(SupportTicketRowDTO::getRepliedAt,
                                        Comparator.nullsLast(Comparator.reverseOrder()))
                        );

                        long unread = r.getRows().stream().filter(x -> !x.isReadByClient()).count();
                        lblInbox.setText("You have " + unread + " new messages");
                    } else {
                        lblInbox.setText("You have 0 new messages");
                    }
                },
                (Throwable err) -> {},
                null
        );
    }

    private void openMessage(SupportTicketRowDTO row) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ticket #" + row.getTicketId());

        String from = (row.getAgentName() == null || row.getAgentName().isBlank())
                ? "Support"
                : row.getAgentName();

        String subject = "Ticket #" + row.getTicketId() + " - " + row.getTopic();

        Label lbl1 = new Label("From: " + from);
        Label lbl2 = new Label("Topic: " + subject);

        String dateStr = (row.getRepliedAt() == null ? "" : dtf.format(row.getRepliedAt()));
        Label lbl3 = new Label("Date: " + dateStr);

        TextArea clientBody = new TextArea();
        clientBody.setEditable(false);
        clientBody.setWrapText(true);
        String clientText = row.getClientText();
        if (clientText == null || clientText.isBlank()) clientText = "(No client message)";
        clientBody.setText(clientText);
        clientBody.setPrefRowCount(8);

        TextArea agentBody = new TextArea();
        agentBody.setEditable(false);
        agentBody.setWrapText(true);
        String replyText = row.getAgentReply();
        if (replyText == null || replyText.isBlank()) replyText = "(No reply yet)";
        agentBody.setText(replyText);
        agentBody.setPrefRowCount(8);

        VBox box = new VBox(10,
                lbl1, lbl2, lbl3,
                new Label("Your message:"), clientBody,
                new Label("Support reply:"), agentBody
        );

        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();

        if (!row.isReadByClient()) {
            markRead(row.getTicketId());
        }
    }

    private void markRead(int ticketId) {
        runAsync(
                () -> {
                    User u = GCMClient.getInstance().getCurrentUser();
                    if (u == null) return null;

                    Message req = new Message(
                            ActionType.MARK_SUPPORT_REPLY_READ,
                            new MarkSupportReplyReadRequest(u.getId(), ticketId)
                    );
                    return GCMClient.getInstance().sendMessage(req);
                },
                (Message resp) -> {
                    User u = GCMClient.getInstance().getCurrentUser();
                    if (u != null) refreshInbox(u);
                },
                (Throwable err) -> {},
                null
        );
    }

    // ==================== UTILITY ====================

    private <T> void runAsync(java.util.concurrent.Callable<T> work,
                              java.util.function.Consumer<T> onSuccess,
                              java.util.function.Consumer<Throwable> onError,
                              Runnable onFinally) {

        javafx.concurrent.Task<T> task = new javafx.concurrent.Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };

        task.setOnSucceeded(e -> {
            try { onSuccess.accept(task.getValue()); }
            finally { if (onFinally != null) onFinally.run(); }
        });

        task.setOnFailed(e -> {
            try { onError.accept(task.getException()); }
            finally { if (onFinally != null) onFinally.run(); }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
