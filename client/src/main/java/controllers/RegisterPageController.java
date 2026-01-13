package controllers;

import client.GCMClient;
import common.messaging.Message;
import common.enums.ActionType;
import common.dto.AuthResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;

/**
 * Controller for the registration page.
 */
public class RegisterPageController {

    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtId;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtCardNumber;
    @FXML private Label lblError;

    private Stage dialogStage;
    private LoginPageController loginController;

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setLoginController(LoginPageController controller) {
        this.loginController = controller;
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    @FXML
    private void handleRegister() {
        // Collect and validate input
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String idNumber = txtId.getText().trim();
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText();
        String cardNumber = txtCardNumber.getText().trim();

        // Validation
        if (firstName.isEmpty() || lastName.isEmpty() || idNumber.isEmpty() ||
            email.isEmpty() || password.isEmpty() || cardNumber.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address.");
            return;
        }

        if (password.length() < 8) {
            showError("Password must be at least 8 characters.");
            return;
        }

        showMessage("Registering...", Color.GRAY);

        // Send registration request
        new Thread(() -> {
            try {
                ArrayList<String> data = new ArrayList<>();
                data.add(firstName);   // index 0
                data.add(lastName);    // index 1
                data.add(idNumber);    // index 2
                data.add(email);       // index 3
                data.add(password);    // index 4
                data.add(cardNumber);  // index 5

                Message request = new Message(ActionType.REGISTER_REQUEST, data);
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

                Platform.runLater(() -> handleRegisterResponse(response, email));

            } catch (Exception e) {
                Platform.runLater(() -> showError("Connection error: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Handle the registration response from server.
     */
    private void handleRegisterResponse(Message response, String email) {
        if (response == null) {
            showError("No response from server.");
            return;
        }

        if (response.getAction() == ActionType.REGISTER_RESPONSE) {
            AuthResponse authResponse = (AuthResponse) response.getMessage();
            
            if (authResponse.isSuccess()) {
                // Notify login controller and close dialog
                if (loginController != null) {
                    loginController.fillAfterRegister(email);
                }
                if (dialogStage != null) {
                    dialogStage.close();
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

    // ==================== HELPER METHODS ====================

    private void showError(String message) {
        lblError.setTextFill(Color.RED);
        lblError.setText(message);
    }

    private void showMessage(String message, Color color) {
        lblError.setTextFill(color);
        lblError.setText(message);
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
