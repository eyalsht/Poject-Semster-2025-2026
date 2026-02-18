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


public class SupportPageController
{

    @FXML private TextArea detailsArea;
    @FXML private ComboBox<String> questionsCombo;
    @FXML private Button btnSend;

    @FXML private ListView<ChatItem> chatList;
    private static final String DEFAULT_QUESTION = "Choose your question here :)";

    private boolean suppressAutoSend = true;

    private final ObservableList<ChatItem> chatItems = FXCollections.observableArrayList();

    private static class ChatItem {
        final String text;
        final boolean isBot;
        final java.util.List<SupportChoice> choices;

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



    public void initialize() {
        //Those are costom questions prepared for a client to choose from
        questionsCombo.setItems(FXCollections.observableArrayList(
                DEFAULT_QUESTION,
                "I can’t log in",
                "Payment issue",
                "Subscription problem",
                "Bug / app not working",
                "When my membership expires?",
                "Other"
        ));


        chatList.setItems(chatItems);


        chatList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ChatItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Text t = new Text(item.text);
                t.setFill(item.isBot ? Color.GREEN : Color.BLACK);

                TextFlow flow = new TextFlow(t);
                flow.setPrefWidth(0);

                if (item.choices != null && !item.choices.isEmpty()) {
                    javafx.scene.layout.FlowPane btnPane = new javafx.scene.layout.FlowPane(8, 8);

                    for (SupportChoice c : item.choices) {
                        Button b = new Button(c.getLabel());
                        b.getStyleClass().add("support-city-btn");
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


        suppressAutoSend = true;
        questionsCombo.getSelectionModel().select(DEFAULT_QUESTION);
        updateInputState();

        questionsCombo.valueProperty().addListener((obs, oldV, newV) -> {
            updateInputState();

            if (newV == null) return;

            // If client pick the started, default question again
            if (DEFAULT_QUESTION.equals(newV)) {
                addBot("Please choose a question from the list or write your own by choosing \"Other\" :)");
                return;
            }

            boolean isOther = "Other".equals(newV);

            // auto-send for predefined questions (but not for "Other")
            if (!suppressAutoSend && !isOther) {
                handleSend();
            }
        });


        addBot("Hi! Choose a question above 🙂");

        suppressAutoSend = false;
    }

    // enable typing + send only for "Other"
    void updateInputState() {
        String selected = questionsCombo.getValue();

        boolean isDefault = DEFAULT_QUESTION.equals(selected) || selected == null;
        boolean isOther = "Other".equals(selected);

        // details + send allowed only for "Other"
        detailsArea.setDisable(!isOther);

        // send button enabled only for "Other"
        btnSend.setDisable(!isOther);

        if (!isOther) detailsArea.clear();
    }




    @FXML
    private void handleBack() {
        System.out.println("Back clicked");
    }

    @FXML
    private void handleSend() {

        String selected = questionsCombo.getValue();
        if (selected == null || DEFAULT_QUESTION.equals(selected)) {
            addBot("Please choose a question from the list or write your own by choosing \"Other\" :)");
            return;
        }

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

        // clients message
        addUser(userText);

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

        if ("Other".equals(selected)) {
            detailsArea.clear();
        }

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
                () -> btnSend.setDisable(false)
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




    //those are different pre prepared answers for questions client can choose from
    private String getBotReplyLocal(String userText) {
        String t = userText.toLowerCase();

        if (t.contains("log in")) {
            return "Try: 1) check caps lock 2) retype password 3) restart app. If it still fails, choose 'Other' and write your username + exact error below.";
        }
        if (t.contains("payment")) {
            return "Payment issues: please verify your card details and try again. If you were charged but didn't get access, choose 'Other' and write purchase date + city name below.";
        }
        if (t.contains("subscription") || t.contains("membership")) {
            return "Subscription help: Please choose 'Other' and write what is the problem exactly. Our support agent will write you back!";
        }
        if (t.contains("bug")) {
            return "Please choose 'Other' and write what screen you were in + what button you clicked + what happened.";
        }

        return "I’m not sure I can answer this automatically. Please choose 'Other' and write details below. Support agent will handle it within 24 hours.";
    }

    private void onCityChoiceClicked(SupportChoice choice) {
        addUser("City: " + choice.getLabel());
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
