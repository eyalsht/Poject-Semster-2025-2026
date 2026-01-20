package controllers;


import client.GCMClient;
import common.messaging.Message;
import common.user.User;
import common.enums.ActionType;
import common.dto.AuthResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;

/**
 * Controller for the login page.
 * Handles user authentication.
 */
public class LoginPageController {

    @FXML private TextField tfUsername;
    @FXML private PasswordField tfPassword;
    @FXML private Label lblError;

    private HomePageController homePageController;
    private final GCMClient client = GCMClient.getInstance();

    public void setHomePageController(HomePageController controller) {
        this.homePageController = controller;
    }

    @FXML
    public void initialize() {
        clearError();
    }

    @FXML
    private void handleLogin() {
        String username = tfUsername.getText().trim();
        String password = tfPassword.getText();

        // Basic validation
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password.");
            return;
        }

        showMessage("Checking credentials...", Color.GRAY);

        // Send login request in background thread
        new Thread(() -> {
            try {
                ArrayList<String> credentials = new ArrayList<>();
                credentials.add(username);
                credentials.add(password);

                Message request = new Message(ActionType.LOGIN_REQUEST, credentials);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> handleLoginResponse(response));

            } catch (Exception e) {
                Platform.runLater(() -> showError("Connection error: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Handle the login response from server.
     */
    private void handleLoginResponse(Message response) {
        if (response == null) {
            showError("No response from server.");
            return;
        }

        if (response.getAction() == ActionType.LOGIN_RESPONSE) {
            AuthResponse authResponse = (AuthResponse) response.getMessage();

            if (authResponse.isSuccess()) {
                User user = authResponse.getUser();
                if (user.isLoggedIn()) {
                    showError("User is already logged in.");
                }
                else {
                    client.setCurrentUser(user);

                    showMessage("Welcome, " + user.getFirstName() + "!", Color.GREEN);

                    if (homePageController != null) {
                        homePageController.onLoginSuccess(user);
                    }
                }
            } else {
                showError(authResponse.getMessage());
            }
        } else if (response.getAction() == ActionType.ERROR) {
            showError("Server error: " + response.getMessage());
        } else {
            showError("Unexpected response from server.");
        }
    }

    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/RegisterPage.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Register New Account");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            RegisterPageController controller = loader.getController();
            controller.setDialogStage(stage);
            controller.setLoginController(this);

            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Could not open registration form.");
        }
    }

    /**
     * Called by RegisterPageController after successful registration.
     */
    public void fillAfterRegister(String email) {
        tfUsername.setText(email);
        tfPassword.clear();
        tfPassword.requestFocus();
        showMessage("Registration successful! Please log in.", Color.GREEN);
    }

    // ==================== HELPER METHODS ====================

    private void showError(String message) {
        lblError.setTextFill(Color.RED);
        lblError.setText(message);
    }

    private void showMessage(String message, Color color) {
        lblError.setTextFill(color);
        lblError.setText(message);
    }

    private void clearError() {
        lblError.setText("");
    }
}