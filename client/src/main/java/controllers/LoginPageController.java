package controllers;


import client.GCMClient;
import common.Message;
import common.User;
import common.actionType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.stage.Modality;

import java.util.ArrayList;

    public class LoginPageController
    {

        @FXML private TextField tfUsername;
        @FXML private PasswordField tfPassword;
        @FXML private Label lblError;
        private HomePageController homePageController;

        public void setHomePageController(HomePageController homePageController)
        {
            System.out.println("LoginPageController got HomePageController injected ✅");
            this.homePageController = homePageController;
        }

        private final GCMClient client = GCMClient.getInstance();

        @FXML
        public void initialize()
        {
            lblError.setText("");
            client.setLoginController(this);
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
                            System.out.println("homePageController = " + homePageController);
                            if (homePageController != null)
                            {
                                homePageController.onLoginSuccess(loggedInUser);
                            }
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
        @FXML
        private void handleRegister()
        {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/RegisterPage.fxml"));
                Parent root = loader.load();

                Stage stage = new Stage();
                stage.setTitle("Register");
                stage.setScene(new Scene(root));
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setResizable(false);

                RegisterPageController regCtrl = loader.getController();
                regCtrl.setDialogStage(stage);
                regCtrl.setLoginController(this);

                stage.showAndWait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        public void fillAfterRegister(String email) {
            tfUsername.setText(email);
            tfPassword.clear();
        }
        public void onServerMessage(Message msg)
        {
            if (msg.getAction() == actionType.LOGIN_SUCCESS) {
                User loggedInUser = (User) msg.getMessage();
                System.out.println("firstName=" + loggedInUser.getFirstName()
                        + " lastName=" + loggedInUser.getLastName()
                        + " username=" + loggedInUser.getUsername()
                        + " email=" + loggedInUser.getEmail());
                client.setCurrentUser(loggedInUser);

                if (homePageController != null) {
                    homePageController.onLoginSuccess(loggedInUser);
                }
                return;
            }

            if (msg.getAction() == actionType.LOGIN_FAILED) {
                String errorMsg = (String) msg.getMessage();
                lblError.setTextFill(Color.RED);
                lblError.setText(errorMsg);
            }
        }


    }
