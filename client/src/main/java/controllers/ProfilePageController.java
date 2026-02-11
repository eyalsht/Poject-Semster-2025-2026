package controllers;

import client.GCMClient;
import common.enums.ActionType;
import common.messaging.Message;
import common.support.ListClientSupportRepliesRequest;
import common.support.ListClientSupportRepliesResponse;
import common.support.MarkSupportReplyReadRequest;
import common.support.SupportTicketRowDTO;
import common.dto.SubscriptionStatusDTO;
import common.purchase.PaymentDetails;
import common.purchase.PurchasedMapSnapshot;
import common.user.Client;
import common.user.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import util.TableRowHighlighter;

public class ProfilePageController {

    @FXML private Label lblWelcome;
    @FXML private Label lblName;
    @FXML private Label lblEmail;
    @FXML private Label lblPayment;
    @FXML private Label lblInbox;
    @FXML private Label lblHistory;
    @FXML private Button btnOpenInbox;
    @FXML private Button btnChangePayment;

    // NEW inbox table
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
                    new javafx.beans.property.SimpleStringProperty(
                            d.getValue().getAgentName() == null ? "Support" : d.getValue().getAgentName()
                    )
            );

            colSubject.setCellValueFactory(d ->
                    new javafx.beans.property.SimpleStringProperty(
                            "Ticket #" + d.getValue().getTicketId() + " - " + d.getValue().getTopic()
                    )
            );

            colDate.setCellValueFactory(d ->
                    new javafx.beans.property.SimpleStringProperty(
                            d.getValue().getRepliedAt() == null ? "" : dtf.format(d.getValue().getRepliedAt())
                    )
            );

            colPreview.setCellValueFactory(d -> {
                String reply = d.getValue().getAgentReply();
                if (reply == null) reply = "";
                reply = reply.replace("\n", " ").trim();
                if (reply.length() > 45) reply = reply.substring(0, 45) + "...";
                return new javafx.beans.property.SimpleStringProperty(reply);
            });

            TableRowHighlighter.apply(tblInbox, row -> !row.isReadByClient());

            // double click to open
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

        // Payment details
        if (user instanceof Client client) {
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

            if (btnChangePayment != null) {
                btnChangePayment.setVisible(true);
                btnChangePayment.setManaged(true);
            }

            loadPurchaseSummary(user);
        } else {
            lblPayment.setText("N/A");
            lblHistory.setText("N/A");
        }

        refreshInbox(user);
    }

    private void loadPurchaseSummary(User user) {
        lblHistory.setText("Loading...");

        // Fetch subscriptions count
        runAsync(
            () -> {
                Message subReq = new Message(ActionType.GET_USER_SUBSCRIPTIONS_REQUEST, user.getId());
                return GCMClient.getInstance().sendMessage(subReq);
            },
            (Message subResp) -> {
                int subCount = 0;
                if (subResp != null && subResp.getMessage() instanceof java.util.ArrayList<?> list) {
                    subCount = list.size();
                }
                final int subs = subCount;

                // Fetch purchased maps count
                runAsync(
                    () -> {
                        Message mapReq = new Message(ActionType.GET_USER_PURCHASED_MAPS_REQUEST, user.getId());
                        return GCMClient.getInstance().sendMessage(mapReq);
                    },
                    (Message mapResp) -> {
                        int mapCount = 0;
                        if (mapResp != null && mapResp.getMessage() instanceof java.util.ArrayList<?> list) {
                            mapCount = list.size();
                        }
                        lblHistory.setText(subs + " active subscriptions, " + mapCount + " purchased maps");
                    },
                    (Throwable err) -> lblHistory.setText("Could not load"),
                    null
                );
            },
            (Throwable err) -> lblHistory.setText("Could not load"),
            null
        );
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

                        // Sort so unread is on top, then newest reply first
                        inboxRows.sort(Comparator
                                .comparing(SupportTicketRowDTO::isReadByClient) // false first
                                .thenComparing(SupportTicketRowDTO::getRepliedAt,
                                        Comparator.nullsLast(Comparator.reverseOrder()))
                        );

                        long unread = r.getRows().stream().filter(x -> !x.isReadByClient()).count();
                        lblInbox.setText("You have " + unread + " new messages");
                    } else {
                        lblInbox.setText("You have 0 new messages");
                    }
                },
                (Throwable err) -> {
                    // optional: new Alert(Alert.AlertType.ERROR, err.getMessage()).showAndWait();
                },
                null
        );
    }

    @FXML
    private void onChangePayment() {
        openPaymentUpdateDialog();
    }

    private void openPaymentUpdateDialog() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/GUI/PaymentUpdateDialog.fxml"));
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

    @FXML
    private void onOpenInbox() {
        SupportTicketRowDTO selected = (tblInbox == null ? null : tblInbox.getSelectionModel().getSelectedItem());
        if (selected == null) return;
        openMessage(selected);
    }

    private void openMessage(SupportTicketRowDTO row) {
        // show dialog first (even if already read)
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

        // --- Client message (FULL) ---
        TextArea clientBody = new TextArea();
        clientBody.setEditable(false);
        clientBody.setWrapText(true);

        String clientText = row.getClientText();
        if (clientText == null || clientText.isBlank()) {
            clientText = "(No client message)";
        }
        clientBody.setText(clientText);
        clientBody.setPrefRowCount(8);

        // --- Agent reply ---
        TextArea agentBody = new TextArea();
        agentBody.setEditable(false);
        agentBody.setWrapText(true);

        String replyText = row.getAgentReply();
        if (replyText == null || replyText.isBlank()) {
            replyText = "(No reply yet)";
        }
        agentBody.setText(replyText);
        agentBody.setPrefRowCount(8);

        VBox box = new VBox(10,
                lbl1,
                lbl2,
                lbl3,
                new Label("Your message:"),
                clientBody,
                new Label("Support reply:"),
                agentBody
        );

        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();

        // If it was unread -> mark as read and refresh (it will move down + become grey)
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
