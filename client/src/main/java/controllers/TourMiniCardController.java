package controllers;

import client.GCMClient;
import common.content.Site;
import common.content.Tour;
import common.user.Employee;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class TourMiniCardController {
    @FXML private VBox cardRoot;
    @FXML private Label lblTourName;
    @FXML private Label lblDuration;
    @FXML private Label lblDescription;
    @FXML private VBox detailsPane;
    @FXML private VBox vboxTourSites;
    @FXML private Label lblArrow;

    private static final double COLLAPSED_WIDTH = 340;
    private static final double EXPANDED_WIDTH = 700;

    private Tour currentTour;
    private boolean hasAccess = false;

    public void setAccess(boolean hasAccess) {
        this.hasAccess = hasAccess;
    }

    public void setTourData(Tour tour) {
        this.currentTour = tour;
        if (tour != null) {
            lblTourName.setText(tour.getName());
            lblDuration.setText("Duration: " + tour.getRecommendedDuration());
            lblDescription.setText(tour.getDescription());
        }
    }

    @FXML
    private void toggleDetails() {

        boolean isEmployee = GCMClient.getInstance().getCurrentUser() instanceof Employee;
        boolean canView = isEmployee || hasAccess;

        if (canView && detailsPane != null) {
            boolean isExpanded = !detailsPane.isVisible();
            detailsPane.setVisible(isExpanded);
            detailsPane.setManaged(isExpanded);

            lblArrow.setText(isExpanded ? "▼" : "▶");

            // Resize card: expanded takes full row, collapsed is compact
            if (isExpanded) {
                cardRoot.setPrefWidth(EXPANDED_WIDTH);
                cardRoot.setMaxWidth(EXPANDED_WIDTH);
                lblDescription.setMaxWidth(EXPANDED_WIDTH - 30);
            } else {
                cardRoot.setPrefWidth(COLLAPSED_WIDTH);
                cardRoot.setMaxWidth(COLLAPSED_WIDTH);
                lblDescription.setMaxWidth(COLLAPSED_WIDTH - 30);
            }

            if (isExpanded && vboxTourSites.getChildren().isEmpty()) {
                populateSites();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Subscription Required");
            alert.setHeaderText(null);
            alert.setContentText("Subscribe to this city to get full access to city tours and their details.");
            alert.showAndWait();
        }
    }

    private void populateSites() {
        if (currentTour != null && currentTour.getSites() != null) {
            vboxTourSites.getChildren().clear();
            for (Site site : currentTour.getSites()) {
                Label siteLbl = new Label("• " + site.getName());
                siteLbl.setTextFill(Color.WHITE);
                vboxTourSites.getChildren().add(siteLbl);
            }
        }
    }
}
