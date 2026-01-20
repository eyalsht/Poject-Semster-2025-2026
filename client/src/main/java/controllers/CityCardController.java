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
    @FXML
    private ImageView imgCity;
    @FXML private Label lblCityName;
    @FXML private Label lblPrice;

    public void setData(GCMMap mapData) {

        City city = mapData.getCity();
        if (city != null) {
            // UPDATED: Now using the local 'city' variable to get the name
            lblCityName.setText(city.getName());
            lblPrice.setText(String.format("Sub: $%.2f", city.getPriceSub()));
            imgCity.setImage(new Image(new ByteArrayInputStream(mapData.getImage())));
        }
        if (mapData.getImage() != null) {
            imgCity.setImage(new Image(new ByteArrayInputStream(mapData.getImage())));
        }
    }
}