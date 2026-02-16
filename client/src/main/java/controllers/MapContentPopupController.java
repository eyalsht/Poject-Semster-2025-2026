package controllers;

import common.content.GCMMap;
import common.content.Site;
import common.content.SiteMarker;
import common.content.Tour;
import common.enums.ActionType;
import common.enums.MapAccessLevel;
import common.messaging.Message;
import common.purchase.PurchasedMapSnapshot;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import client.GCMClient;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class MapContentPopupController {
    @FXML private Label lblMapName;
    @FXML private VBox vboxSites;
    @FXML private VBox vboxTours;
    @FXML private TabPane tabPane;
    @FXML private Tab tabTours;
    @FXML private ImageView imgMapView;
    @FXML private Pane markerOverlay;
    @FXML private StackPane imageContainer;
    @FXML private VBox imagePlaceholder;
    @FXML private Label lblPlaceholderTitle;
    @FXML private Label lblPlaceholderMessage;
    @FXML private Button btnDownloadPdf;

    private MapAccessLevel accessLevel = MapAccessLevel.NO_ACCESS;
    private GCMMap currentMap;
    private PurchasedMapSnapshot currentSnapshot;

    public void setMapData(GCMMap currentMap, MapAccessLevel accessLevel) {
        if (currentMap == null) return;
        this.currentMap = currentMap;
        this.accessLevel = accessLevel;

        if (lblMapName != null) {
            lblMapName.setText(currentMap.getName());
        }

        // Fetch full map details from server (includes image, sites, markers)
        new Thread(() -> {
            try {
                Message request = new Message(ActionType.GET_MAP_DETAILS_REQUEST, currentMap.getId());
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getAction() == ActionType.GET_MAP_DETAILS_RESPONSE) {
                        GCMMap fullMap = (GCMMap) response.getMessage();
                        this.currentMap = fullMap;
                    }
                    // Apply access level and display content (with full or partial data)
                    applyAccessLevel();
                    try {
                        displaySites(this.currentMap.getSites());
                        if (this.accessLevel == MapAccessLevel.FULL_ACCESS) {
                            displayTours(this.currentMap.getAvailableTours());
                        }
                    } catch (Exception e) {
                        if (vboxSites != null) {
                            vboxSites.getChildren().clear();
                            vboxSites.getChildren().add(new Label("Content loading error"));
                        }
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    // Fallback: use whatever data we have
                    applyAccessLevel();
                    try {
                        displaySites(this.currentMap.getSites());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Backward-compatible overload (used if called without access level).
     */
    public void setMapData(GCMMap currentMap) {
        setMapData(currentMap, MapAccessLevel.NO_ACCESS);
    }

    /**
     * Display a purchased map snapshot (no server fetch needed — all data is local).
     */
    public void setSnapshotData(PurchasedMapSnapshot snapshot) {
        if (snapshot == null) return;
        this.currentSnapshot = snapshot;
        this.accessLevel = MapAccessLevel.MAP_PURCHASED;

        if (lblMapName != null) {
            lblMapName.setText(snapshot.getMapName());
        }

        // Show map image from snapshot bytes
        byte[] imageData = snapshot.getMapImageData();
        if (imageData != null && imageData.length > 0) {
            Image image = new Image(new ByteArrayInputStream(imageData));
            imgMapView.setImage(image);
            imgMapView.setVisible(true);
        } else {
            showImagePlaceholder("No Image Available", "Map image was not captured at purchase time.");
        }

        // Parse and render markers from snapshot
        List<SiteMarker> markers = snapshot.getSnapshotMarkers();
        List<Site> sites = snapshot.getSnapshotSitesAsSiteObjects();

        if (markers != null && !markers.isEmpty() && sites != null && !sites.isEmpty()
                && imgMapView.getImage() != null) {
            renderMarkersFromData(markers, sites);
        }

        // Display sites
        displaySites(sites);

        // Hide tours tab (purchased maps don't include tours)
        showToursTab(false);

        // Show download PDF button
        if (btnDownloadPdf != null) {
            btnDownloadPdf.setVisible(true);
            btnDownloadPdf.setManaged(true);
        }
    }

    private void applyAccessLevel() {
        switch (accessLevel) {
            case FULL_ACCESS:
                // Show everything: image, sites, tours, markers
                showMapImage();
                renderMarkers();
                showToursTab(true);
                showDownloadButton(true);
                break;

            case MAP_PURCHASED:
                // Show image + sites + markers, but NO tours tab
                showMapImage();
                renderMarkers();
                showToursTab(false);
                showDownloadButton(true);
                break;

            case NO_ACCESS:
            default:
                // Show site list only, placeholder instead of image
                showImagePlaceholder("Map Preview", "Purchase this map or subscribe to the city to view the full image.");
                showToursTab(false);
                showDownloadButton(false);
                break;
        }
    }

    private void showDownloadButton(boolean show) {
        if (btnDownloadPdf != null) {
            btnDownloadPdf.setVisible(show);
            btnDownloadPdf.setManaged(show);
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

    private void renderMarkers() {
        if (currentMap == null || markerOverlay == null || imgMapView == null) return;

        List<SiteMarker> markers = currentMap.getSiteMarkers();
        if (markers == null || markers.isEmpty()) return;

        List<Site> sites = currentMap.getSites();
        if (sites == null) return;

        renderMarkersFromData(markers, sites);
    }

    /**
     * Renders markers on the map overlay from provided data (used by both live maps and snapshots).
     */
    private void renderMarkersFromData(List<SiteMarker> markers, List<Site> sites) {
        if (markerOverlay == null || imgMapView == null) return;

        markerOverlay.getChildren().clear();

        // Constrain overlay to match ImageView size so they overlap perfectly in the StackPane
        markerOverlay.setMaxWidth(imgMapView.getFitWidth());
        markerOverlay.setMaxHeight(imgMapView.getFitHeight());
        markerOverlay.setPrefWidth(imgMapView.getFitWidth());
        markerOverlay.setPrefHeight(imgMapView.getFitHeight());

        Image img = imgMapView.getImage();
        if (img == null) return;

        double imgW = img.getWidth();
        double imgH = img.getHeight();
        double fitW = imgMapView.getFitWidth();
        double fitH = imgMapView.getFitHeight();

        // Account for preserveRatio — actual rendered size may differ from fitWidth/fitHeight
        double scale = Math.min(fitW / imgW, fitH / imgH);
        double renderedW = imgW * scale;
        double renderedH = imgH * scale;
        double offsetX = (fitW - renderedW) / 2.0;
        double offsetY = (fitH - renderedH) / 2.0;

        for (SiteMarker marker : markers) {
            int index = findSiteIndex(sites, marker.getSiteId());
            if (index < 0) continue;

            StackPane pin = createMarkerPin(index + 1);
            double px = offsetX + marker.getX() * renderedW - 12;
            double py = offsetY + marker.getY() * renderedH - 12;
            pin.setLayoutX(px);
            pin.setLayoutY(py);
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

    // ==================== PDF DOWNLOAD ====================

    @FXML
    private void onDownloadPdf() {
        // Resolve metadata from snapshot or live map
        String mapName;
        String cityName;
        String version;
        String description;
        String dateLine;
        List<Site> sites;
        int mapId;

        if (currentSnapshot != null) {
            mapName = currentSnapshot.getMapName();
            cityName = currentSnapshot.getCityName();
            version = currentSnapshot.getPurchasedVersion();
            description = currentSnapshot.getDescription();
            dateLine = "Purchased: " + (currentSnapshot.getPurchaseDate() != null ? currentSnapshot.getPurchaseDate().toString() : "-");
            sites = currentSnapshot.getSnapshotSitesAsSiteObjects();
            mapId = currentSnapshot.getOriginalMapId();
        } else if (currentMap != null) {
            mapName = currentMap.getName();
            cityName = currentMap.getCityName();
            version = currentMap.getVersion();
            description = currentMap.getDescription();
            dateLine = "Downloaded: " + LocalDate.now();
            sites = currentMap.getSites();
            mapId = currentMap.getId();
        } else {
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save Map as PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));
        fc.setInitialFileName(mapName + ".pdf");

        File out = fc.showSaveDialog(lblMapName.getScene().getWindow());
        if (out == null) return;

        try {
            // Capture map image + markers as rendered
            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.web("#1a252f"));
            WritableImage fxImg = imageContainer.snapshot(params, null);
            BufferedImage mapImage = SwingFXUtils.fromFXImage(fxImg, null);

            try (PDDocument doc = new PDDocument()) {
                // Page 1: Map image + metadata
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                float pageW = page.getMediaBox().getWidth();
                float pageH = page.getMediaBox().getHeight();
                float margin = 40;
                float usableW = pageW - 2 * margin;

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    float y = pageH - margin;

                    // Title
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
                    y -= 20;
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    cs.showText(truncateText(mapName, 50));
                    cs.endText();

                    // Metadata line
                    cs.setFont(PDType1Font.HELVETICA, 11);
                    y -= 18;
                    cs.beginText();
                    cs.newLineAtOffset(margin, y);
                    String meta = "City: " + cityName + "  |  Version: " + version + "  |  " + dateLine;
                    cs.showText(truncateText(meta, 100));
                    cs.endText();

                    y -= 10;

                    // Map image
                    if (mapImage != null) {
                        var pdImage = LosslessFactory.createFromImage(doc, mapImage);
                        float imgW = mapImage.getWidth();
                        float imgH = mapImage.getHeight();
                        float scale = Math.min(usableW / imgW, 350f / imgH);
                        float drawW = imgW * scale;
                        float drawH = imgH * scale;
                        float imgX = margin + (usableW - drawW) / 2;
                        y -= drawH + 5;
                        cs.drawImage(pdImage, imgX, y, drawW, drawH);
                        y -= 15;
                    }

                    // Description
                    if (description != null && !description.isEmpty()) {
                        cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
                        y -= 12;
                        String desc = description.replace("\n", " ").replace("\r", "");
                        cs.beginText();
                        cs.newLineAtOffset(margin, y);
                        cs.showText(truncateText(desc, 120));
                        cs.endText();
                        y -= 10;
                    }

                    // Sites header
                    if (sites != null && !sites.isEmpty()) {
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                        y -= 20;
                        cs.beginText();
                        cs.newLineAtOffset(margin, y);
                        cs.showText("Sites (" + sites.size() + ")");
                        cs.endText();
                        y -= 5;

                        // Sites list on this page
                        y = writeSitesToPdf(doc, cs, page, sites, 0, margin, y, usableW);
                    }
                }

                doc.save(out);
            }

            // Log download event
            logMapDownload(mapId);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("PDF Saved");
            alert.setHeaderText(null);
            alert.setContentText("Map PDF saved successfully.");
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to save PDF: " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Writes site entries to the PDF. Handles page overflow by creating new pages.
     * Returns the final y position.
     */
    private float writeSitesToPdf(PDDocument doc, PDPageContentStream cs, PDPage currentPage,
                                   List<Site> sites, int startIndex, float margin, float startY, float usableW) throws IOException {
        float y = startY;
        float bottomMargin = 50;
        PDPageContentStream stream = cs;
        boolean ownsStream = false;

        for (int i = startIndex; i < sites.size(); i++) {
            Site site = sites.get(i);

            // Check if we need a new page (each site entry needs ~50px)
            if (y < bottomMargin + 50) {
                if (ownsStream) stream.close();
                PDPage newPage = new PDPage(PDRectangle.A4);
                doc.addPage(newPage);
                stream = new PDPageContentStream(doc, newPage);
                ownsStream = true;
                y = newPage.getMediaBox().getHeight() - margin;
            }

            // Site number + name
            stream.setFont(PDType1Font.HELVETICA_BOLD, 11);
            y -= 18;
            stream.beginText();
            stream.newLineAtOffset(margin, y);
            String siteHeader = (i + 1) + ". " + truncateText(site.getName() != null ? site.getName() : "Unknown", 60);
            stream.showText(siteHeader);
            stream.endText();

            // Category + location
            stream.setFont(PDType1Font.HELVETICA, 9);
            y -= 13;
            stream.beginText();
            stream.newLineAtOffset(margin + 15, y);
            String catLoc = "Category: " + (site.getCategory() != null ? site.getCategory().toString() : "-")
                    + "  |  Location: " + (site.getLocation() != null && !site.getLocation().isEmpty() ? site.getLocation() : "-");
            stream.showText(truncateText(catLoc, 90));
            stream.endText();

            // Description
            if (site.getDescription() != null && !site.getDescription().isEmpty()) {
                y -= 12;
                stream.beginText();
                stream.newLineAtOffset(margin + 15, y);
                stream.showText(truncateText(site.getDescription().replace("\n", " "), 100));
                stream.endText();
            }

            y -= 5;
        }

        if (ownsStream) stream.close();
        return y;
    }

    private String truncateText(String text, int maxLen) {
        if (text == null) return "";
        // Remove characters that PDType1Font can't encode
        text = text.replaceAll("[^\\x00-\\x7F]", "?");
        if (text.length() > maxLen) return text.substring(0, maxLen - 3) + "...";
        return text;
    }

    private void logMapDownload(int mapId) {
        if (mapId <= 0) return;
        new Thread(() -> {
            try {
                Message request = new Message(ActionType.LOG_MAP_DOWNLOAD_REQUEST, mapId);
                GCMClient.getInstance().sendRequest(request);
            } catch (Exception e) {
                System.err.println("Failed to log map download: " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void onClose() {
        ((Stage) lblMapName.getScene().getWindow()).close();
    }
}
