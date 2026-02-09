package controllers; // השם חייב להתאים ל-fx:controller ב-FXML

import common.content.Site;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SiteMiniCardController {
    @FXML private Label lblSiteName;
    @FXML private Label lblCategory;
    @FXML private Label lblDescription;

    public void setSiteData(Site site) {
        if (site != null) {
            lblSiteName.setText(site.getName());
            lblCategory.setText(site.getCategory().toString());
            lblDescription.setText(site.getDescription());
        }
    }
}