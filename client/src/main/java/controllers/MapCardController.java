package controllers;

import common.content.GCMMap;
import common.content.Site;
import common.content.Tour;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.List;

public class MapCardController {
    @FXML private ImageView imgMap;
    @FXML private Label lblMapName;
    @FXML private Label lblMapPrice;
    private GCMMap currentMap;

    public void setData(GCMMap gcmMap) {
        this.currentMap = gcmMap;
        lblMapName.setText(gcmMap.getName());
        lblMapPrice.setText(String.format("Price: $%.2f", gcmMap.getPrice()));

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

        StringBuilder details = new StringBuilder("Detailed Information for " + currentMap.getName() + ":\n\n");

        details.append("📍 Sites in this map:\n");
        if (currentMap.getSites() != null && !currentMap.getSites().isEmpty()) {
            for (Site site : currentMap.getSites()) {
                details.append("- ").append(site.getName()).append(" (").append(site.getCategory()).append(")\n");
            }
        } else {
            details.append("No sites listed.\n");
        }

        details.append("\n🚶 Recommended Tours:\n");
        // Using the logic that filters tours based on available sites
        List<Tour> tours = currentMap.getAvailableTours();
        if (tours != null && !tours.isEmpty()) {
            for (Tour tour : tours) {
                details.append("- ").append(tour.getName()).append("\n");
            }
        } else {
            details.append("No full tours available on this specific map.\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Map Content");
        alert.setHeaderText(currentMap.getCityName() + " - " + currentMap.getName());
        alert.setContentText(details.toString());
        alert.showAndWait();
    }
}