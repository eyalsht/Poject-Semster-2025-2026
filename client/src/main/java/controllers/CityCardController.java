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
    private Object mainController;

    public void setData(GCMMap mapData, Object controller) {
        //this.mainController = mainController;
        this.mainController = controller;
        this.city = mapData.getCity();
        if (controller == null) {
            System.err.println("DEBUG: setData received a NULL controller!");
        } else {
            System.out.println("DEBUG: setData received controller of type: " + controller.getClass().getName());
        }
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
        if (city == null) {
            System.err.println("DEBUG: Clicked on a card with NULL City"); //
            return;
        }
        if (mainController == null) {
            System.err.println("DEBUG: Clicked on card [" + city.getName() + "] but mainController is NULL"); //
            return;
        }

        if (mainController instanceof CatalogPageController catalog) {
            System.out.println("DEBUG: Navigating to city maps for: " + city.getName()); //
            catalog.showCityMaps(city);
        }
        else if (mainController instanceof CityMapsPageController) {
            System.out.println("DEBUG: Already inside " + city.getName()); //
        }
       /* if (mainController != null && city != null) {
            System.out.println("Error: City or MainController is not initialized.");
            return;
        }
        if (mainController instanceof CatalogPageController catalog) {

            catalog.showCityMaps(city);
        } else if (mainController instanceof CityMapsPageController mapsPage) {
            System.out.println("Already in CityMapsPage for: " + city.getName());
        }*/
    }
}