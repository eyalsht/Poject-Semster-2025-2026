package controllers;

import common.content.GCMMap;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MapCardController {
    @FXML private ImageView imgMap;
    @FXML private Label lblMapName;
    @FXML private Label lblMapPrice;

    // This matches the call: controller.setData(map) in CatalogPageController
    public void setData(GCMMap map) {
        lblMapName.setText(map.getName());
        lblMapPrice.setText(String.format("Price: $%.2f", map.getPrice()));

        // Logic for loading map image from Client resources
        if (map.getImagePath() != null) {
            try {
                String path = "/images/maps/" + map.getImagePath();
                imgMap.setImage(new Image(getClass().getResourceAsStream(path)));
            } catch (Exception e) {
                System.err.println("Map image not found: " + map.getImagePath());
            }
        }
    }

    @FXML
    private void onMapClicked() {
        // Here we will add the logic to open Site/Tour details
    }
}