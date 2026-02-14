package controllers;

import client.GCMClient;
import common.content.*;
import common.dto.ContentChangeRequest;
import common.enums.*;
import common.messaging.Message;
import common.user.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class EditModeController {

    private final GCMClient client = GCMClient.getInstance();

    // FXML components
    @FXML private ComboBox<City> cbCitySelector;
    @FXML private TabPane tabPane;
    @FXML private Tab tabCityDetails, tabMaps, tabSites, tabTours;
    @FXML private VBox vboxCityDetails, vboxMapEditor, vboxSiteEditor, vboxTourEditor;
    @FXML private ListView<GCMMap> lvMaps;
    @FXML private ListView<Site> lvSites;
    @FXML private ListView<Tour> lvTours;
    @FXML private Label lblStatus, lblChangeCount;
    @FXML private Button btnSubmitAll, btnClose, btnNewSite, btnNewTour;
    @FXML private HBox statusBar;

    // Data
    private City currentCity;
    private ObservableList<City> allCities = FXCollections.observableArrayList();
    private ObservableList<GCMMap> cityMaps = FXCollections.observableArrayList();
    private ObservableList<Site> citySites = FXCollections.observableArrayList();
    private ObservableList<Tour> cityTours = FXCollections.observableArrayList();

    // Change tracking — snapshot-based: store original state and compare
    private final Map<String, String> originalValues = new HashMap<>();
    private final Map<String, HBox> fieldRows = new HashMap<>();
    // pendingChanges removed — Submit Changes now builds requests from current state

    // Snapshot of original site IDs per map (for dirty detection)
    private final Map<Integer, Set<Integer>> originalMapSiteIds = new HashMap<>();
    // Snapshot of original site IDs per tour
    private final Map<Integer, List<Integer>> originalTourSiteIds = new HashMap<>();
    // Snapshot of original markers per map
    private final Map<Integer, List<SiteMarker>> originalMapMarkers = new HashMap<>();
    // Snapshot of original map image (just whether it was null/present)
    private byte[] originalMapImage;

    // Map editing state
    private byte[] pendingMapImage;
    private List<SiteMarker> pendingMarkers = new ArrayList<>();
    private GCMMap currentEditingMap;
    private Pane markerOverlayPane;
    private ImageView mapImageView;
    private ListView<Site> lvOnMapSites; // reference to On Map list for marker placement

    // Site/Tour editing state
    private Site currentEditingSite;
    private Tour currentEditingTour;
    private boolean isNewSite = false;
    private boolean isNewTour = false;

    // Live observable lists for current map/tour editors (for snapshot comparison)
    private ObservableList<Site> currentMapOnMapSites;
    private ObservableList<Site> currentTourSites;

    @FXML
    public void initialize() {
        // Disable tabs until city is selected
        tabCityDetails.setDisable(true);
        tabMaps.setDisable(true);
        tabSites.setDisable(true);
        tabTours.setDisable(true);

        // City selector setup
        cbCitySelector.setCellFactory(lv -> new ListCell<City>() {
            @Override
            protected void updateItem(City item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        cbCitySelector.setButtonCell(new ListCell<City>() {
            @Override
            protected void updateItem(City item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
                setTextFill(Color.WHITE);
            }
        });
        cbCitySelector.setOnAction(e -> onCitySelected());

        // ListView cell factories
        lvMaps.setCellFactory(lv -> new ListCell<GCMMap>() {
            @Override
            protected void updateItem(GCMMap item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " v" + item.getVersion());
            }
        });
        lvMaps.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) populateMapEditor(newVal);
        });

        lvSites.setCellFactory(lv -> new ListCell<Site>() {
            @Override
            protected void updateItem(Site item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        lvSites.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                isNewSite = false;
                populateSiteEditor(newVal);
            }
        });

        lvTours.setCellFactory(lv -> new ListCell<Tour>() {
            @Override
            protected void updateItem(Tour item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        lvTours.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                isNewTour = false;
                populateTourEditor(newVal);
            }
        });

        loadCities();
    }

    /**
     * Pre-select a city (called when opened from CityMapsPage).
     */
    public void preSelectCity(City city) {
        if (city == null) return;
        Platform.runLater(() -> {
            for (City c : allCities) {
                if (c.getId() == city.getId()) {
                    cbCitySelector.getSelectionModel().select(c);
                    return;
                }
            }
        });
    }

    // ==================== DATA LOADING ====================

    private void loadCities() {
        new Thread(() -> {
            try {
                Message request = new Message(ActionType.GET_CITIES_REQUEST, null);
                Message response = (Message) client.sendRequest(request);
                if (response != null && response.getAction() == ActionType.GET_CITIES_RESPONSE) {
                    @SuppressWarnings("unchecked")
                    List<City> cities = (List<City>) response.getMessage();
                    Platform.runLater(() -> {
                        allCities.setAll(cities);
                        cbCitySelector.setItems(allCities);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void onCitySelected() {
        City selected = cbCitySelector.getValue();
        if (selected == null) return;

        lblStatus.setText("Loading: " + selected.getName() + "...");

        new Thread(() -> {
            try {
                Message request = new Message(ActionType.GET_CITY_FULL_DETAILS_REQUEST, selected.getId());
                Message response = (Message) client.sendRequest(request);
                if (response != null && response.getAction() == ActionType.GET_CITY_FULL_DETAILS_RESPONSE) {
                    City fullCity = (City) response.getMessage();
                    Platform.runLater(() -> loadCityData(fullCity));
                } else {
                    Platform.runLater(() -> lblStatus.setText("Failed to load city data."));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblStatus.setText("Error loading city."));
            }
        }).start();
    }

    private void loadCityData(City city) {
        this.currentCity = city;
        resetChangeTracking();

        // Enable tabs
        tabCityDetails.setDisable(false);
        tabMaps.setDisable(false);
        tabSites.setDisable(false);
        tabTours.setDisable(false);

        // Populate lists
        cityMaps.setAll(city.getMaps() != null ? city.getMaps() : new ArrayList<>());
        citySites.setAll(city.getSites() != null ? city.getSites() : new ArrayList<>());
        cityTours.setAll(city.getTours() != null ? city.getTours() : new ArrayList<>());

        lvMaps.setItems(cityMaps);
        lvSites.setItems(citySites);
        lvTours.setItems(cityTours);

        // Take snapshots of original state for all maps and tours
        for (GCMMap m : cityMaps) {
            Set<Integer> siteIds = new HashSet<>();
            if (m.getSites() != null) {
                for (Site s : m.getSites()) siteIds.add(s.getId());
            }
            originalMapSiteIds.put(m.getId(), siteIds);
            originalMapMarkers.put(m.getId(), m.getSiteMarkers() != null ? new ArrayList<>(m.getSiteMarkers()) : new ArrayList<>());
        }
        for (Tour t : cityTours) {
            List<Integer> siteIds = new ArrayList<>();
            if (t.getSites() != null) {
                for (Site s : t.getSites()) siteIds.add(s.getId());
            }
            originalTourSiteIds.put(t.getId(), siteIds);
        }

        // Populate city details tab
        populateCityDetailsTab();

        // Reset editors
        vboxMapEditor.getChildren().clear();
        vboxMapEditor.getChildren().add(createPlaceholderLabel("Select a map to edit"));
        vboxSiteEditor.getChildren().clear();
        vboxSiteEditor.getChildren().add(createPlaceholderLabel("Select a site to edit"));
        vboxTourEditor.getChildren().clear();
        vboxTourEditor.getChildren().add(createPlaceholderLabel("Select a tour to edit"));

        lblStatus.setText("Editing: " + city.getName());
    }

    // ==================== TAB 1: CITY DETAILS ====================

    private void populateCityDetailsTab() {
        vboxCityDetails.getChildren().clear();

        Label title = new Label("City Details");
        title.getStyleClass().add("section-title");
        vboxCityDetails.getChildren().add(title);

        HBox nameRow = createEditableFieldRow("City Name", currentCity.getName(), "cityName");
        vboxCityDetails.getChildren().add(nameRow);

        HBox descRow = createEditableTextAreaRow("Description", currentCity.getDescription(), "cityDesc");
        vboxCityDetails.getChildren().add(descRow);

        // No individual submit — use "Submit Changes" in the status bar
    }

    // ==================== TAB 2: MAPS ====================

    private void populateMapEditor(GCMMap map) {
        currentEditingMap = map;
        pendingMapImage = map.getMapImage();
        originalMapImage = map.getMapImage();
        pendingMarkers = map.getSiteMarkers() != null ? new ArrayList<>(map.getSiteMarkers()) : new ArrayList<>();

        vboxMapEditor.getChildren().clear();

        Label title = new Label("Editing: " + map.getName());
        title.getStyleClass().add("section-title");
        vboxMapEditor.getChildren().add(title);

        // Map Name
        HBox nameRow = createEditableFieldRow("Map Name", map.getName(), "mapName_" + map.getId());
        vboxMapEditor.getChildren().add(nameRow);

        // Map Description
        HBox descRow = createEditableTextAreaRow("Description", map.getDescription(), "mapDesc_" + map.getId());
        vboxMapEditor.getChildren().add(descRow);

        // Map Image Section
        vboxMapEditor.getChildren().add(createMapImageSection(map));

        // Map-Site assignment section
        vboxMapEditor.getChildren().add(createMapSiteAssignmentSection(map));

        // Delete button only — submit via "Submit Changes" in status bar
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        Button btnDeleteMap = new Button("Delete Map");
        btnDeleteMap.getStyleClass().add("delete-button");
        btnDeleteMap.setDisable(true);
        btnDeleteMap.setTooltip(new Tooltip("Coming soon - requires transfer destination"));

        btnBox.getChildren().add(btnDeleteMap);
        vboxMapEditor.getChildren().add(btnBox);
    }

    private VBox createMapImageSection(GCMMap map) {
        VBox section = new VBox(8);
        section.getStyleClass().add("field-section");
        section.setPadding(new Insets(10));

        Label lbl = new Label("Map Image");
        lbl.getStyleClass().add("field-label");

        StackPane imageContainer = new StackPane();
        imageContainer.setMinHeight(300);
        imageContainer.setMaxHeight(400);
        imageContainer.getStyleClass().add("image-container");

        mapImageView = new ImageView();
        mapImageView.setFitWidth(500);
        mapImageView.setFitHeight(350);
        mapImageView.setPreserveRatio(true);

        markerOverlayPane = new Pane();
        markerOverlayPane.setPickOnBounds(true);
        markerOverlayPane.prefWidthProperty().bind(mapImageView.fitWidthProperty());
        markerOverlayPane.prefHeightProperty().bind(mapImageView.fitHeightProperty());
        markerOverlayPane.maxWidthProperty().bind(mapImageView.fitWidthProperty());
        markerOverlayPane.maxHeightProperty().bind(mapImageView.fitHeightProperty());

        if (pendingMapImage != null && pendingMapImage.length > 0) {
            Image img = new Image(new ByteArrayInputStream(pendingMapImage));
            mapImageView.setImage(img);
        }

        // Instruction label
        Label lblInstruction = new Label("Select a site from 'On Map' list, then click to place/move its marker");
        lblInstruction.setId("markerInstruction");
        lblInstruction.getStyleClass().add("marker-instruction");

        imageContainer.getChildren().addAll(mapImageView, markerOverlayPane);

        // Click handler on the overlay pane (same coordinate space as marker rendering)
        // Overlay is sized to match ImageView fitWidth/fitHeight, so coordinates align exactly
        markerOverlayPane.setOnMouseClicked(event -> {
            if (mapImageView.getImage() == null || lvOnMapSites == null) return;
            Site selected = lvOnMapSites.getSelectionModel().getSelectedItem();
            if (selected == null) {
                lblInstruction.setText("Select a site from 'On Map' list first, then click to place its marker");
                return;
            }

            // Compute relative position — same offset math as renderMarkersOnOverlay
            Image img = mapImageView.getImage();
            double imgW = img.getWidth();
            double imgH = img.getHeight();
            double fitW = mapImageView.getFitWidth();
            double fitH = mapImageView.getFitHeight();
            double scale = Math.min(fitW / imgW, fitH / imgH);
            double renderedW = imgW * scale;
            double renderedH = imgH * scale;
            double offsetX = (fitW - renderedW) / 2.0;
            double offsetY = (fitH - renderedH) / 2.0;

            // event coordinates are relative to markerOverlayPane = same as ImageView fitWidth/fitHeight space
            double relX = (event.getX() - offsetX) / renderedW;
            double relY = (event.getY() - offsetY) / renderedH;

            // Reject clicks outside the actual rendered image area
            if (relX < 0 || relX > 1 || relY < 0 || relY > 1) return;

            // Place or move marker for the selected site
            pendingMarkers.removeIf(m -> m.getSiteId() == selected.getId());
            pendingMarkers.add(new SiteMarker(selected.getId(), relX, relY));

            renderMarkersOnOverlay();
            lblInstruction.setText("Marker placed for: " + selected.getName() + "  \u2014  click again to reposition");
            recalculateChangeCount();
            if (lvOnMapSites != null) lvOnMapSites.refresh();
        });

        Button btnUpload = new Button("Upload Image");
        btnUpload.getStyleClass().add("upload-button");
        btnUpload.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Map Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File file = fileChooser.showOpenDialog(btnUpload.getScene().getWindow());
            if (file != null) {
                try {
                    long fileSize = file.length();
                    if (fileSize > 5 * 1024 * 1024) {
                        showAlert("File Too Large", "Image must be under 5 MB. Selected file is " + String.format("%.1f", fileSize / (1024.0 * 1024.0)) + " MB.");
                        return;
                    }
                    pendingMapImage = Files.readAllBytes(file.toPath());
                    Image img = new Image(new ByteArrayInputStream(pendingMapImage));
                    mapImageView.setImage(img);
                    recalculateChangeCount();
                } catch (Exception ex) {
                    showAlert("Error", "Could not load image: " + ex.getMessage());
                }
            }
        });

        section.getChildren().addAll(lbl, imageContainer, btnUpload, lblInstruction);

        renderMarkersOnOverlay();

        return section;
    }

    private void renderMarkersOnOverlay() {
        if (markerOverlayPane == null || mapImageView == null) return;
        markerOverlayPane.getChildren().clear();

        if (mapImageView.getImage() == null) return;

        // Compute the actual rendered image size (preserveRatio means it may be smaller than fitWidth/fitHeight)
        Image img = mapImageView.getImage();
        double imgW = img.getWidth();
        double imgH = img.getHeight();
        double fitW = mapImageView.getFitWidth();
        double fitH = mapImageView.getFitHeight();

        double scale = Math.min(fitW / imgW, fitH / imgH);
        double renderedW = imgW * scale;
        double renderedH = imgH * scale;

        // Offset from top-left of the ImageView to the actual image
        double offsetX = (fitW - renderedW) / 2.0;
        double offsetY = (fitH - renderedH) / 2.0;

        // Get map sites for numbering
        List<Site> mapSites = currentEditingMap != null && currentEditingMap.getSites() != null
                ? currentEditingMap.getSites() : new ArrayList<>();

        for (SiteMarker marker : pendingMarkers) {
            int index = -1;
            for (int i = 0; i < mapSites.size(); i++) {
                if (mapSites.get(i).getId() == marker.getSiteId()) {
                    index = i;
                    break;
                }
            }
            if (index < 0) {
                // Check citySites for newly added sites not yet in mapSites list
                for (int i = 0; i < citySites.size(); i++) {
                    if (citySites.get(i).getId() == marker.getSiteId()) {
                        index = mapSites.size() + i;
                        break;
                    }
                }
            }
            if (index < 0) continue;

            StackPane pin = createMarkerPin(index + 1);
            double px = offsetX + marker.getX() * renderedW - 12;
            double py = offsetY + marker.getY() * renderedH - 12;
            pin.setLayoutX(px);
            pin.setLayoutY(py);
            markerOverlayPane.getChildren().add(pin);
        }
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
        pin.setMouseTransparent(true);
        return pin;
    }

    private VBox createMapSiteAssignmentSection(GCMMap map) {
        VBox section = new VBox(8);
        section.getStyleClass().add("field-section");
        section.setPadding(new Insets(10));

        Label lbl = new Label("Sites on this Map");
        lbl.getStyleClass().add("field-label");

        // Current map sites
        ListView<Site> lvOnMap = new ListView<>();
        lvOnMap.setPrefHeight(120);
        lvOnMap.getStyleClass().add("edit-list");
        currentMapOnMapSites = FXCollections.observableArrayList(
                map.getSites() != null ? map.getSites() : new ArrayList<>()
        );
        lvOnMap.setItems(currentMapOnMapSites);
        lvOnMap.setCellFactory(lv -> new ListCell<Site>() {
            @Override
            protected void updateItem(Site item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    boolean hasMarker = pendingMarkers.stream().anyMatch(m -> m.getSiteId() == item.getId());
                    setText(item.getName() + (hasMarker ? "" : "  (no marker)"));
                }
            }
        });

        // Store reference so the map click handler can read the selection
        this.lvOnMapSites = lvOnMap;

        // When selecting a site in On Map list, update the instruction
        lvOnMap.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                boolean hasMarker = pendingMarkers.stream().anyMatch(m -> m.getSiteId() == newVal.getId());
                String action = hasMarker ? "reposition" : "place";
                updateMarkerInstruction("Click on the map to " + action + " marker for: " + newVal.getName());
            }
        });

        // Available sites (not on map)
        ListView<Site> lvAvailable = new ListView<>();
        lvAvailable.setPrefHeight(120);
        lvAvailable.getStyleClass().add("edit-list");
        ObservableList<Site> availableSites = FXCollections.observableArrayList();
        for (Site s : citySites) {
            boolean onMap = currentMapOnMapSites.stream().anyMatch(ms -> ms.getId() == s.getId());
            if (!onMap) availableSites.add(s);
        }
        lvAvailable.setItems(availableSites);
        lvAvailable.setCellFactory(lv -> new ListCell<Site>() {
            @Override
            protected void updateItem(Site item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        // Transfer buttons
        Button btnAdd = new Button("\u2190 Add to Map");
        btnAdd.getStyleClass().add("transfer-button");
        btnAdd.setOnAction(e -> {
            Site selected = lvAvailable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                currentMapOnMapSites.add(selected);
                availableSites.remove(selected);
                if (map.getSites() == null) map.setSites(new ArrayList<>());
                map.getSites().add(selected);

                // Auto-select the newly added site so user can immediately place its marker
                lvOnMap.getSelectionModel().select(selected);
                recalculateChangeCount();
                // Refresh cells to show "(no marker)" hint
                lvOnMap.refresh();
            }
        });

        Button btnRemove = new Button("Remove \u2192");
        btnRemove.getStyleClass().add("transfer-button");
        btnRemove.setOnAction(e -> {
            Site selected = lvOnMap.getSelectionModel().getSelectedItem();
            if (selected != null) {
                currentMapOnMapSites.remove(selected);
                availableSites.add(selected);
                if (map.getSites() != null) map.getSites().remove(selected);
                pendingMarkers.removeIf(m -> m.getSiteId() == selected.getId());
                renderMarkersOnOverlay();
                recalculateChangeCount();
                lvOnMap.refresh();
            }
        });

        VBox transferButtons = new VBox(5, btnAdd, btnRemove);
        transferButtons.setAlignment(Pos.CENTER);

        HBox listsBox = new HBox(10);
        VBox onMapBox = new VBox(3, new Label("On Map"), lvOnMap);
        onMapBox.getStyleClass().add("transfer-list-box");
        HBox.setHgrow(onMapBox, Priority.ALWAYS);

        VBox availableBox = new VBox(3, new Label("Available"), lvAvailable);
        availableBox.getStyleClass().add("transfer-list-box");
        HBox.setHgrow(availableBox, Priority.ALWAYS);

        listsBox.getChildren().addAll(onMapBox, transferButtons, availableBox);

        section.getChildren().addAll(lbl, listsBox);
        return section;
    }

    private void updateMarkerInstruction(String text) {
        vboxMapEditor.lookupAll("#markerInstruction").forEach(node -> {
            if (node instanceof Label) {
                ((Label) node).setText(text != null ? text : "");
            }
        });
    }

    // ==================== TAB 3: SITES ====================

    @FXML
    private void onNewSite() {
        isNewSite = true;
        currentEditingSite = new Site();
        lvSites.getSelectionModel().clearSelection();
        populateSiteEditor(currentEditingSite);
    }

    private void populateSiteEditor(Site site) {
        currentEditingSite = site;
        vboxSiteEditor.getChildren().clear();

        String titleText = isNewSite ? "New Site" : "Editing: " + site.getName();
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        vboxSiteEditor.getChildren().add(title);

        String prefix = "site_" + (isNewSite ? "new" : site.getId());

        // Name
        HBox nameRow = createEditableFieldRow("Name", isNewSite ? "" : site.getName(), prefix + "_name");
        vboxSiteEditor.getChildren().add(nameRow);

        // Description
        HBox descRow = createEditableTextAreaRow("Description", isNewSite ? "" : site.getDescription(), prefix + "_desc");
        vboxSiteEditor.getChildren().add(descRow);

        // Category ComboBox
        HBox catRow = createComboBoxRow("Category", SiteCategory.values(),
                isNewSite ? null : site.getCategory(), prefix + "_cat");
        vboxSiteEditor.getChildren().add(catRow);

        // Location
        HBox locRow = createEditableFieldRow("Location", isNewSite ? "" : site.getLocation(), prefix + "_loc");
        vboxSiteEditor.getChildren().add(locRow);

        // Accessibility CheckBox
        HBox accRow = createCheckBoxRow("Accessible", isNewSite ? false : site.isAccessible(), prefix + "_acc");
        vboxSiteEditor.getChildren().add(accRow);

        // Duration ComboBox
        HBox durRow = createComboBoxRow("Visit Duration", SiteDuration.values(),
                isNewSite ? null : site.getRecommendedVisitDuration(), prefix + "_dur");
        vboxSiteEditor.getChildren().add(durRow);

        // Buttons
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        if (!isNewSite) {
            Button btnDelete = new Button("Delete Site");
            btnDelete.getStyleClass().add("delete-button");
            btnDelete.setOnAction(e -> onDeleteSite(site));
            btnBox.getChildren().add(btnDelete);
        }

        // No individual submit — use "Submit Changes" in status bar
        vboxSiteEditor.getChildren().add(btnBox);
    }

    // ==================== TAB 4: TOURS ====================

    @FXML
    private void onNewTour() {
        isNewTour = true;
        currentEditingTour = new Tour();
        lvTours.getSelectionModel().clearSelection();
        populateTourEditor(currentEditingTour);
    }

    private void populateTourEditor(Tour tour) {
        currentEditingTour = tour;
        vboxTourEditor.getChildren().clear();

        String titleText = isNewTour ? "New Tour" : "Editing: " + tour.getName();
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        vboxTourEditor.getChildren().add(title);

        String prefix = "tour_" + (isNewTour ? "new" : tour.getId());

        // Name
        HBox nameRow = createEditableFieldRow("Name", isNewTour ? "" : tour.getName(), prefix + "_name");
        vboxTourEditor.getChildren().add(nameRow);

        // Description
        HBox descRow = createEditableTextAreaRow("Description", isNewTour ? "" : tour.getDescription(), prefix + "_desc");
        vboxTourEditor.getChildren().add(descRow);

        // Duration
        HBox durRow = createEditableFieldRow("Duration", isNewTour ? "" : tour.getRecommendedDuration(), prefix + "_dur");
        vboxTourEditor.getChildren().add(durRow);

        // Tour sites section with ordering
        vboxTourEditor.getChildren().add(createTourSitesSection(tour));

        // Buttons
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        if (!isNewTour) {
            Button btnDelete = new Button("Delete Tour");
            btnDelete.getStyleClass().add("delete-button");
            btnDelete.setOnAction(e -> onDeleteTour(tour));
            btnBox.getChildren().add(btnDelete);
        }

        // No individual submit — use "Submit Changes" in status bar
        vboxTourEditor.getChildren().add(btnBox);
    }

    private VBox createTourSitesSection(Tour tour) {
        VBox section = new VBox(8);
        section.getStyleClass().add("field-section");
        section.setPadding(new Insets(10));

        Label lbl = new Label("Tour Sites (ordered)");
        lbl.getStyleClass().add("field-label");

        // Tour sites (ordered)
        ListView<Site> lvTourSitesView = new ListView<>();
        lvTourSitesView.setPrefHeight(150);
        lvTourSitesView.getStyleClass().add("edit-list");
        currentTourSites = FXCollections.observableArrayList(
                tour.getSites() != null ? tour.getSites() : new ArrayList<>()
        );
        lvTourSitesView.setItems(currentTourSites);
        lvTourSitesView.setCellFactory(lv -> new ListCell<Site>() {
            @Override
            protected void updateItem(Site item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        // Up/Down buttons with labels
        Button btnUp = new Button("\u25B2 Move Up");
        btnUp.getStyleClass().add("order-button");
        btnUp.setOnAction(e -> {
            int idx = lvTourSitesView.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                Site s = currentTourSites.remove(idx);
                currentTourSites.add(idx - 1, s);
                lvTourSitesView.getSelectionModel().select(idx - 1);
                recalculateChangeCount();
            }
        });

        Button btnDown = new Button("\u25BC Move Down");
        btnDown.getStyleClass().add("order-button");
        btnDown.setOnAction(e -> {
            int idx = lvTourSitesView.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < currentTourSites.size() - 1) {
                Site s = currentTourSites.remove(idx);
                currentTourSites.add(idx + 1, s);
                lvTourSitesView.getSelectionModel().select(idx + 1);
                recalculateChangeCount();
            }
        });

        VBox orderButtons = new VBox(5, btnUp, btnDown);
        orderButtons.setAlignment(Pos.CENTER);

        // Available sites (not in tour)
        ListView<Site> lvAvailable = new ListView<>();
        lvAvailable.setPrefHeight(150);
        lvAvailable.getStyleClass().add("edit-list");
        ObservableList<Site> availableSites = FXCollections.observableArrayList();
        for (Site s : citySites) {
            boolean inTour = currentTourSites.stream().anyMatch(ts -> ts.getId() == s.getId());
            if (!inTour) availableSites.add(s);
        }
        lvAvailable.setItems(availableSites);
        lvAvailable.setCellFactory(lv -> new ListCell<Site>() {
            @Override
            protected void updateItem(Site item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        // Transfer buttons
        Button btnAddToTour = new Button("\u2190 Add to Tour");
        btnAddToTour.getStyleClass().add("transfer-button");
        btnAddToTour.setOnAction(e -> {
            Site selected = lvAvailable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                currentTourSites.add(selected);
                availableSites.remove(selected);
                recalculateChangeCount();
            }
        });

        Button btnRemoveFromTour = new Button("Remove \u2192");
        btnRemoveFromTour.getStyleClass().add("transfer-button");
        btnRemoveFromTour.setOnAction(e -> {
            Site selected = lvTourSitesView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                currentTourSites.remove(selected);
                availableSites.add(selected);
                recalculateChangeCount();
            }
        });

        VBox transferButtons = new VBox(5, btnAddToTour, btnRemoveFromTour);
        transferButtons.setAlignment(Pos.CENTER);

        HBox listsBox = new HBox(10);
        VBox tourBox = new VBox(3, new Label("In Tour (ordered)"), lvTourSitesView);
        tourBox.getStyleClass().add("transfer-list-box");
        HBox.setHgrow(tourBox, Priority.ALWAYS);

        VBox middleBox = new VBox(10, orderButtons, transferButtons);
        middleBox.setAlignment(Pos.CENTER);

        VBox availableBox = new VBox(3, new Label("Available"), lvAvailable);
        availableBox.getStyleClass().add("transfer-list-box");
        HBox.setHgrow(availableBox, Priority.ALWAYS);

        listsBox.getChildren().addAll(tourBox, middleBox, availableBox);
        section.getChildren().addAll(lbl, listsBox);
        return section;
    }

    // ==================== REUSABLE FIELD BUILDERS ====================

    private HBox createEditableFieldRow(String label, String originalValue, String fieldKey) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("field-row");
        row.setPadding(new Insets(8, 10, 8, 10));

        Label lbl = new Label(label + ":");
        lbl.setMinWidth(120);
        lbl.getStyleClass().add("field-label");

        TextField tf = new TextField(originalValue != null ? originalValue : "");
        tf.getStyleClass().add("edit-field");
        HBox.setHgrow(tf, Priority.ALWAYS);

        Button btnReset = new Button("\u21BA");
        btnReset.getStyleClass().add("reset-button");
        btnReset.setVisible(false);
        btnReset.setManaged(false);

        String origVal = originalValue != null ? originalValue : "";
        originalValues.put(fieldKey, origVal);

        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean changed = !origVal.equals(newVal);
            if (changed) {
                if (!row.getStyleClass().contains("field-row-changed"))
                    row.getStyleClass().add("field-row-changed");
                btnReset.setVisible(true);
                btnReset.setManaged(true);
            } else {
                row.getStyleClass().remove("field-row-changed");
                btnReset.setVisible(false);
                btnReset.setManaged(false);
            }
            recalculateChangeCount();
        });

        btnReset.setOnAction(e -> tf.setText(origVal));

        row.getChildren().addAll(lbl, tf, btnReset);
        fieldRows.put(fieldKey, row);
        return row;
    }

    private HBox createEditableTextAreaRow(String label, String originalValue, String fieldKey) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("field-row");
        row.setPadding(new Insets(8, 10, 8, 10));

        Label lbl = new Label(label + ":");
        lbl.setMinWidth(120);
        lbl.getStyleClass().add("field-label");

        TextArea ta = new TextArea(originalValue != null ? originalValue : "");
        ta.getStyleClass().add("edit-textarea");
        ta.setPrefRowCount(3);
        ta.setWrapText(true);
        HBox.setHgrow(ta, Priority.ALWAYS);

        Button btnReset = new Button("\u21BA");
        btnReset.getStyleClass().add("reset-button");
        btnReset.setVisible(false);
        btnReset.setManaged(false);

        String origVal = originalValue != null ? originalValue : "";
        originalValues.put(fieldKey, origVal);

        ta.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean changed = !origVal.equals(newVal);
            if (changed) {
                if (!row.getStyleClass().contains("field-row-changed"))
                    row.getStyleClass().add("field-row-changed");
                btnReset.setVisible(true);
                btnReset.setManaged(true);
            } else {
                row.getStyleClass().remove("field-row-changed");
                btnReset.setVisible(false);
                btnReset.setManaged(false);
            }
            recalculateChangeCount();
        });

        btnReset.setOnAction(e -> ta.setText(origVal));

        row.getChildren().addAll(lbl, ta, btnReset);
        fieldRows.put(fieldKey, row);
        return row;
    }

    @SuppressWarnings("unchecked")
    private <T> HBox createComboBoxRow(String label, T[] values, T selected, String fieldKey) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("field-row");
        row.setPadding(new Insets(8, 10, 8, 10));

        Label lbl = new Label(label + ":");
        lbl.setMinWidth(120);
        lbl.getStyleClass().add("field-label");

        ComboBox<T> cb = new ComboBox<>(FXCollections.observableArrayList(values));
        cb.getStyleClass().add("edit-combo");
        cb.setValue(selected);
        HBox.setHgrow(cb, Priority.ALWAYS);
        cb.setMaxWidth(Double.MAX_VALUE);

        // Make the button cell text white
        cb.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
                setTextFill(Color.WHITE);
            }
        });

        Button btnReset = new Button("\u21BA");
        btnReset.getStyleClass().add("reset-button");
        btnReset.setVisible(false);
        btnReset.setManaged(false);

        cb.setOnAction(e -> {
            boolean changed = !Objects.equals(selected, cb.getValue());
            if (changed) {
                if (!row.getStyleClass().contains("field-row-changed"))
                    row.getStyleClass().add("field-row-changed");
                btnReset.setVisible(true);
                btnReset.setManaged(true);
            } else {
                row.getStyleClass().remove("field-row-changed");
                btnReset.setVisible(false);
                btnReset.setManaged(false);
            }
            recalculateChangeCount();
        });

        btnReset.setOnAction(e -> cb.setValue(selected));

        row.getChildren().addAll(lbl, cb, btnReset);
        fieldRows.put(fieldKey, row);
        return row;
    }

    private HBox createCheckBoxRow(String label, boolean initialValue, String fieldKey) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("field-row");
        row.setPadding(new Insets(8, 10, 8, 10));

        Label lbl = new Label(label + ":");
        lbl.setMinWidth(120);
        lbl.getStyleClass().add("field-label");

        CheckBox cb = new CheckBox();
        cb.setSelected(initialValue);

        Button btnReset = new Button("\u21BA");
        btnReset.getStyleClass().add("reset-button");
        btnReset.setVisible(false);
        btnReset.setManaged(false);

        cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boolean changed = initialValue != newVal;
            if (changed) {
                if (!row.getStyleClass().contains("field-row-changed"))
                    row.getStyleClass().add("field-row-changed");
                btnReset.setVisible(true);
                btnReset.setManaged(true);
            } else {
                row.getStyleClass().remove("field-row-changed");
                btnReset.setVisible(false);
                btnReset.setManaged(false);
            }
            recalculateChangeCount();
        });

        btnReset.setOnAction(e -> cb.setSelected(initialValue));

        row.getChildren().addAll(lbl, cb, btnReset);
        fieldRows.put(fieldKey, row);
        return row;
    }

    private Label createPlaceholderLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("placeholder-label");
        return lbl;
    }

    // ==================== CHANGE COUNTING (snapshot-based) ====================

    /**
     * Recalculate the total change count by comparing current state to original snapshots.
     * This ensures add+remove = 0 changes, not 2.
     */
    private void recalculateChangeCount() {
        int count = 0;

        // 1. Count changed text fields / text areas (field rows with "field-row-changed" class)
        for (HBox row : fieldRows.values()) {
            if (row.getStyleClass().contains("field-row-changed")) {
                count++;
            }
        }

        // 2. Count map site assignment changes (compare current vs original)
        if (currentEditingMap != null && currentMapOnMapSites != null) {
            Set<Integer> currentIds = new HashSet<>();
            for (Site s : currentMapOnMapSites) currentIds.add(s.getId());
            Set<Integer> origIds = originalMapSiteIds.getOrDefault(currentEditingMap.getId(), new HashSet<>());
            if (!currentIds.equals(origIds)) {
                count++;
            }
        }

        // 3. Count map marker changes
        if (currentEditingMap != null) {
            List<SiteMarker> origMarkers = originalMapMarkers.getOrDefault(currentEditingMap.getId(), new ArrayList<>());
            if (!markersEqual(pendingMarkers, origMarkers)) {
                count++;
            }
        }

        // 4. Count map image changes
        if (currentEditingMap != null) {
            boolean imageChanged = !Arrays.equals(pendingMapImage, originalMapImage);
            if (imageChanged) {
                count++;
            }
        }

        // 5. Count tour site changes (compare current vs original)
        if (currentEditingTour != null && currentTourSites != null && !isNewTour) {
            List<Integer> currentIds = new ArrayList<>();
            for (Site s : currentTourSites) currentIds.add(s.getId());
            List<Integer> origIds = originalTourSiteIds.getOrDefault(currentEditingTour.getId(), new ArrayList<>());
            if (!currentIds.equals(origIds)) {
                count++;
            }
        }

        updateChangeCountLabel(count);
    }

    private boolean markersEqual(List<SiteMarker> a, List<SiteMarker> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            SiteMarker ma = a.get(i);
            SiteMarker mb = b.get(i);
            if (ma.getSiteId() != mb.getSiteId() ||
                    Math.abs(ma.getX() - mb.getX()) > 0.001 ||
                    Math.abs(ma.getY() - mb.getY()) > 0.001) {
                return false;
            }
        }
        return true;
    }

    // ==================== SUBMISSION METHODS ====================

    /**
     * Build a ContentChangeRequest for city detail changes, or null if nothing changed.
     */
    private ContentChangeRequest buildCityChangeRequest() {
        if (currentCity == null) return null;
        // Check if any city field changed
        boolean cityChanged = false;
        for (String key : fieldRows.keySet()) {
            if (key.startsWith("city") && fieldRows.get(key).getStyleClass().contains("field-row-changed")) {
                cityChanged = true;
                break;
            }
        }
        if (!cityChanged) return null;

        String name = getFieldValue("cityName");
        String desc = getTextAreaValue("cityDesc");
        if (name == null || name.trim().isEmpty()) return null;

        String json = buildCityJson(name, desc);
        User currentUser = client.getCurrentUser();
        Integer requesterId = currentUser != null ? currentUser.getId() : null;
        return new ContentChangeRequest(requesterId, ContentActionType.EDIT, ContentType.CITY,
                currentCity.getId(), currentCity.getName(), json);
    }

    /**
     * Build a ContentChangeRequest for the currently selected map, or null if nothing changed.
     */
    private ContentChangeRequest buildMapChangeRequest() {
        if (currentEditingMap == null || currentCity == null) return null;

        // Check if any map field, sites, markers, or image changed
        boolean changed = false;
        for (String key : fieldRows.keySet()) {
            if (key.startsWith("map") && fieldRows.get(key).getStyleClass().contains("field-row-changed")) {
                changed = true;
                break;
            }
        }
        if (currentMapOnMapSites != null) {
            Set<Integer> currentIds = new HashSet<>();
            for (Site s : currentMapOnMapSites) currentIds.add(s.getId());
            Set<Integer> origIds = originalMapSiteIds.getOrDefault(currentEditingMap.getId(), new HashSet<>());
            if (!currentIds.equals(origIds)) changed = true;
        }
        List<SiteMarker> origMarkers = originalMapMarkers.getOrDefault(currentEditingMap.getId(), new ArrayList<>());
        if (!markersEqual(pendingMarkers, origMarkers)) changed = true;
        if (!Arrays.equals(pendingMapImage, originalMapImage)) changed = true;

        if (!changed) return null;

        String name = getFieldValue("mapName_" + currentEditingMap.getId());
        String desc = getTextAreaValue("mapDesc_" + currentEditingMap.getId());
        String json = buildMapJson(currentEditingMap, name, desc);
        User currentUser = client.getCurrentUser();
        Integer requesterId = currentUser != null ? currentUser.getId() : null;
        return new ContentChangeRequest(requesterId, ContentActionType.EDIT, ContentType.MAP,
                currentEditingMap.getId(), currentCity.getName() + " - " + currentEditingMap.getName(), json);
    }

    /**
     * Build a ContentChangeRequest for the currently selected site, or null if nothing changed.
     */
    private ContentChangeRequest buildSiteChangeRequest() {
        if (currentCity == null) return null;
        Site site = currentEditingSite;
        if (site == null && !isNewSite) return null;

        String prefix = "site_" + (isNewSite ? "new" : site.getId());

        // Check if any site field changed (or if it's a new site)
        boolean changed = isNewSite;
        if (!changed) {
            for (String key : fieldRows.keySet()) {
                if (key.startsWith(prefix) && fieldRows.get(key).getStyleClass().contains("field-row-changed")) {
                    changed = true;
                    break;
                }
            }
        }
        if (!changed) return null;

        String name = getFieldValue(prefix + "_name");
        String desc = getTextAreaValue(prefix + "_desc");
        String location = getFieldValue(prefix + "_loc");
        if (name == null || name.trim().isEmpty()) return null;

        SiteCategory category = getComboValue(prefix + "_cat");
        Boolean accessible = getCheckBoxValue(prefix + "_acc");
        SiteDuration duration = getComboValue(prefix + "_dur");
        String json = buildSiteJson(name, desc, category, accessible, duration, location);

        User currentUser = client.getCurrentUser();
        Integer requesterId = currentUser != null ? currentUser.getId() : null;
        if (isNewSite) {
            return new ContentChangeRequest(requesterId, ContentActionType.ADD, ContentType.SITE,
                    currentCity.getId(), currentCity.getName() + " - " + name, json);
        } else {
            return new ContentChangeRequest(requesterId, ContentActionType.EDIT, ContentType.SITE,
                    site.getId(), currentCity.getName() + " - " + site.getName(), json);
        }
    }

    /**
     * Build a ContentChangeRequest for the currently selected tour, or null if nothing changed.
     */
    private ContentChangeRequest buildTourChangeRequest() {
        if (currentCity == null) return null;
        Tour tour = currentEditingTour;
        if (tour == null && !isNewTour) return null;

        String prefix = "tour_" + (isNewTour ? "new" : tour.getId());

        // Check if any tour field or site list changed (or if it's a new tour)
        boolean changed = isNewTour;
        if (!changed) {
            for (String key : fieldRows.keySet()) {
                if (key.startsWith(prefix) && fieldRows.get(key).getStyleClass().contains("field-row-changed")) {
                    changed = true;
                    break;
                }
            }
            if (currentTourSites != null) {
                List<Integer> currentIds = new ArrayList<>();
                for (Site s : currentTourSites) currentIds.add(s.getId());
                List<Integer> origIds = originalTourSiteIds.getOrDefault(tour.getId(), new ArrayList<>());
                if (!currentIds.equals(origIds)) changed = true;
            }
        }
        if (!changed) return null;

        String name = getFieldValue(prefix + "_name");
        String desc = getTextAreaValue(prefix + "_desc");
        String duration = getFieldValue(prefix + "_dur");
        if (name == null || name.trim().isEmpty()) return null;

        String siteIds = "";
        if (currentTourSites != null) {
            siteIds = currentTourSites.stream()
                    .map(s -> String.valueOf(s.getId()))
                    .collect(Collectors.joining(","));
        }
        String json = buildTourJson(name, desc, duration, siteIds);

        User currentUser = client.getCurrentUser();
        Integer requesterId = currentUser != null ? currentUser.getId() : null;
        if (isNewTour) {
            return new ContentChangeRequest(requesterId, ContentActionType.ADD, ContentType.TOUR,
                    currentCity.getId(), currentCity.getName() + " - " + name, json);
        } else {
            return new ContentChangeRequest(requesterId, ContentActionType.EDIT, ContentType.TOUR,
                    tour.getId(), currentCity.getName() + " - " + tour.getName(), json);
        }
    }

    private void onDeleteSite(Site site) {
        if (site == null || currentCity == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Site: " + site.getName());
        confirm.setContentText("This will submit a deletion request for approval.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String json = "{\"name\":\"" + escapeJson(site.getName()) + "\"}";
                submitContentChange(ContentActionType.DELETE, ContentType.SITE, site.getId(),
                        currentCity.getName() + " - " + site.getName(), json,
                        "Site deletion submitted for approval.");
            }
        });
    }

    private void onDeleteTour(Tour tour) {
        if (tour == null || currentCity == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Tour: " + tour.getName());
        confirm.setContentText("This will submit a deletion request for approval.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String json = "{\"name\":\"" + escapeJson(tour.getName()) + "\"}";
                submitContentChange(ContentActionType.DELETE, ContentType.TOUR, tour.getId(),
                        currentCity.getName() + " - " + tour.getName(), json,
                        "Tour deletion submitted for approval.");
            }
        });
    }

    @FXML
    private void onSubmitAll() {
        // Collect all change requests from current state
        List<ContentChangeRequest> toSubmit = new ArrayList<>();

        ContentChangeRequest cityReq = buildCityChangeRequest();
        if (cityReq != null) toSubmit.add(cityReq);

        ContentChangeRequest mapReq = buildMapChangeRequest();
        if (mapReq != null) toSubmit.add(mapReq);

        ContentChangeRequest siteReq = buildSiteChangeRequest();
        if (siteReq != null) toSubmit.add(siteReq);

        ContentChangeRequest tourReq = buildTourChangeRequest();
        if (tourReq != null) toSubmit.add(tourReq);

        if (toSubmit.isEmpty()) {
            showAlert("Info", "No changes to submit.");
            return;
        }

        int count = toSubmit.size();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Submit Changes");
        confirm.setHeaderText("Submit " + count + " change(s) for approval?");
        confirm.setContentText("Each change will appear as a separate item in the approval queue.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    int success = 0;
                    for (ContentChangeRequest req : toSubmit) {
                        try {
                            Message request = new Message(ActionType.SUBMIT_CONTENT_CHANGE_REQUEST, req);
                            Message resp = (Message) client.sendRequest(request);
                            if (resp != null && resp.getAction() == ActionType.SUBMIT_CONTENT_CHANGE_RESPONSE) {
                                boolean ok = (Boolean) resp.getMessage();
                                if (ok) success++;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    int finalSuccess = success;
                    Platform.runLater(() -> {
                        showAlert("Submitted", finalSuccess + "/" + toSubmit.size() + " changes submitted for approval.");
                        // Close the edit mode window
                        ((Stage) cbCitySelector.getScene().getWindow()).close();
                    });
                }).start();
            }
        });
    }

    private void submitContentChange(ContentActionType actionType, ContentType contentType,
                                      int targetId, String targetName, String json, String successMsg) {
        User currentUser = client.getCurrentUser();
        Integer requesterId = currentUser != null ? currentUser.getId() : null;

        ContentChangeRequest changeRequest = new ContentChangeRequest(
                requesterId, actionType, contentType, targetId, targetName, json
        );

        new Thread(() -> {
            try {
                Message request = new Message(ActionType.SUBMIT_CONTENT_CHANGE_REQUEST, changeRequest);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getAction() == ActionType.SUBMIT_CONTENT_CHANGE_RESPONSE) {
                        boolean success = (Boolean) response.getMessage();
                        if (success) {
                            showAlert("Success", successMsg);
                        } else {
                            showAlert("Error", "Failed to submit change.");
                        }
                    } else {
                        showAlert("Error", "Server error.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Error: " + e.getMessage()));
            }
        }).start();
    }

    // ==================== JSON BUILDERS ====================

    private String buildCityJson(String name, String description) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"name\":\"").append(escapeJson(name)).append("\"");
        sb.append(",\"description\":\"").append(escapeJson(description != null ? description : "")).append("\"");
        sb.append(",\"cityId\":").append(currentCity.getId());
        sb.append("}");
        return sb.toString();
    }

    private String buildMapJson(GCMMap map, String name, String description) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"mapName\":\"").append(escapeJson(name != null ? name : "")).append("\"");
        sb.append(",\"description\":\"").append(escapeJson(description != null ? description : "")).append("\"");

        // Map image (Base64) — only include if actually changed from original
        boolean imageChanged = !Arrays.equals(pendingMapImage, originalMapImage);
        if (imageChanged && pendingMapImage != null && pendingMapImage.length > 0) {
            String base64 = Base64.getEncoder().encodeToString(pendingMapImage);
            sb.append(",\"mapImage\":\"").append(base64).append("\"");
        }

        // Site markers JSON (unquoted array)
        if (!pendingMarkers.isEmpty()) {
            StringBuilder markersSb = new StringBuilder("[");
            for (int i = 0; i < pendingMarkers.size(); i++) {
                SiteMarker mk = pendingMarkers.get(i);
                if (i > 0) markersSb.append(",");
                markersSb.append(String.format(java.util.Locale.US, "{\"siteId\":%d,\"x\":%.4f,\"y\":%.4f}",
                        mk.getSiteId(), mk.getX(), mk.getY()));
            }
            markersSb.append("]");
            sb.append(",\"siteMarkersJson\":").append(markersSb);
        }

        // Site IDs (comma-separated) — use the live list
        if (currentMapOnMapSites != null && !currentMapOnMapSites.isEmpty()) {
            String siteIds = currentMapOnMapSites.stream()
                    .map(s -> String.valueOf(s.getId()))
                    .collect(Collectors.joining(","));
            sb.append(",\"siteIds\":\"").append(siteIds).append("\"");
        }

        sb.append("}");
        return sb.toString();
    }

    private String buildSiteJson(String name, String description, SiteCategory category,
                                  Boolean accessible, SiteDuration duration, String location) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"name\":\"").append(escapeJson(name != null ? name : "")).append("\"");
        sb.append(",\"description\":\"").append(escapeJson(description != null ? description : "")).append("\"");
        sb.append(",\"category\":\"").append(category != null ? category.name() : "").append("\"");
        sb.append(",\"isAccessible\":").append(accessible != null ? accessible : false);
        sb.append(",\"recommendedVisitDuration\":\"").append(duration != null ? duration.toString() : "").append("\"");
        sb.append(",\"location\":\"").append(escapeJson(location != null ? location : "")).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String buildTourJson(String name, String description, String duration, String siteIds) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"name\":\"").append(escapeJson(name != null ? name : "")).append("\"");
        sb.append(",\"description\":\"").append(escapeJson(description != null ? description : "")).append("\"");
        sb.append(",\"recommendedDuration\":\"").append(escapeJson(duration != null ? duration : "")).append("\"");
        sb.append(",\"siteIds\":\"").append(siteIds != null ? siteIds : "").append("\"");
        sb.append("}");
        return sb.toString();
    }

    // ==================== HELPER METHODS ====================

    private String getFieldValue(String fieldKey) {
        HBox row = fieldRows.get(fieldKey);
        if (row == null) return null;
        for (javafx.scene.Node node : row.getChildren()) {
            if (node instanceof TextField) return ((TextField) node).getText();
        }
        return null;
    }

    private String getTextAreaValue(String fieldKey) {
        HBox row = fieldRows.get(fieldKey);
        if (row == null) return null;
        for (javafx.scene.Node node : row.getChildren()) {
            if (node instanceof TextArea) return ((TextArea) node).getText();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T getComboValue(String fieldKey) {
        HBox row = fieldRows.get(fieldKey);
        if (row == null) return null;
        for (javafx.scene.Node node : row.getChildren()) {
            if (node instanceof ComboBox) return (T) ((ComboBox<?>) node).getValue();
        }
        return null;
    }

    private Boolean getCheckBoxValue(String fieldKey) {
        HBox row = fieldRows.get(fieldKey);
        if (row == null) return null;
        for (javafx.scene.Node node : row.getChildren()) {
            if (node instanceof CheckBox) return ((CheckBox) node).isSelected();
        }
        return null;
    }

    private void resetChangeTracking() {
        originalValues.clear();
        fieldRows.clear();
        originalMapSiteIds.clear();
        originalTourSiteIds.clear();
        originalMapMarkers.clear();
        pendingMapImage = null;
        originalMapImage = null;
        pendingMarkers.clear();
        lvOnMapSites = null;
        currentMapOnMapSites = null;
        currentTourSites = null;
        updateChangeCountLabel(0);
    }

    private void updateChangeCountLabel(int count) {
        lblChangeCount.setText(count + " change" + (count != 1 ? "s" : ""));
        btnSubmitAll.setDisable(count == 0);
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @FXML
    private void onClose() {
        ((Stage) cbCitySelector.getScene().getWindow()).close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
