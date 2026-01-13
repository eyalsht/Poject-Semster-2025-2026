package controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Example utility class updated to work with the new PurchaseDialogController.
 */
public class PurchaseDialogExample {

    /**
     * Example: Show purchase dialog for a ONE_TIME purchase.
     */
    public static void showOneTimePurchaseDialog(Stage ownerStage) {
        // מזהי דמה לצורך בדיקה (במערכת האמיתית הם יגיעו מהמשתמש המחובר)
        int dummyUserId = 1;
        int dummyCityId = 10;
        Integer dummyMapId = null; // רכישת אוסף (לא מפה בודדת)

        showPurchaseDialog(ownerStage, dummyUserId, dummyCityId, dummyMapId, "Haifa Map Collection", "ONE_TIME", 150.00);
    }

    /**
     * Example: Show purchase dialog for a SUBSCRIPTION purchase.
     */
    public static void showSubscriptionPurchaseDialog(Stage ownerStage) {
        int dummyUserId = 1;
        int dummyCityId = 10;
        Integer dummyMapId = null;

        showPurchaseDialog(ownerStage, dummyUserId, dummyCityId, dummyMapId, "Subscription for Haifa", "SUBSCRIPTION", 100.00);
    }

    /**
     * Generic method to show the purchase dialog.
     */
    public static void showPurchaseDialog(Stage ownerStage, int userId, int cityId, Integer mapId, String itemName, String purchaseType, double price) {
        try {
            // Load the FXML
            FXMLLoader loader = new FXMLLoader(
                    PurchaseDialogExample.class.getResource("/GUI/PurchaseDialog.fxml")
            );
            Parent root = loader.load();

            // Get the controller
            PurchaseDialogController controller = loader.getController();

            // Create the dialog stage
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Secure Purchase");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(ownerStage);
            dialogStage.setScene(new Scene(root));

            // Pass the Stage to the controller (so it can close itself)
            controller.setDialogStage(dialogStage);

            // FIX: Pass all required IDs to initData
            controller.initData(userId, cityId, mapId, itemName, purchaseType, price);

            // Show the dialog
            dialogStage.showAndWait();

            // NOTE: We no longer check 'isPaymentConfirmed' here.
            // The controller itself now sends the request to the Server inside 'handlePay'.

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load PurchaseDialog.fxml: " + e.getMessage());
        }
    }
}