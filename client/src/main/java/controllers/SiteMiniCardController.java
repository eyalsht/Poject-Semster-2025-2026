package controllers;

import common.content.Site;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SiteMiniCardController {
    @FXML private Label lblNumber;
    @FXML private Label lblSiteName;
    @FXML private Label lblCategory;
    @FXML private Label lblDescription;

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
            lblCategory.setText(site.getCategory().toString());
            lblDescription.setText(site.getDescription());
        }
    }
}
