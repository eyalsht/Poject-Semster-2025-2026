package controllers;

import common.content.Tour;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class TourMiniCardController {
    @FXML private Label lblTourName;
    @FXML private Label lblDuration;
    @FXML private Label lblDescription;

    public void setTourData(Tour tour) {
        if (tour != null) {
            lblTourName.setText(tour.getName());
            lblDuration.setText("Duration: " + tour.getRecommendedDuration());
            lblDescription.setText(tour.getDescription());
        }
    }
}