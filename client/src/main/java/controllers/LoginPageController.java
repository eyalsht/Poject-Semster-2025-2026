package controllers;


import client.GCMClient;
import common.Message;
import common.User;
import common.actionType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import java.util.ArrayList;

    public class LoginPageController {

        @FXML private TextField tfUsername;
        @FXML private PasswordField tfPassword;
        @FXML private Label lblError;

        private final GCMClient client = GCMClient.getInstance();

        @FXML
        public void initialize() {
            lblError.setText("");
        }

        @FXML
        private void handleLogin() {
            String username = tfUsername.getText();
            String password = tfPassword.getText();



            lblError.setText("Checking credentials...");
            lblError.setTextFill(Color.BLACK);

            new Thread(() -> {
                ArrayList<String> creds = new ArrayList<>();
                creds.add(username);
                creds.add(password);

                Message request = new Message(actionType.LOGIN_REQUEST, creds);
                Object response = client.sendRequest(request);

                Platform.runLater(() -> {
                    if (response instanceof Message msg) {
                        if (msg.getAction() == actionType.LOGIN_SUCCESS) {
                            User loggedInUser = (User) msg.getMessage();
                            client.setCurrentUser(loggedInUser);
                            lblError.setTextFill(Color.GREEN);
                            lblError.setText("Login Successful! Welcome " + loggedInUser.getUsername());

                        } else if (msg.getAction() == actionType.LOGIN_FAILED) {
                            String errorMsg = (String) msg.getMessage();
                            lblError.setTextFill(Color.RED);
                            lblError.setText(errorMsg);
                        }
                    } else {
                        lblError.setTextFill(Color.RED);
                        lblError.setText("Server communication error.");
                    }
                });
            }).start();
        }
    }
