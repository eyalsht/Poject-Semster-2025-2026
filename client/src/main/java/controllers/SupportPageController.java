package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import client.GCMClient;
import common.messaging.Message;
import common.user.User;
import common.enums.ActionType;
import common.support.SupportSubmitRequest;
import common.support.SupportSubmitResponse;
import common.support.CreateSupportTicketRequest;
import common.support.CreateSupportTicketResponse;


public class SupportPageController {

    @FXML private TextArea detailsArea;
    @FXML private ComboBox<String> questionsCombo;
    @FXML private Button btnSend;

    @FXML private ListView<ChatItem> chatList;

    private final ObservableList<ChatItem> chatItems = FXCollections.observableArrayList();

    // Simple model for chat list items
    private static class ChatItem {
        final String text;
        final boolean isBot;
        final java.util.List<SupportChoice> choices; // null/empty -> normal message

        ChatItem(String text, boolean isBot) {
            this(text, isBot, null);
        }

        ChatItem(String text, boolean isBot, java.util.List<SupportChoice> choices) {
            this.text = text;
            this.isBot = isBot;
            this.choices = choices;
        }
    }


    public static class SupportChoice {
        private final int cityId;
        private final String label;

        public SupportChoice(int cityId, String label) {
            this.cityId = cityId;
            this.label = label;
        }
        public int getCityId() { return cityId; }
        public String getLabel() { return label; }
    }


