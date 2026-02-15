package controllers;

import client.GCMClient;
import common.enums.ActionType;
import common.messaging.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;

public class PurchaseConfirmationDialogController {

    @FXML private Label lblTitle;
    @FXML private Label lblDetails;
    @FXML private Label lblPrice;
    @FXML private Label lblDiscount;
    @FXML private Label lblStatus;
    @FXML private ComboBox<String> cbMonths;
    @FXML private HBox hboxMonths;
    @FXML private ProgressBar progressBar;
    @FXML private Button btnConfirm;
    @FXML private Button btnCancel;
    @FXML private Button btnUpdatePayment;

    private String purchaseType;
    private int cityId;
    private int mapId;
    private double monthlyPrice;
    private double mapPrice;
    private boolean isRenewal;
    private boolean isUpgrade;
    private boolean purchaseComplete = false;

    @FXML
    public void initialize() {
        cbMonths.getItems().addAll(
            "1 month",
            "3 months (5% off)",
            "6 months (10% off)",
            "12 months (15% off)"
        );
        cbMonths.getSelectionModel().selectFirst();
        cbMonths.setOnAction(e -> updatePrice());
    }

    /**
     * Configure for a subscription purchase.
     */
    public void setSubscriptionData(String cityName, int cityId, double monthlyPrice, boolean isRenewal) {
        this.purchaseType = "SUBSCRIPTION";
        this.cityId = cityId;
        this.monthlyPrice = monthlyPrice;
        this.isRenewal = isRenewal;

        lblTitle.setText(isRenewal ? "Extend Subscription" : "Subscribe to City");
        lblDetails.setText("City: " + cityName + "\nMonthly rate: $" + String.format("%.2f", monthlyPrice));

        hboxMonths.setVisible(true);
        hboxMonths.setManaged(true);

        if (isRenewal) {
            lblDiscount.setText("Renewal discount: 10% off!");
            lblDiscount.setVisible(true);
            lblDiscount.setManaged(true);
        }

        updatePrice();
    }

    /**
     * Configure for a one-time map purchase.
     */
    public void setMapPurchaseData(String mapName, int mapId, int cityId, double price,
                                    boolean isUpgrade, String oldVersion, String newVersion) {
        this.purchaseType = "ONE_TIME";
        this.mapId = mapId;
        this.cityId = cityId;
        this.mapPrice = price;
        this.isUpgrade = isUpgrade;

        hboxMonths.setVisible(false);
        hboxMonths.setManaged(false);

        if (isUpgrade) {
            lblTitle.setText("Upgrade Map");
            lblDetails.setText("Map: " + mapName + "\nUpgrade: v" + oldVersion + " -> v" + newVersion);
            lblDiscount.setText("Upgrade discount: 50% off!");
            lblDiscount.setVisible(true);
            lblDiscount.setManaged(true);
            lblPrice.setText("Total: $" + String.format("%.2f", price));
        } else {
            lblTitle.setText("Buy Map");
            lblDetails.setText("Map: " + mapName);
            lblPrice.setText("Total: $" + String.format("%.2f", price));
        }
    }

    private void updatePrice() {
        if (!"SUBSCRIPTION".equals(purchaseType)) return;

        int months = getSelectedMonths();

        double durationDiscount = 0;
        if (months >= 12) durationDiscount = 0.15;
        else if (months >= 6) durationDiscount = 0.10;
        else if (months >= 3) durationDiscount = 0.05;

        double renewalDiscount = isRenewal ? 0.10 : 0;
        double totalDiscount = durationDiscount + renewalDiscount;

        double total = monthlyPrice * months * (1 - totalDiscount);

        lblPrice.setText("Total: $" + String.format("%.2f", total));

        if (totalDiscount > 0) {
            String discountText = "";
            if (durationDiscount > 0 && renewalDiscount > 0) {
                discountText = String.format("Duration: %.0f%% + Renewal: %.0f%% = %.0f%% total savings!",
                    durationDiscount * 100, renewalDiscount * 100, totalDiscount * 100);
            } else if (durationDiscount > 0) {
                discountText = String.format("Duration discount: %.0f%% off!", durationDiscount * 100);
            } else {
                discountText = "Renewal discount: 10% off!";
            }
            lblDiscount.setText(discountText);
            lblDiscount.setVisible(true);
            lblDiscount.setManaged(true);
        }
    }

    private int getSelectedMonths() {
        int idx = cbMonths.getSelectionModel().getSelectedIndex();
        return switch (idx) {
            case 1 -> 3;
            case 2 -> 6;
            case 3 -> 12;
            default -> 1;
        };
    }

    @FXML
    private void onConfirm() {
        btnConfirm.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        lblStatus.setText("Processing payment...");
        lblStatus.setTextFill(javafx.scene.paint.Color.web("#bdc3c7"));

        new Thread(() -> {
            try {
                // Simulate processing delay
                Thread.sleep(2000);

                int userId = GCMClient.getInstance().getCurrentUser().getId();
                int months = "SUBSCRIPTION".equals(purchaseType) ? getSelectedMonths() : 1;

                ArrayList<Object> data = new ArrayList<>();
                data.add(userId);
                data.add("SUBSCRIPTION".equals(purchaseType) ? cityId : null);
                data.add("ONE_TIME".equals(purchaseType) ? mapId : null);
                data.add(purchaseType);
                data.add("card_token");
                data.add(months);

                Message request = new Message(ActionType.PURCHASE_REQUEST, data);
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

                boolean success = response != null
                    && response.getAction() == ActionType.PURCHASE_RESPONSE
                    && Boolean.TRUE.equals(response.getMessage());

                // Extract error reason if present
                String errorReason = null;
                if (!success && response != null && response.getMessage() instanceof String) {
                    errorReason = (String) response.getMessage();
                }
                final String displayError = errorReason;

                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    progressBar.setManaged(false);

                    if (success) {
                        purchaseComplete = true;
                        lblStatus.setText("Purchase successful!");
                        lblStatus.setTextFill(javafx.scene.paint.Color.web("#2ecc71"));
                        btnConfirm.setVisible(false);
                        btnConfirm.setManaged(false);
                        btnCancel.setText("Close");
                    } else {
                        String msg = displayError != null
                            ? "Purchase failed: " + displayError
                            : "Purchase failed. Please try again.";
                        lblStatus.setText(msg);
                        lblStatus.setTextFill(javafx.scene.paint.Color.web("#e74c3c"));
                        btnConfirm.setDisable(false);
                        btnConfirm.setText("Retry");

                        // Show Update Payment button
                        if (btnUpdatePayment != null) {
                            btnUpdatePayment.setVisible(true);
                            btnUpdatePayment.setManaged(true);
                        }
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    progressBar.setManaged(false);
                    lblStatus.setText("Error: " + e.getMessage());
                    lblStatus.setTextFill(javafx.scene.paint.Color.web("#e74c3c"));
                    btnConfirm.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void onUpdatePayment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/PaymentUpdateDialog.fxml"));
            Parent root = loader.load();

            PaymentUpdateDialogController controller = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Update Payment Details");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            if (controller.isSaved()) {
                lblStatus.setText("Payment updated! Click Retry to continue.");
                lblStatus.setTextFill(javafx.scene.paint.Color.web("#2ecc71"));
                btnUpdatePayment.setVisible(false);
                btnUpdatePayment.setManaged(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onCancel() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    public boolean isPurchaseComplete() {
        return purchaseComplete;
    }
}
