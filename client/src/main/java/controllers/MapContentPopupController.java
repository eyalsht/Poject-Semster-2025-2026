package controllers;

import common.content.GCMMap;
import common.content.Site;
import common.content.SiteMarker;
import common.content.Tour;
import common.enums.MapAccessLevel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class MapContentPopupController {
    @FXML private Label lblMapName;
    @FXML private VBox vboxSites;
    @FXML private VBox vboxTours;
    @FXML private TabPane tabPane;
    @FXML private Tab tabTours;
    @FXML private Button btnBuyInPopup;
    @FXML private ImageView imgMapView;
    @FXML private Pane markerOverlay;
    @FXML private StackPane imageContainer;
    @FXML private VBox imagePlaceholder;
    @FXML private Label lblPlaceholderTitle;
    @FXML private Label lblPlaceholderMessage;

    private MapAccessLevel accessLevel = MapAccessLevel.NO_ACCESS;
    private GCMMap currentMap;

    public void setMapData(GCMMap currentMap, MapAccessLevel accessLevel) {
        if (currentMap == null) return;
        this.currentMap = currentMap;
        this.accessLevel = accessLevel;

        if (lblMapName != null) {
            lblMapName.setText(currentMap.getName());
        }

        applyAccessLevel();

        try {
            displaySites(currentMap.getSites());
            if (accessLevel == MapAccessLevel.FULL_ACCESS) {
                displayTours(currentMap.getAvailableTours());
            }
        } catch (Exception e) {
            if (vboxSites != null) {
                vboxSites.getChildren().clear();
                vboxSites.getChildren().add(new Label("Content loading error (Lazy Loading)"));
            }
            e.printStackTrace();
        }
    }

    /**
     * Backward-compatible overload (used if called without access level).
     */
    public void setMapData(GCMMap currentMap) {
        setMapData(currentMap, MapAccessLevel.NO_ACCESS);
    }

    private void applyAccessLevel() {
        switch (accessLevel) {
            case FULL_ACCESS:
                // Show everything: image, sites, tours, markers
                showMapImage();
                renderMarkers();
                showToursTab(true);
                showBuyButton(false);
                break;

            case MAP_PURCHASED:
                // Show image + sites + markers, but NO tours tab
                showMapImage();
                renderMarkers();
                showToursTab(false);
                showBuyButton(false);
                break;

            case NO_ACCESS:
            default:
                // Show site list only, placeholder instead of image, buy button visible
                showImagePlaceholder("Map Preview", "Purchase this map to view the full image.");
                showToursTab(false);
                showBuyButton(true);
                break;
        }
    }

    private void showMapImage() {
        if (currentMap == null || imgMapView == null) return;

        byte[] imageData = currentMap.getMapImage();
        if (imageData != null && imageData.length > 0) {
            Image image = new Image(new ByteArrayInputStream(imageData));
            imgMapView.setImage(image);
            imgMapView.setVisible(true);
        } else {
            // Has access but no image uploaded yet
            showImagePlaceholder("No Image Available", "A map image has not been uploaded yet.");
        }
    }

    private void showImagePlaceholder(String title, String message) {
        if (imgMapView != null) {
            imgMapView.setVisible(false);
        }
        if (imagePlaceholder != null) {
            imagePlaceholder.setVisible(true);
            imagePlaceholder.setManaged(true);
        }
        if (lblPlaceholderTitle != null) {
            lblPlaceholderTitle.setText(title);
        }
        if (lblPlaceholderMessage != null) {
            lblPlaceholderMessage.setText(message);
        }
    }

    private void showToursTab(boolean show) {
        if (tabPane == null || tabTours == null) return;
        if (!show) {
            tabPane.getTabs().remove(tabTours);
        }
    }

    private void showBuyButton(boolean show) {
        if (btnBuyInPopup != null) {
            btnBuyInPopup.setVisible(show);
            btnBuyInPopup.setManaged(show);
        }
    }

    private void renderMarkers() {
        if (currentMap == null || markerOverlay == null || imgMapView == null) return;

        List<SiteMarker> markers = currentMap.getSiteMarkers();
        if (markers == null || markers.isEmpty()) return;

        List<Site> sites = currentMap.getSites();
        if (sites == null) return;

        markerOverlay.getChildren().clear();

        for (SiteMarker marker : markers) {
            int index = findSiteIndex(sites, marker.getSiteId());
            if (index < 0) continue;

            StackPane pin = createMarkerPin(index + 1);
            // Position at relative coords — bind to image actual dimensions
            pin.layoutXProperty().bind(imgMapView.fitWidthProperty().multiply(marker.getX()).subtract(12));
            pin.layoutYProperty().bind(imgMapView.fitHeightProperty().multiply(marker.getY()).subtract(12));
            markerOverlay.getChildren().add(pin);
        }
    }

    private int findSiteIndex(List<Site> sites, int siteId) {
        for (int i = 0; i < sites.size(); i++) {
            if (sites.get(i).getId() == siteId) return i;
        }
        return -1;
    }

    private StackPane createMarkerPin(int number) {
        Circle circle = new Circle(12);
        circle.setFill(Color.web("#e67e22"));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(2);

        Text text = new Text(String.valueOf(number));
        text.setFill(Color.WHITE);
        text.setFont(Font.font("System", FontWeight.BOLD, 12));

        StackPane pin = new StackPane(circle, text);
        pin.setPrefSize(24, 24);
        return pin;
    }

    private void displayTours(List<Tour> tours) {
        vboxTours.getChildren().clear();
        if (tours == null || tours.isEmpty()) {
            vboxTours.getChildren().add(new Label("No full tours available on this map."));
            return;
        }

        boolean hasAccess = (accessLevel == MapAccessLevel.FULL_ACCESS);

        for (Tour tour : tours) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/TourMiniCard.fxml"));
                Node card = loader.load();

                TourMiniCardController controller = loader.getController();
                controller.setTourData(tour);
                controller.setAccess(hasAccess);
                vboxTours.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void displaySites(List<Site> sites) {
        vboxSites.getChildren().clear();
        if (sites == null || sites.isEmpty()) {
            vboxSites.getChildren().add(new Label("No sites found on this map."));
            return;
        }

        int number = 1;
        for (Site site : sites) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/SiteMiniCard.fxml"));
                Node card = loader.load();
                SiteMiniCardController controller = loader.getController();
                controller.setSiteData(site, number);
                vboxSites.getChildren().add(card);
                number++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onClose() {
        ((Stage) lblMapName.getScene().getWindow()).close();
    }

    @FXML
    public void onBuyClicked() {
        // Delegate to the existing purchase flow — the MapCardController
        // handles the actual purchase logic. This button is a convenience shortcut.
        ((Stage) lblMapName.getScene().getWindow()).close();
    }
}
