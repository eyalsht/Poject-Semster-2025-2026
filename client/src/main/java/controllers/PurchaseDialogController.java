package controllers;

import client.GCMClient;
import common.Message;
import common.actionType;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.util.ArrayList;

public class PurchaseDialogController {

    @FXML private Label lblItemName;
    @FXML private Label lblPurchaseType;
    @FXML private Label lblDuration;
    @FXML private ComboBox<String> comboDuration;
    @FXML private Label lblPrice;

    // Data Fields
    private int userId;
    private int cityId;
    private Integer mapId;
    private String purchaseType;
    private double baseMonthlyPrice;
    private int monthsToAdd = 0;

    // UI Fields
    private Stage dialogStage;

    /**
     * Initialize the dialog with purchase data.
     * Updated to include User and City IDs needed for the server request.
     */
    public void initData(int userId, int cityId, Integer mapId, String itemName, String purchaseType, double price) {
        this.userId = userId;
        this.cityId = cityId;
        this.mapId = mapId;
        this.purchaseType = purchaseType;

        // Set item name and purchase type labels
        lblItemName.setText(itemName);
        lblPurchaseType.setText(purchaseType.replace("_", " ")); // "ONE_TIME" -> "ONE TIME"

        if ("ONE_TIME".equals(purchaseType)) {
            // ONE_TIME purchase logic
            lblDuration.setVisible(false);
            lblDuration.setManaged(false);
            comboDuration.setVisible(false);
            comboDuration.setManaged(false);

            // Set fixed price
            lblPrice.setText(String.format("$%.2f", price));
            monthsToAdd = 0;

        } else if ("SUBSCRIPTION".equals(purchaseType)) {
            // SUBSCRIPTION purchase logic
            lblDuration.setVisible(true);
            lblDuration.setManaged(true);
            comboDuration.setVisible(true);
            comboDuration.setManaged(true);

            // Store base monthly price
            baseMonthlyPrice = price;

            // Populate duration options
            comboDuration.getItems().clear();
            comboDuration.getItems().addAll("1 Month", "3 Months", "6 Months");
            comboDuration.setValue("1 Month"); // Default selection

            // Set initial price for 1 month
            monthsToAdd = 1;
            updatePrice();

            // Add listener for duration changes
            comboDuration.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    monthsToAdd = parseDuration(newValue);
                    updatePrice();
                }
            });
        }
    }

    private int parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) return 1;
        try {
            return Integer.parseInt(duration.split(" ")[0]);
        } catch (Exception e) {
            return 1;
        }
    }

    private void updatePrice() {
        double totalPrice = baseMonthlyPrice * monthsToAdd;
        // Optional: Add discount logic here (e.g., if monthsToAdd == 6 -> 10% off)
        lblPrice.setText(String.format("$%.2f", totalPrice));
    }

    @FXML
    private void handlePay() {
        // 1. Generate Mock Token (Simulation)
        String mockCreditCardToken = "VISA-MOCK-TOKEN-9999";

        System.out.println("Processing Payment...");
        System.out.println("User: " + userId + ", City: " + cityId + ", Type: " + purchaseType + ", Months: " + monthsToAdd);

        // 2. Build Request Object for Server
        // Protocol: [userId, cityId, mapId, purchaseType, creditCardToken, monthsToAdd]
        ArrayList<Object> request = new ArrayList<>();
        request.add(userId);
        request.add(cityId);
        request.add(mapId);
        request.add(purchaseType);
        request.add(mockCreditCardToken);
        request.add(monthsToAdd);

        // 3. Send to Server via GCMClient
        Message msg = new Message(actionType.PURCHASE_REQUEST, request);
        try {
            GCMClient.getInstance().sendToServer(msg);
            System.out.println("Purchase request sent successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to send purchase request.");
        }

        // 4. Close the dialog
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        } else {
            // Fallback if stage wasn't set explicitly
            Stage stage = (Stage) lblPrice.getScene().getWindow();
            stage.close();
        }
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
}