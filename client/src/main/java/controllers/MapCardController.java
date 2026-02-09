package controllers;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import common.content.GCMMap;
import common.content.Site;
import common.content.Tour;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.util.List;

public class MapCardController {
    @FXML private ImageView imgMap;
    @FXML private Label lblMapName;
    @FXML private Label lblMapPrice;
    @FXML private Label lblVersion;
    private GCMMap currentMap;

     public void setData(GCMMap gcmMap) {
         this.currentMap = gcmMap;
         lblMapName.setText(gcmMap.getName());
         lblMapPrice.setText(String.format("Price: $%.2f", gcmMap.getPrice()));
         if (lblVersion != null) {
             lblVersion.setText("Version: " + gcmMap.getVersion());
         }
         // Logic for loading map image from Client resources
         if (gcmMap.getImagePath() != null) {
             try {
                 String path = "/images/maps/" + gcmMap.getImagePath();
                 imgMap.setImage(new Image(getClass().getResourceAsStream(path)));
             } catch (Exception e) {
                 System.err.println("Map image not found: " + gcmMap.getImagePath());
             }
         }
     }

    @FXML
    private void onMapClicked() {
        if (currentMap == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapContentPopup.fxml"));
            Parent root = loader.load();

            MapContentPopupController controller = loader.getController();
            controller.setMapData(currentMap);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Map Content - " + currentMap.getName());
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            System.err.println("Failed to open MapContentPopup: " + e.getMessage());
            e.printStackTrace();
        }
    }
}