package controllers;

import client.GCMClient;
import common.enums.ActionType;
import common.messaging.Message;
import common.support.ListClientSupportRepliesRequest;
import common.support.ListClientSupportRepliesResponse;
import common.user.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ProfilePageController {

    @FXML private Label lblWelcome;
    @FXML private Label lblName;
    @FXML private Label lblEmail;
    @FXML private Label lblPayment;
    @FXML private Label lblInbox;
    @FXML private Label lblHistory;
    @FXML private javafx.scene.control.Button btnOpenInbox;

    public void setUser(User user) {
        if (user == null) return;

        String first = user.getFirstName();
        String last  = user.getLastName();

        String fullName = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
        if (fullName.isBlank()) fullName = user.getUsername();

        lblWelcome.setText("Welcome " + (first != null && !first.isBlank() ? first : user.getUsername()));
        lblName.setText(fullName);
        lblEmail.setText(user.getEmail() != null ? user.getEmail() : user.getUsername());

        lblPayment.setText("Not set");
        lblInbox.setText("You have 0 new messages");
        lblHistory.setText("(coming soon)");

        loadInboxCount(user);
    }

    private void loadInboxCount(User user) {
        runAsync(
                () -> {
                    Message req = new Message(
                            ActionType.LIST_CLIENT_SUPPORT_REPLIES,
                            new ListClientSupportRepliesRequest(user.getId())
                    );
                    return GCMClient.getInstance().sendMessage(req);
                },
                (Message resp) -> {
                    if (resp != null && resp.getMessage() instanceof ListClientSupportRepliesResponse r) {
                        long unread = r.getRows().stream().filter(x -> !x.isReadByClient()).count();
                        lblInbox.setText("You have " + unread + " new messages");
                    }
                },
                (Throwable err) -> {
                    // optional
                },
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
