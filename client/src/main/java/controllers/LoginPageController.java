package controllers;


import client.GCMClient;
import main.java.common.Message;
import main.java.common.User;
import main.java.common.actionType;
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
        @FXML private Label lblError; // וודא שיש לך Label כזה ב-FXML

        private final GCMClient client = GCMClient.getInstance();

        @FXML
        public void initialize() {
            lblError.setText(""); // איפוס הודעות שגיאה בעלייה
        }

        @FXML
        private void handleLogin() {
            String username = tfUsername.getText();
            String password = tfPassword.getText();

            // 1. בדיקות תקינות מקומיות (Client Side Validation)


            // 2. שליחה לשרת
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
                            // מעבר למסך הבא
                            lblError.setTextFill(Color.GREEN);
                            lblError.setText("Login Successful! Welcome " + loggedInUser.getUsername());
                            // כאן תבוא פונקציית הניווט (navigateToMainScreen)

                        } else if (msg.getAction() == actionType.LOGIN_FAILED) {
                            // הצגת הודעת השגיאה המדויקת שהגיעה מהשרת (כמו במעבדה)
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