    @FXML
    public void initialize() {

        questionsCombo.setItems(FXCollections.observableArrayList(
                "I can’t log in",
                "Payment issue",
                "Subscription problem",
                "Bug / app not working",
                "When my membership expires?",
                "Other"
        ));

        questionsCombo.getSelectionModel().selectFirst();
        detailsArea.setDisable(true);

        questionsCombo.valueProperty().addListener((obs, oldV, newV) -> {
            boolean isOther = "Other".equals(newV);
            detailsArea.setDisable(!isOther);
            if (!isOther) detailsArea.clear();
        });

        // Attach list items
        chatList.setItems(chatItems);

        // Render each message with color
        chatList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ChatItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                javafx.scene.text.Text t = new javafx.scene.text.Text(item.text);
                t.setFill(item.isBot ? javafx.scene.paint.Color.GREEN : javafx.scene.paint.Color.BLACK);

                javafx.scene.text.TextFlow flow = new javafx.scene.text.TextFlow(t);
                flow.setPrefWidth(0);

                // If this message has choices -> show buttons under it
                if (item.choices != null && !item.choices.isEmpty()) {
                    javafx.scene.layout.FlowPane btnPane = new javafx.scene.layout.FlowPane(8, 8);

                    for (SupportChoice c : item.choices) {
                        Button b = new Button(c.getLabel());
                        b.setOnAction(e -> onCityChoiceClicked(c));
                        btnPane.getChildren().add(b);
                    }

                    javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(6, flow, btnPane);
                    setGraphic(box);
                    return;
                }

                setGraphic(flow);
            }
        });


        // Optional: start conversation
        addBot("Hi! Choose a question above and click Send 🙂");
    }

    @FXML
    private void handleBack() {
        System.out.println("Back clicked");
    }

    @FXML
    private void handleSend() {

        String selected = questionsCombo.getValue();

        String userText;
        if ("Other".equals(selected)) {
            userText = detailsArea.getText() == null ? "" : detailsArea.getText().trim();
            if (userText.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please write details for 'Other'.").showAndWait();
                return;
            }
        } else {
            userText = selected;
        }

        // User message
        addUser(userText);

        // One single decision (NO duplicates)
        if ("When my membership expires?".equals(selected)) {
            sendSupportToServer("MEMBERSHIP_EXPIRE", userText, null);
        }
        else if ("Other".equals(selected)) {
            createTicketOnServer("OTHER", userText);
        }
        else {
            String botReply = getBotReplyLocal(userText);
            addBot(botReply);
        }

        // Cleanup
        if ("Other".equals(selected)) {
            detailsArea.clear();
        }

        // Scroll
        chatList.scrollTo(chatItems.size() - 1);
    }



    private void sendSupportToServer(String topic, String text, Integer cityId) {

        addBot("Checking your account...");
        btnSend.setDisable(true);

        runAsync(
                () -> {
                    GCMClient gcmClient = GCMClient.getInstance();
                    User user = gcmClient.getCurrentUser();
                    if (user == null) return null;

                    SupportSubmitRequest payload =
                            new SupportSubmitRequest(user.getId(), topic, text, cityId);

                    Message req = new Message(ActionType.SUBMIT_SUPPORT_REQUEST, payload);
                    return gcmClient.sendMessage(req);
                },
                (Message resp) -> {
                    if (resp == null) {
                        addBot("You must login first to contact support.");
                        return;
                    }

                    if (!(resp.getMessage() instanceof SupportSubmitResponse data)) {
                        addBot("Server error: bad response.");
                        return;
                    }

                    if (data.getChoices() != null && !data.getChoices().isEmpty()) {
                        var uiChoices = data.getChoices().stream()
                                .map(c -> new SupportChoice(c.getCityId(), c.getLabel()))
                                .toList();
                        addBot(data.getResponseText(), uiChoices);
                    } else {
                        addBot(data.getResponseText());
                    }

                    chatList.scrollTo(chatItems.size() - 1);
                },
                (Throwable err) -> addBot("Support system error: " + err.getMessage()),
                () -> btnSend.setDisable(false)   // ✅ always executed
        );
    }




    private void addUser(String text) {
        chatItems.add(new ChatItem("You: " + text, false));
    }

    private void addBot(String text) {
        chatItems.add(new ChatItem("Bot: " + text, true));
    }

    private void addBot(String text, java.util.List<SupportChoice> choices) {
        chatItems.add(new ChatItem("Bot: " + text, true, choices));
    }




    // Temporary local bot (so UI works NOW).
    // After UI is good, we will connect it to your real server SupportController.
    private String getBotReplyLocal(String userText) {
        String t = userText.toLowerCase();

        if (t.contains("log in")) {
            return "Try: 1) check caps lock 2) retype password 3) restart app. If it still fails, choose 'Other' and write your username + exact error.";
        }
        if (t.contains("payment")) {
            return "Payment issues: please verify your card details and try again. If you were charged but didn't get access, write purchase date + city name in 'Other'.";
        }
        if (t.contains("subscription") || t.contains("membership")) {
            return "Subscription help: If you tell me the city name, I can check what you currently have (when we connect this to the server).";
        }
        if (t.contains("bug")) {
            return "Please write what screen you were in + what button you clicked + what happened. A screenshot helps a lot.";
        }

        return "I’m not sure I can answer this automatically. Please write details in 'Other' and a support agent will handle it.";
    }

    private void onCityChoiceClicked(SupportChoice choice) {
        addUser("City: " + choice.getLabel());

        // Follow up: same topic, but now with cityId
        sendSupportToServer("MEMBERSHIP_EXPIRE", "", choice.getCityId());
    }

    private <T> void runAsync(java.util.concurrent.Callable<T> work,
                              java.util.function.Consumer<T> onSuccess,
                              java.util.function.Consumer<Throwable> onError,
                              Runnable onFinally)
    {

        javafx.concurrent.Task<T> task = new javafx.concurrent.Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };

        task.setOnSucceeded(e -> {
            try {
                onSuccess.accept(task.getValue());
            } finally {
                if (onFinally != null) onFinally.run();
            }
        });

        task.setOnFailed(e -> {
            try {
                onError.accept(task.getException());
            } finally {
                if (onFinally != null) onFinally.run();
            }
        });

        task.setOnCancelled(e -> {
            if (onFinally != null) onFinally.run();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void createTicketOnServer(String topic, String text) {
        addBot("Sending your request to support...");
        btnSend.setDisable(true);

        runAsync(
                () -> {
                    GCMClient gcmClient = GCMClient.getInstance();
                    User user = gcmClient.getCurrentUser();
                    if (user == null) return null;

                    CreateSupportTicketRequest payload =
                            new CreateSupportTicketRequest(user.getId(), topic, text);

                    Message req = new Message(ActionType.CREATE_SUPPORT_TICKET, payload);
                    return gcmClient.sendMessage(req);
                },
                (Message resp) -> {
                    if (resp == null) {
                        addBot("You must login first to contact support.");
                        return;
                    }

                    if (resp.getMessage() instanceof CreateSupportTicketResponse r) {
                        addBot("Sent ✅ Ticket #" + r.getTicketId() + ". A support agent will reply soon.");
                    } else {
                        addBot("Sent ✅ A support agent will reply soon.");
                    }
                },
                (Throwable err) -> addBot("Support error: " + err.getMessage()),
                () -> btnSend.setDisable(false)
        );
    }


}
