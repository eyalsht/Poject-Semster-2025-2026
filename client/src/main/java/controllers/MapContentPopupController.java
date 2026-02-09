package controllers;

import common.content.GCMMap;
import common.content.Site;
import common.content.Tour;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.util.List;

public class MapContentPopupController {
    @FXML private Label lblMapName;
    @FXML private ListView<String> lvSites;
    @FXML private ListView<String> lvTours;

    public void setMapData(GCMMap currentMap) {
        if (currentMap == null) return;
        lblMapName.setText("Detailed Information for " + currentMap.getName());
try {
    // הוספת אתרים
    List<Site> sites = currentMap.getSites();
    if (sites != null && !sites.isEmpty()) {
        for (Site site : currentMap.getSites()) {
            lvSites.getItems().add(site.getName() + " (" + site.getCategory() + ")");
        }
    } else {
        lvSites.getItems().add("No sites listed.");
    }

    // הוספת סיורים
    if (currentMap.getAvailableTours() != null && !currentMap.getAvailableTours().isEmpty()) {
        for (Tour tour : currentMap.getAvailableTours()) {
            lvTours.getItems().add(tour.getName());
        }
    } else {
        lvTours.getItems().add("No full tours available on this specific map.");
    }
    }catch (Exception e) {
        // אם השרת עדיין שולח LAZY, נציג הודעה במקום לקרוס
        lvSites.getItems().add("Content loading error (Lazy Loading)");
        }
    }

    @FXML
    private void onClose() {
        ((Stage) lblMapName.getScene().getWindow()).close();
    }
}