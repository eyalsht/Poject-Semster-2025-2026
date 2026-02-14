package controllers;

import common.content.Site;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SiteMiniCardController {
    @FXML private Label lblNumber;
    @FXML private Label lblSiteName;
    @FXML private Label lblCategory;
    @FXML private Label lblDescription;
    @FXML private Label lblLocation;
    @FXML private Label lblDuration;
    @FXML private Label lblAccessible;
    @FXML private Label lblArrow;
    @FXML private VBox detailsPane;

    public void setSiteData(Site site) {
        setSiteData(site, -1);
    }

    public void setSiteData(Site site, int number) {
        if (site != null) {
            if (number > 0 && lblNumber != null) {
                lblNumber.setText(String.valueOf(number));
            } else if (lblNumber != null) {
                lblNumber.setVisible(false);
                lblNumber.setManaged(false);
            }
            lblSiteName.setText(site.getName());
            lblCategory.setText(site.getCategory() != null ? site.getCategory().toString() : "");
            lblDescription.setText(site.getDescription() != null ? site.getDescription() : "");
            lblLocation.setText(site.getLocation() != null && !site.getLocation().isEmpty() ? site.getLocation() : "-");
            lblDuration.setText(site.getRecommendedVisitDuration() != null ? site.getRecommendedVisitDuration().toString() : "-");
            lblAccessible.setText(site.isAccessible() ? "Yes" : "No");
        }
    }

    @FXML
    private void toggleDetails() {
        if (detailsPane != null) {
            boolean isExpanded = !detailsPane.isVisible();
            detailsPane.setVisible(isExpanded);
            detailsPane.setManaged(isExpanded);
            lblArrow.setText(isExpanded ? "\u25BC" : "\u25B6");
        }
    }
}