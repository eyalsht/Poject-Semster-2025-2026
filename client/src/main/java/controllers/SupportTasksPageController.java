package controllers;

import javafx.scene.input.MouseButton;
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
import util.TableRowHighlighter;

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

        TableRowHighlighter.apply(tblTickets, t -> t.getStatus() != SupportTicketStatus.DONE);
        tblTickets.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                SupportTicketRowDTO selected = tblTickets.getSelectionModel().getSelectedItem();
                if (selected != null) onOpen(); // reuse same logic as the button
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

                        rows.sort((a, b) -> {
                            // OPEN first, DONE after
                            int ga = (a.getStatus() == SupportTicketStatus.OPEN) ? 0 : 1;
                            int gb = (b.getStatus() == SupportTicketStatus.OPEN) ? 0 : 1;
                            if (ga != gb) return Integer.compare(ga, gb);

                            // OPEN: createdAt ASC (oldest first)
                            if (a.getStatus() == SupportTicketStatus.OPEN) {
                                if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                                if (a.getCreatedAt() == null) return 1;
                                if (b.getCreatedAt() == null) return -1;
                                return a.getCreatedAt().compareTo(b.getCreatedAt());
                            }

                            // DONE: repliedAt DESC (newest first)
                            if (a.getRepliedAt() == null && b.getRepliedAt() == null) return 0;
                            if (a.getRepliedAt() == null) return 1;
                            if (b.getRepliedAt() == null) return -1;
                            return b.getRepliedAt().compareTo(a.getRepliedAt());
                        });
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

        String css = client.MainApplication.class.getResource("/styles/theme.css").toExternalForm();

        // Apply to dialog pane (sometimes not enough alone)
        dialog.getDialogPane().getStylesheets().add(css);

        // GUARANTEED: apply to the Dialog's Scene once it exists
        dialog.getDialogPane().sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && !newScene.getStylesheets().contains(css)) {
                newScene.getStylesheets().add(css);
            }
        });

        dialog.setTitle("Ticket #" + row.getTicketId());

        String full = row.getClientText();
        if (full == null || full.isBlank()) full = row.getPreview(); // fallback
        TextArea clientText = new TextArea(full);

        clientText.setEditable(false);
        clientText.setWrapText(true);

        TextArea replyText = new TextArea();

        boolean done = row.getStatus() == SupportTicketStatus.DONE;

        if (done) {
            String existing = row.getAgentReply();
            replyText.setText(existing == null ? "" : existing);
            replyText.setEditable(false);
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

        box.getStyleClass().add("welcome-card");

        dialog.getDialogPane().setContent(box);
        dialog.showAndWait();
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
                (Message resp) -> javafx.application.Platform.runLater(this::refresh),
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
