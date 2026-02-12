package controllers;

import client.GCMClient;
import common.content.GCMMap;
import common.dto.MapPurchaseStatusDTO;
import common.enums.ActionType;
import common.messaging.Message;
import common.user.Client;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;

public class MapCardController {
    @FXML private Label lblDescription;
    @FXML private Label lblMapName;
    @FXML private Label lblMapPrice;
    @FXML private Label lblVersion;
    @FXML private Button btnBuy;
    private GCMMap currentMap;
    private MapPurchaseStatusDTO purchaseStatus;

     public void setData(GCMMap gcmMap) {
         this.currentMap = gcmMap;
         lblMapName.setText(gcmMap.getName());
         lblMapPrice.setText(String.format("Price: $%.2f", gcmMap.getPrice()));
         if (lblVersion != null) {
             lblVersion.setText("Version: " + gcmMap.getVersion());
         }
         if (lblDescription != null) {
             lblDescription.setText(gcmMap.getDescription());
         }
         // Check purchase status for clients
         if (btnBuy != null && GCMClient.getInstance().getCurrentUser() instanceof Client) {
             checkMapPurchaseStatus(gcmMap);
         }
         // Logic for loading map image from Client resources
       /*  if (gcmMap.getImagePath() != null) {
             try {
                 String path = "/images/maps/" + gcmMap.getImagePath();
                 imgMap.setImage(new Image(getClass().getResourceAsStream(path)));
             } catch (Exception e) {
                 System.err.println("Map image not found: " + gcmMap.getImagePath());
             }
         }*/
     }

    private void checkMapPurchaseStatus(GCMMap map) {
        int userId = GCMClient.getInstance().getCurrentUser().getId();

        new Thread(() -> {
            try {
                ArrayList<Integer> params = new ArrayList<>();
                params.add(userId);
                params.add(map.getId());

                Message request = new Message(ActionType.CHECK_MAP_PURCHASE_STATUS_REQUEST, params);
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

                if (response != null && response.getAction() == ActionType.CHECK_MAP_PURCHASE_STATUS_RESPONSE) {
                    MapPurchaseStatusDTO status = (MapPurchaseStatusDTO) response.getMessage();
                    this.purchaseStatus = status;

                    Platform.runLater(() -> {
                        if (status.isPurchased()) {
                            btnBuy.setText("Owned");
                            btnBuy.setDisable(true);
                            btnBuy.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-opacity: 0.7;");
                        } else if (status.isOlderVersionPurchased()) {
                            btnBuy.setText(String.format("Upgrade $%.2f", status.getUpgradePrice()));
                            btnBuy.setDisable(false);
                        } else {
                            btnBuy.setText(String.format("Buy $%.2f", status.getFullPrice()));
                            btnBuy.setDisable(false);
                        }
                        btnBuy.setVisible(true);
                        btnBuy.setManaged(true);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void onBuy() {
        if (currentMap == null || purchaseStatus == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/PurchaseConfirmationDialog.fxml"));
            Parent root = loader.load();

            PurchaseConfirmationDialogController controller = loader.getController();

            int cityId = currentMap.getCity() != null ? currentMap.getCity().getId() : 0;

            if (purchaseStatus.isOlderVersionPurchased()) {
                controller.setMapPurchaseData(
                    currentMap.getName(), currentMap.getId(), cityId,
                    purchaseStatus.getUpgradePrice(),
                    true, purchaseStatus.getPurchasedVersion(), currentMap.getVersion()
                );
            } else {
                controller.setMapPurchaseData(
                    currentMap.getName(), currentMap.getId(), cityId,
                    purchaseStatus.getFullPrice(),
                    false, null, currentMap.getVersion()
                );
            }

            Stage stage = new Stage();
            stage.setTitle("Purchase - " + currentMap.getName());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Refresh status after dialog closes
            if (controller.isPurchaseComplete()) {
                checkMapPurchaseStatus(currentMap);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onMapClicked() {
        if (currentMap == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapContentPopup.fxml"));
            Parent root = loader.load();

            MapContentPopupController controller = loader.getController();

            if (controller != null) {
                controller.setMapData(this.currentMap);
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Map Content - " + currentMap.getName());
                stage.setScene(new Scene(root));
                stage.show();
            } else {
                System.err.println("Error: MapContentPopupController not found! Check fx:controller in FXML.");
            }
        } catch (IOException e) {
            System.err.println("Failed to open MapContentPopup: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
