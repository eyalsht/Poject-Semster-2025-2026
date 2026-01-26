package controllers;

import common.content.City;
import common.content.GCMMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.TilePane;
import javafx.scene.control.Label;
import java.io.IOException;

public class CityMapsPageController {

    @FXML private Label lblCityTitle;
    private City selectedCity;
    @FXML private FlowPane flowPaneMaps;

    public void setCity(City city) {
        this.selectedCity = city;
        lblCityTitle.setText("Explore Maps in " + city.getName());
        loadMaps();
    }

    private void loadMaps() {
        flowPaneMaps.getChildren().clear();

        if (selectedCity.getMaps() == null || selectedCity.getMaps().isEmpty()) {
            lblCityTitle.setText("No maps available yet for " + selectedCity.getName());
            return;
        }
        for (GCMMap map : selectedCity.getMaps()) {
            try {
                //  Load MapCard)
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapCard.fxml"));
                Parent card = loader.load();

                MapCardController cardController = loader.getController();
                cardController.setData(map);

                // Add card
                flowPaneMaps.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}