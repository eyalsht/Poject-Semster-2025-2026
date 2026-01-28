package controllers;

import client.GCMClient;
import common.enums.ActionType;
import common.enums.EmployeeRole;
import common.messaging.Message;
import common.support.*;
import common.user.Employee;
import common.user.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import common.enums.SupportTicketStatus;

import java.time.format.DateTimeFormatter;

public class SupportTasksPageController {

    @FXML private TableView<SupportTicketRowDTO> tblTickets;
    @FXML private TableColumn<SupportTicketRowDTO, Number> colId;
    @FXML private TableColumn<SupportTicketRowDTO, String> colUser;
    @FXML private TableColumn<SupportTicketRowDTO, String> colTopic;
    @FXML private TableColumn<SupportTicketRowDTO, String> colStatus;
    @FXML private TableColumn<SupportTicketRowDTO, String> colCreated;
    @FXML private TableColumn<SupportTicketRowDTO, String> colPreview;
    @FXML private Button btnOpen;

    private final ObservableList<SupportTicketRowDTO> rows = FXCollections.observableArrayList();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        tblTickets.setItems(rows);

        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getTicketId()));
        colUser.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getClientUsername()));
        colTopic.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTopic()));
        colStatus.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getStatus())));
        colCreated.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getCreatedAt() == null ? "" : dtf.format(data.getValue().getCreatedAt())
        ));
        colPreview.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPreview()));

        // GREEN for DONE
        tblTickets.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(SupportTicketRowDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.getStatus() == SupportTicketStatus.DONE) {
                    setStyle("-fx-background-color: #d6ffd6;");
                } else {
                    setStyle("");
                }
            }
        });

        refresh();
    }

    private void refresh() {
        runAsync(
                () -> {
                    GCMClient c = GCMClient.getInstance();
                    User u = c.getCurrentUser();
                    if (!(u instanceof Employee emp) || emp.getRole() != EmployeeRole.SUPPORT_AGENT) return null;

                    Message req = new Message(ActionType.LIST_SUPPORT_TICKETS, new ListSupportTicketsRequest(emp.getId()));
                    return c.sendMessage(req);
                },
                (Message resp) -> {
                    rows.clear();
                    if (resp == null) return;

                    if (resp.getMessage() instanceof ListSupportTicketsResponse r) {
                        rows.addAll(r.getRows());
                    }
                },
                (Throwable err) -> new Alert(Alert.AlertType.ERROR, err.getMessage()).showAndWait(),
                null
        );
    }

    @FXML
    private void onOpen() {
        SupportTicketRowDTO selected = tblTickets.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        openReplyDialog(selected);
    }

    private void openReplyDialog(SupportTicketRowDTO row) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ticket #" + row.getTicketId());

        TextArea clientText = new TextArea(row.getPreview());
        clientText.setEditable(false);
        clientText.setWrapText(true);

        TextArea replyText = new TextArea();

        boolean done = row.getStatus() == SupportTicketStatus.DONE;

        if (done) {
            // show what was already sent
            String existing = row.getAgentReply();
            replyText.setText(existing == null ? "" : existing);
            replyText.setEditable(false);   // read-only but visible
            replyText.setWrapText(true);
        } else {
            replyText.setPromptText("Write reply...");
            replyText.setWrapText(true);
        }


        ButtonType sendBtn = new ButtonType("Send", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(sendBtn, ButtonType.CLOSE);

        Button sendNode = (Button) dialog.getDialogPane().lookupButton(sendBtn);
        sendNode.setDisable(done);

        sendNode.setOnAction(e -> {
            String text = replyText.getText() == null ? "" : replyText.getText().trim();
            if (text.isEmpty()) {
                e.consume();
                return;
            }
            doReply(row.getTicketId(), text);
        });

        VBox box = new VBox(10,
                new Label("Client message:"),
                clientText,
                new Label("Agent reply:"),
                replyText
        );

        dialog.getDialogPane().setContent(box);
        dialog.showAndWait();

        refresh();
    }

    private void doReply(int ticketId, String reply) {
        runAsync(
                () -> {
                    GCMClient c = GCMClient.getInstance();
                    User u = c.getCurrentUser();
                    if (!(u instanceof Employee emp)) return null;

                    Message req = new Message(ActionType.REPLY_SUPPORT_TICKET,
                            new ReplySupportTicketRequest(emp.getId(), ticketId, reply));
                    return c.sendMessage(req);
                },
                (Message resp) -> {},
                (Throwable err) -> new Alert(Alert.AlertType.ERROR, err.getMessage()).showAndWait(),
                null
        );
    }

    private <T> void runAsync(java.util.concurrent.Callable<T> work,
                              java.util.function.Consumer<T> onSuccess,
                              java.util.function.Consumer<Throwable> onError,
                              Runnable onFinally) {
        javafx.concurrent.Task<T> task = new javafx.concurrent.Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };

        task.setOnSucceeded(e -> { try { onSuccess.accept(task.getValue()); } finally { if (onFinally != null) onFinally.run(); }});
        task.setOnFailed(e -> { try { onError.accept(task.getException()); } finally { if (onFinally != null) onFinally.run(); }});

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}
