package controllers;

import client.GCMClient;
import common.Message;
import common.actionType;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

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

    public void setDialogStage(Stage stage) { this.dialogStage = stage; }
    public void setLoginController(LoginPageController c) { this.loginController = c; }

    @FXML
    private void handleClose() {
        if (dialogStage != null) dialogStage.close();
    }

    @FXML
    private void handleRegister() {
        // 1) validate locally
        String first = txtFirstName.getText().trim();
        String last  = txtLastName.getText().trim();
        String id    = txtId.getText().trim();
        String email = txtEmail.getText().trim();
        String pass  = txtPassword.getText();
        String card  = txtCardNumber.getText().trim();

        if (first.isEmpty() || last.isEmpty() || id.isEmpty() || email.isEmpty() || pass.isEmpty() || card.isEmpty()) {
            lblError.setText("Please fill all fields.");
            return;
        }

        // 2) build request payload (keep it simple for now)
        Map<String, String> data = new HashMap<>();
        data.put("firstName", first);
        data.put("lastName", last);
        data.put("id", id);
        data.put("email", email);
        data.put("password", pass);
        data.put("card", card);

        // 3) send to server
        new Thread(() -> {
            try {
                GCMClient client = GCMClient.getInstance();
                Message req = new Message(actionType.REGISTER_REQUEST, data);
                Object resp = client.sendRequest(req);

                javafx.application.Platform.runLater(() -> {
                    if (resp instanceof Message msg) {
                        if (msg.getAction() == actionType.REGISTER_SUCCESS) {
                            // auto-fill login
                            if (loginController != null) {
                                loginController.fillAfterRegister(email);
                            }
                            if (dialogStage != null) dialogStage.close();
                        } else {
                            lblError.setText(String.valueOf(msg.getMessage()));
                        }
                    } else {
                        lblError.setText("Server communication error.");
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> lblError.setText("Error: " + e.getMessage()));
            }
        }).start();
    }
}
