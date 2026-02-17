package controllers;

import client.GCMClient;
import common.enums.ActionType;
import common.messaging.Message;
import common.user.Client;
import common.user.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class PersonalDetailsUpdateDialogController {

    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private Label lblPhoneLabel;
    @FXML private Label lblError;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private boolean saved = false;

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\u0590-\\u05FF\\s]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^05\\d-?\\d{7}$");

    @FXML
    public void initialize() {
        User user = GCMClient.getInstance().getCurrentUser();
        if (user == null) return;

        if (user.getFirstName() != null) txtFirstName.setText(user.getFirstName());
        if (user.getLastName() != null) txtLastName.setText(user.getLastName());
        if (user.getEmail() != null) txtEmail.setText(user.getEmail());

        if (user instanceof Client client) {
            if (client.getPhoneNumber() != null) txtPhone.setText(client.getPhoneNumber());
        } else {
            // Hide phone row for non-Client users
            lblPhoneLabel.setVisible(false);
            lblPhoneLabel.setManaged(false);
            txtPhone.setVisible(false);
            txtPhone.setManaged(false);
        }
    }

    @FXML
    private void onSave() {
        if (!validate()) return;

        btnSave.setDisable(true);
        lblError.setText("");

        User user = GCMClient.getInstance().getCurrentUser();
        int userId = user.getId();
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = (user instanceof Client) ? txtPhone.getText().trim() : "";

        new Thread(() -> {
            try {
                ArrayList<Object> data = new ArrayList<>();
                data.add(userId);
                data.add(firstName);
                data.add(lastName);
                data.add(email);
                data.add(phone);

                Message request = new Message(ActionType.UPDATE_PERSONAL_DETAILS_REQUEST, data);
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null
                            && response.getAction() == ActionType.UPDATE_PERSONAL_DETAILS_RESPONSE) {
                        String result = (String) response.getMessage();
                        if ("OK".equals(result)) {
                            // Update the cached user object
                            user.setFirstName(firstName);
                            user.setLastName(lastName);
                            user.setEmail(email);
                            if (user instanceof Client client) {
                                client.setPhoneNumber(phone);
                            }
                            saved = true;
                            ((Stage) btnSave.getScene().getWindow()).close();
                        } else {
                            lblError.setText(result);
                            btnSave.setDisable(false);
                        }
                    } else {
                        lblError.setText("Failed to update personal details.");
                        btnSave.setDisable(false);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblError.setText("Error: " + e.getMessage());
                    btnSave.setDisable(false);
                });
            }
        }).start();
    }

    private boolean validate() {
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String email = txtEmail.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            lblError.setText("Please fill in all required fields.");
            return false;
        }
        if (!NAME_PATTERN.matcher(firstName).matches() || !NAME_PATTERN.matcher(lastName).matches()) {
            lblError.setText("Names should contain only letters.");
            return false;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            lblError.setText("Invalid email format (e.g., user@example.com).");
            return false;
        }

        User user = GCMClient.getInstance().getCurrentUser();
        if (user instanceof Client) {
            String phone = txtPhone.getText().trim();
            if (phone.isEmpty()) {
                lblError.setText("Please enter your phone number.");
                return false;
            }
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                lblError.setText("Invalid phone number (e.g., 050-1234567).");
                return false;
            }
        }

        return true;
    }

    @FXML
    private void onCancel() {
        ((Stage) btnCancel.getScene().getWindow()).close();
    }

    public boolean isSaved() {
        return saved;
    }
}
