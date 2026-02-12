package controllers;

import common.content.GCMMap;
import common.content.Site;
import common.content.Tour;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class MapContentPopupController {
    @FXML private Label lblMapName;
    // @FXML private ListView<String> lvSites;
 //   @FXML private ListView<String> lvTours;
    @FXML
    private VBox vboxSites; // ודאי שזה תואם ל-fx:id ב-FXML

    @FXML
    private VBox vboxTours;
    private boolean isPurchased;

    public void setMapData(GCMMap currentMap) {
        if (currentMap == null) return;
        this.isPurchased = isPurchased;

        if (lblMapName != null) {
            lblMapName.setText(currentMap.getName());
        }

    try {
        displaySites(currentMap.getSites());
        displayTours(currentMap.getAvailableTours());
    }  catch (Exception e) {
            if (vboxSites != null) {
                vboxSites.getChildren().clear();
                vboxSites.getChildren().add(new Label("Content loading error (Lazy Loading)"));
            }
        e.printStackTrace();
        }
    }
    private void displayTours(List<Tour> tours) {
        vboxTours.getChildren().clear();
        if (tours == null || tours.isEmpty()) {
            vboxTours.getChildren().add(new Label("No full tours available on this map."));
            return;
        }

        for (Tour tour : tours) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/TourMiniCard.fxml"));
                Node card = loader.load();

                TourMiniCardController controller = loader.getController();
                controller.setTourData(tour);
                controller.setAccess(this.isPurchased);
                vboxTours.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void displaySites(List<Site> sites) {
        vboxSites.getChildren().clear();
        if (sites == null || sites.isEmpty()) {
            vboxSites.getChildren().add(new Label("No sites found on this map."));
            return;
        }

        for (Site site : sites) {
            try {

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/SiteMiniCard.fxml"));
                Node card = loader.load();
                SiteMiniCardController controller = loader.getController();
                controller.setSiteData(site);
                vboxSites.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onClose() {
        ((Stage) lblMapName.getScene().getWindow()).close();
    }
}