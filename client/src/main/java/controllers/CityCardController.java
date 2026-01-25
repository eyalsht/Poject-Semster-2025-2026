package controllers;

import common.content.GCMMap;
import common.dto.CatalogResponse;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import common.content.City;

import java.util.List;

public class CityCardController {
    @FXML private ImageView imgCity;
    @FXML private Label lblCityName;
    @FXML private Label lblPrice;

    // Search mode fields
    @FXML private HBox countsBox;
    @FXML private Label lblMapCount;
    @FXML private Label lblSiteCount;
    @FXML private Label lblTourCount;
    @FXML private Label lblMapDesc;

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
                    String path = "/images/cities/" + city.getImagePath() +".png";
                    System.out.println("path: " + path);
                    Image img = new Image(getClass().getResourceAsStream(path));
                    imgCity.setImage(img);
                } catch (Exception e) {
                    System.err.println("Could not find image: " + city.getImagePath());
                }
            }
        }
    }

    /**
     * Set data for search mode - displays city with counts and map descriptions.
     */
    public void setSearchData(CatalogResponse.CitySearchResult result, CatalogPageController mainController) {
        this.mainController = mainController;
        this.city = result.getCity();

        if (city != null) {
            lblCityName.setText(city.getName());
            lblPrice.setText(String.format("Sub: $%.2f", city.getPriceSub()));

            // Load city image
            if (city.getImagePath() != null && !city.getImagePath().isEmpty()) {
                try {
                    String path = "/images/cities/" + city.getImagePath() + ".png";
                    Image img = new Image(getClass().getResourceAsStream(path));
                    imgCity.setImage(img);
                } catch (Exception e) {
                    System.err.println("Could not find image: " + city.getImagePath());
                }
            }

            // Show and populate counts
            countsBox.setVisible(true);
            countsBox.setManaged(true);
            lblMapCount.setText("Maps: " + result.getMapCount());
            lblSiteCount.setText("Sites: " + result.getSiteCount());
            lblTourCount.setText("Tours: " + result.getTourCount());

            // Show map descriptions (truncated)
            List<String> mapDescriptions = result.getMapDescriptions();
            if (mapDescriptions != null && !mapDescriptions.isEmpty()) {
                lblMapDesc.setVisible(true);
                lblMapDesc.setManaged(true);
                String descText = String.join(", ", mapDescriptions);
                // Truncate if too long
                if (descText.length() > 100) {
                    descText = descText.substring(0, 97) + "...";
                }
                lblMapDesc.setText(descText);
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