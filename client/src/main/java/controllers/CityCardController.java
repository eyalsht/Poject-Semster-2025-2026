package controllers;

import common.content.GCMMap;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.ByteArrayInputStream;
import common.content.City;
import common.content.GCMMap;
import java.io.ByteArrayInputStream;

public class CityCardController {
    @FXML private ImageView imgCity;
    @FXML private Label lblCityName;
    @FXML private Label lblPrice;

    private City city;
    private CatalogPageController mainController;

    public void setData(GCMMap mapData, CatalogPageController mainController) {
        this.mainController = mainController;
        this.city = mapData.getCity();

        if (city != null) {
            // UPDATED: Now using the local 'city' variable to get the name
            lblCityName.setText(city.getName());
            lblPrice.setText(String.format("Sub: $%.2f", city.getPriceSub()));
            System.out.println("Checking image for city: " + city.getName() +
                    " | Path in DB: [" + city.getImagePath() + "]");
            if (city.getImagePath() != null && !city.getImagePath().isEmpty()) {
                try {
                    // Generates the path: /images/paris.png
                    String path = "/images/cities/" + city.getImagePath() +".png/";
                    System.out.println("path: " + path);
                    Image img = new Image(getClass().getResourceAsStream(path));
                    imgCity.setImage(img);
                } catch (Exception e) {
                    System.err.println("Could not find image: " + city.getImagePath());
                }
            }
        }
    }

    @FXML
    private void onCardClicked() {
        if (mainController != null && city != null) {
            mainController.showCityMaps(city);
        }
    }
}