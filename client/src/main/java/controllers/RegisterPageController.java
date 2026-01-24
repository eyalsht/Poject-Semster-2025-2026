package controllers;

import client.GCMClient;
import common.messaging.Message;
import common.enums.ActionType;
import common.dto.AuthResponse;
import common.purchase.PaymentDetails;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class RegisterPageController {

    // --- Step 1: Personal Details ---
    @FXML
    private VBox step1Container;
    @FXML
    private TextField txtFirstName;
    @FXML
    private TextField txtLastName;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private PasswordField txtConfirmPassword;
    @FXML
    private TextField txtPhoneNumber;

    // --- Step 2: Payment Details ---
    @FXML
    private VBox step2Container;
    @FXML
    private TextField txtCardNumber;
    @FXML
    private TextField txtCVV;
    @FXML
    private TextField txtExpiryMonth;
    @FXML
    private TextField txtExpiryYear;

    // --- General ---
    @FXML
    private Label lblError;
    private Stage dialogStage;
    private LoginPageController loginController;

    // Regex Patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\u0590-\\u05FF\\s]+$"); // Hebrew and English
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d{9}$"); // Israeli ID - 9 digits
    private static final Pattern PHONE_PATTERN = Pattern.compile("^05\\d-?\\d{7}$"); // Israeli phone format
    private static final Pattern CARD_PATTERN = Pattern.compile("^\\d{16}$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^\\d{3}$");
    private static final Pattern MONTH_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])$");
    private static final Pattern YEAR_PATTERN = Pattern.compile("^\\d{2}$"); // YY

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setLoginController(LoginPageController controller) {
        this.loginController = controller;
    }

    @FXML
    public void initialize() {
        // Start: Show step 1, hide step 2
        showStep1();
    }

    // --- Navigation Logic ---

    @FXML
    private void handleNext() {
        if (validateStep1()) {
            step1Container.setVisible(false);
            step1Container.setManaged(false); // So it doesn't take up space
            step2Container.setVisible(true);
            step2Container.setManaged(true);
            lblError.setText(""); // Clear errors
        }
    }

    @FXML
    private void handleBack() {
        showStep1();
    }

    private void showStep1() {
        step2Container.setVisible(false);
        step2Container.setManaged(false);
        step1Container.setVisible(true);
        step1Container.setManaged(true);
        lblError.setText("");
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) dialogStage.close();
    }

    // --- Validation Logic ---

    private boolean validateStep1() {
        if (txtFirstName.getText().isEmpty() || txtLastName.getText().isEmpty() ||
                txtEmail.getText().isEmpty() ||
                txtUsername.getText().isEmpty() || txtPassword.getText().isEmpty()) {
            showError("Please fill in all personal details.");
            return false;
        }

        if (!NAME_PATTERN.matcher(txtFirstName.getText()).matches() ||
                !NAME_PATTERN.matcher(txtLastName.getText()).matches()) {
            showError("Names should contain only letters.");
            return false;
        }

        if (!EMAIL_PATTERN.matcher(txtEmail.getText()).matches()) {
            showError("Invalid email format.");
            return false;
        }

        if (!PHONE_PATTERN.matcher(txtPhoneNumber.getText()).matches()) {
            showError("Invalid phone number (e.g., 050-1234567).");
            return false;
        }

        if (txtPassword.getText().length() < 6) {
            showError("Password must be at least 6 characters.");
            return false;
        }

        if (!txtConfirmPassword.getText().equals(txtPassword.getText())) {
            showError("Passwords do not match.");
            return false;
        }

        return true;
    }

    private boolean validateStep2() {
        if (txtCardNumber.getText().isEmpty() || txtCVV.getText().isEmpty() ||
                txtExpiryMonth.getText().isEmpty() || txtExpiryYear.getText().isEmpty()) {
            showError("Please fill in all payment details.");
            return false;
        }

        if (!CARD_PATTERN.matcher(txtCardNumber.getText()).matches()) {
            showError("Card number must be 16 digits.");
            return false;
        }

        if (!CVV_PATTERN.matcher(txtCVV.getText()).matches()) {
            showError("CVV must be 3 digits.");
            return false;
        }

        if (!MONTH_PATTERN.matcher(txtExpiryMonth.getText()).matches()) {
            showError("Invalid month (01-12).");
            return false;
        }

        // Basic expiry validation (optional: check if date has passed)
        if (!YEAR_PATTERN.matcher(txtExpiryYear.getText()).matches()) {
            showError("Year must be 2 digits (e.g. 26).");
            return false;
        }

        return true;
    }

    // --- Final Submission ---

    @FXML
    private void handleFinish() {
        if (!validateStep2()) return;

        showMessage("Processing registration...", Color.BLUE);

        // Prepare data
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String email = txtEmail.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String phone = txtPhoneNumber.getText().trim();

        PaymentDetails payment = new PaymentDetails(
                txtCardNumber.getText().trim(),
                txtCVV.getText().trim(),
                txtExpiryMonth.getText().trim(),
                txtExpiryYear.getText().trim()
        );

        // Send to Server
        new Thread(() -> {
            try {
                ArrayList<Object> data = new ArrayList<>();
                data.add(firstName);
                data.add(lastName);
                data.add(email);
                data.add(phone);
                data.add(username);
                data.add(password);
                data.add(payment);

                Message request = new Message(ActionType.REGISTER_REQUEST, data);
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

                Platform.runLater(() -> handleRegisterResponse(response, username));

            } catch (Exception e) {
                Platform.runLater(() -> showError("Connection error: " + e.getMessage()));
            }
        }).start();
    }

    private void handleRegisterResponse(Message response, String username) {
        if (response != null && response.getAction() == ActionType.REGISTER_RESPONSE) {
            AuthResponse authResponse = (AuthResponse) response.getMessage();
            if (authResponse.isSuccess()) {
                if (loginController != null) loginController.fillAfterRegister(username);
                dialogStage.close();
            } else {
                showError(authResponse.getMessage());
            }
        } else if (response != null && response.getAction() == ActionType.ERROR) {
            showError("Server error: " + response.getMessage());
        } else {
            showError("Registration failed. Unknown server response.");
        }
    }

    private void showError(String msg) {
        showMessage(msg, Color.RED);
    }

    private void showMessage(String msg, Color color) {
        lblError.setTextFill(color);
        lblError.setText(msg);
    }
}