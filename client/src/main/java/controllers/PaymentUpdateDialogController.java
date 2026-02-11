package controllers;

import client.GCMClient;
import common.enums.ActionType;
import common.messaging.Message;
import common.purchase.PaymentDetails;
import common.user.Client;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class PaymentUpdateDialogController {

    @FXML private TextField txtCardNumber;
    @FXML private TextField txtExpiryMonth;
    @FXML private TextField txtExpiryYear;
    @FXML private TextField txtCVV;
    @FXML private Label lblError;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private boolean saved = false;

    private static final Pattern CARD_PATTERN = Pattern.compile("^\\d{16}$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^\\d{3}$");
    private static final Pattern MONTH_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])$");
    private static final Pattern YEAR_PATTERN = Pattern.compile("^\\d{2}$");

    @FXML
    public void initialize() {
        // Pre-fill with existing card info if available
        if (GCMClient.getInstance().getCurrentUser() instanceof Client client) {
            PaymentDetails pd = client.getPaymentDetails();
            if (pd != null) {
                if (pd.getCreditCardNumber() != null) txtCardNumber.setText(pd.getCreditCardNumber());
                if (pd.getExpiryMonth() != null) txtExpiryMonth.setText(pd.getExpiryMonth());
                if (pd.getExpiryYear() != null) txtExpiryYear.setText(pd.getExpiryYear());
                if (pd.getCvv() != null) txtCVV.setText(pd.getCvv());
            }
        }
    }

    @FXML
    private void onSave() {
        if (!validate()) return;

        btnSave.setDisable(true);
        lblError.setText("");

        PaymentDetails newPayment = new PaymentDetails(
            txtCardNumber.getText().trim(),
            txtCVV.getText().trim(),
            txtExpiryMonth.getText().trim(),
            txtExpiryYear.getText().trim()
        );

        int userId = GCMClient.getInstance().getCurrentUser().getId();

        new Thread(() -> {
            try {
                ArrayList<Object> data = new ArrayList<>();
                data.add(userId);
                data.add(newPayment);

                Message request = new Message(ActionType.UPDATE_PAYMENT_DETAILS_REQUEST, data);
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

                boolean success = response != null
                    && response.getAction() == ActionType.UPDATE_PAYMENT_DETAILS_RESPONSE
                    && Boolean.TRUE.equals(response.getMessage());

                Platform.runLater(() -> {
                    if (success) {
                        // Update the local cached user's payment details
                        if (GCMClient.getInstance().getCurrentUser() instanceof Client client) {
                            client.setPaymentDetails(newPayment);
                        }
                        saved = true;
                        Stage stage = (Stage) btnSave.getScene().getWindow();
                        stage.close();
                    } else {
                        lblError.setText("Failed to update payment details.");
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
        String card = txtCardNumber.getText().trim();
        String month = txtExpiryMonth.getText().trim();
        String year = txtExpiryYear.getText().trim();
        String cvv = txtCVV.getText().trim();

        if (card.isEmpty() || month.isEmpty() || year.isEmpty() || cvv.isEmpty()) {
            lblError.setText("Please fill in all fields.");
            return false;
        }
        if (!CARD_PATTERN.matcher(card).matches()) {
            lblError.setText("Card number must be 16 digits.");
            return false;
        }
        if (!MONTH_PATTERN.matcher(month).matches()) {
            lblError.setText("Invalid month (01-12).");
            return false;
        }
        if (!YEAR_PATTERN.matcher(year).matches()) {
            lblError.setText("Year must be 2 digits (e.g. 28).");
            return false;
        }
        if (!CVV_PATTERN.matcher(cvv).matches()) {
            lblError.setText("CVV must be 3 digits.");
            return false;
        }
        return true;
    }

    @FXML
    private void onCancel() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    public boolean isSaved() {
        return saved;
    }
}
