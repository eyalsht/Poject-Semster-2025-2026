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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import util.AlertHelper;

import java.io.ByteArrayInputStream;
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

    // Per-map state storage (for multi-map editing across map switches)
    private final Map<Integer, List<SiteMarker>> pendingMarkersPerMap = new HashMap<>();
    private final Map<Integer, List<Site>> savedMapSitesPerMap = new HashMap<>();
    private final Map<Integer, List<Site>> savedTourSitesPerTour = new HashMap<>();

    // Site/Tour editing state
    private Site currentEditingSite;
    private Tour currentEditingTour;
    private final List<Site> newSites = new ArrayList<>();
    private final List<Tour> newTours = new ArrayList<>();
    private int tempSiteIdCounter = -1;
    private int tempTourIdCounter = -1;

    private boolean isTempSite(Site s) { return s != null && s.getId() < 0; }
    private boolean isTempTour(Tour t) { return t != null && t.getId() < 0; }

    // Live observable lists for current map/tour editors (for snapshot comparison)
    private ObservableList<Site> currentMapOnMapSites;
    private ObservableList<Site> currentTourSites;

    // ==================== INITIALIZATION & DATA LOADING ====================

    @FXML
    public void initialize() {
        tabCityDetails.setDisable(true);
        tabMaps.setDisable(true);
        tabSites.setDisable(true);
        tabTours.setDisable(true);

        setupCellFactories();
        setupSelectionListeners();
        loadCities();
    }

    private void setupCellFactories() {
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

        lvMaps.setCellFactory(lv -> new ListCell<GCMMap>() {
            @Override
            protected void updateItem(GCMMap item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + " v" + item.getVersion());
            }
        });

        lvSites.setCellFactory(lv -> new ListCell<Site>() {
            @Override
            protected void updateItem(Site item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String name = (item.getName() == null || item.getName().isBlank()) ? "Untitled" : item.getName();
                    setText(isTempSite(item) ? "(New) " + name : name);
                }
            }
        });

        lvTours.setCellFactory(lv -> new ListCell<Tour>() {
            @Override
            protected void updateItem(Tour item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String name = (item.getName() == null || item.getName().isBlank()) ? "Untitled" : item.getName();
                    setText(isTempTour(item) ? "(New) " + name : name);
                }
            }
        });
    }

    private void setupSelectionListeners() {
        cbCitySelector.setOnAction(e -> onCitySelected());

        lvMaps.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) populateMapEditor(newVal);
        });

        lvSites.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) populateSiteEditor(newVal);
        });

        lvTours.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) populateTourEditor(newVal);
        });
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

        // Warn if there are unsaved changes in the current city
        if (currentCity != null && !btnSubmitAll.isDisabled()) {
            Optional<ButtonType> result = AlertHelper.showConfirmation(
                    "Unsaved Changes",
                    "You have unsaved changes in " + currentCity.getName(),
                    "Switching cities will discard all changes. Continue?");
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                // Revert the ComboBox selection back to the current city
                cbCitySelector.setValue(currentCity);
                return;
            }
        }

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

    // ==================== CITY DETAILS TAB ====================

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

    // ==================== MAPS TAB ====================

    /**
     * Save the current map's markers and site assignments before switching to another map.
     */
    private void saveCurrentMapState() {
        if (currentEditingMap == null) return;
        int mapId = currentEditingMap.getId();
        pendingMarkersPerMap.put(mapId, new ArrayList<>(pendingMarkers));
        if (currentMapOnMapSites != null) {
            savedMapSitesPerMap.put(mapId, new ArrayList<>(currentMapOnMapSites));
        }
    }

    private void populateMapEditor(GCMMap map) {
        saveCurrentMapState();
        currentEditingMap = map;
        pendingMapImage = map.getMapImage();
        originalMapImage = map.getMapImage();
        // Restore markers from saved state if available, otherwise use map's original markers
        if (pendingMarkersPerMap.containsKey(map.getId())) {
            pendingMarkers = new ArrayList<>(pendingMarkersPerMap.get(map.getId()));
        } else {
            pendingMarkers = map.getSiteMarkers() != null ? new ArrayList<>(map.getSiteMarkers()) : new ArrayList<>();
        }

        vboxMapEditor.getChildren().clear();

        Label title = new Label("Editing: " + map.getName());
        title.getStyleClass().add("section-title");
        vboxMapEditor.getChildren().add(title);

        // Map Name & Description — re-attach existing rows if previously edited
        String nameKey = "mapName_" + map.getId();
        String descKey = "mapDesc_" + map.getId();
        if (fieldRows.containsKey(nameKey)) {
            vboxMapEditor.getChildren().add(fieldRows.get(nameKey));
            vboxMapEditor.getChildren().add(fieldRows.get(descKey));
        } else {
            HBox nameRow = createEditableFieldRow("Map Name", map.getName(), nameKey);
            vboxMapEditor.getChildren().add(nameRow);
            HBox descRow = createEditableTextAreaRow("Description", map.getDescription(), descKey);
            vboxMapEditor.getChildren().add(descRow);
        }

        // Map Image Section
        vboxMapEditor.getChildren().add(createMapImageSection(map));

        // Map-Site assignment section
        vboxMapEditor.getChildren().add(createMapSiteAssignmentSection(map));

        // Delete button — reverts map back to external system
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        Button btnDeleteMap = new Button("Delete Map");
        btnDeleteMap.getStyleClass().add("delete-button");
        btnDeleteMap.setOnAction(e -> onDeleteMap(map));

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

        markerOverlayPane.setOnMouseClicked(event -> onMapOverlayClicked(event, lblInstruction));

        section.getChildren().addAll(lbl, imageContainer, lblInstruction);

        renderMarkersOnOverlay();

        return section;
    }

    private void onMapOverlayClicked(MouseEvent event, Label lblInstruction) {
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

        // Current map sites — restore from saved state if available
        ListView<Site> lvOnMap = new ListView<>();
        lvOnMap.setPrefHeight(120);
        lvOnMap.getStyleClass().add("edit-list");
        if (savedMapSitesPerMap.containsKey(map.getId())) {
            currentMapOnMapSites = FXCollections.observableArrayList(savedMapSitesPerMap.get(map.getId()));
        } else {
            currentMapOnMapSites = FXCollections.observableArrayList(
                    map.getSites() != null ? map.getSites() : new ArrayList<>()
            );
        }
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

    // ==================== SITES TAB ====================

    @FXML
    private void onNewSite() {
        Site site = new Site();
        site.setId(tempSiteIdCounter--);
        site.setName("(New Site)");
        newSites.add(site);
        citySites.add(site);
        lvSites.getSelectionModel().select(site);
    }

    private void populateSiteEditor(Site site) {
        currentEditingSite = site;
        vboxSiteEditor.getChildren().clear();

        boolean isTemp = isTempSite(site);
        String titleText = isTemp ? "New Site" : "Editing: " + site.getName();
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        vboxSiteEditor.getChildren().add(title);

        String prefix = "site_" + site.getId();
        boolean hasExistingFields = fieldRows.containsKey(prefix + "_name");

        if (hasExistingFields) {
            // Re-attach previously created field rows (preserves all edits)
            vboxSiteEditor.getChildren().addAll(
                fieldRows.get(prefix + "_name"),
                fieldRows.get(prefix + "_desc"),
                fieldRows.get(prefix + "_cat"),
                fieldRows.get(prefix + "_loc"),
                fieldRows.get(prefix + "_acc"),
                fieldRows.get(prefix + "_dur")
            );
        } else {
            // Name
            HBox nameRow = createEditableFieldRow("Name", isTemp ? "" : site.getName(), prefix + "_name");
            vboxSiteEditor.getChildren().add(nameRow);

            // Add name-change listener for temp sites to update ListView
            if (isTemp) {
                for (javafx.scene.Node node : nameRow.getChildren()) {
                    if (node instanceof TextField) {
                        ((TextField) node).textProperty().addListener((obs, oldVal, newVal) -> {
                            site.setName(newVal);
                            lvSites.refresh();
                        });
                        break;
                    }
                }
            }

            // Description
            HBox descRow = createEditableTextAreaRow("Description", isTemp ? "" : site.getDescription(), prefix + "_desc");
            vboxSiteEditor.getChildren().add(descRow);

            // Category ComboBox
            HBox catRow = createComboBoxRow("Category", SiteCategory.values(),
                    isTemp ? null : site.getCategory(), prefix + "_cat");
            vboxSiteEditor.getChildren().add(catRow);

            // Location
            HBox locRow = createEditableFieldRow("Location", isTemp ? "" : site.getLocation(), prefix + "_loc");
            vboxSiteEditor.getChildren().add(locRow);

            // Accessibility CheckBox
            HBox accRow = createCheckBoxRow("Accessible", isTemp ? false : site.isAccessible(), prefix + "_acc");
            vboxSiteEditor.getChildren().add(accRow);

            // Duration ComboBox
            HBox durRow = createComboBoxRow("Visit Duration", SiteDuration.values(),
                    isTemp ? null : site.getRecommendedVisitDuration(), prefix + "_dur");
            vboxSiteEditor.getChildren().add(durRow);
        }

        // Buttons
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        Button btnDelete = new Button("Delete Site");
        btnDelete.getStyleClass().add("delete-button");
        btnDelete.setOnAction(e -> onDeleteSite(site));
        btnBox.getChildren().add(btnDelete);

        // No individual submit — use "Submit Changes" in status bar
        vboxSiteEditor.getChildren().add(btnBox);
    }

    // ==================== TOURS TAB ====================

    @FXML
    private void onNewTour() {
        Tour tour = new Tour();
        tour.setId(tempTourIdCounter--);
        tour.setName("(New Tour)");
        newTours.add(tour);
        cityTours.add(tour);
        lvTours.getSelectionModel().select(tour);
    }

    /**
     * Save the current tour's site list before switching to another tour.
     */
    private void saveCurrentTourState() {
        if (currentEditingTour == null) return;
        if (currentTourSites != null) {
            savedTourSitesPerTour.put(currentEditingTour.getId(), new ArrayList<>(currentTourSites));
        }
    }

    private void populateTourEditor(Tour tour) {
        saveCurrentTourState();
        currentEditingTour = tour;
        vboxTourEditor.getChildren().clear();

        boolean isTemp = isTempTour(tour);
        String titleText = isTemp ? "New Tour" : "Editing: " + tour.getName();
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        vboxTourEditor.getChildren().add(title);

        String prefix = "tour_" + tour.getId();
        boolean hasExistingFields = fieldRows.containsKey(prefix + "_name");

        if (hasExistingFields) {
            // Re-attach previously created field rows (preserves all edits)
            vboxTourEditor.getChildren().addAll(
                fieldRows.get(prefix + "_name"),
                fieldRows.get(prefix + "_desc"),
                fieldRows.get(prefix + "_dur")
            );
        } else {
            // Name
            HBox nameRow = createEditableFieldRow("Name", isTemp ? "" : tour.getName(), prefix + "_name");
            vboxTourEditor.getChildren().add(nameRow);

            // Add name-change listener for temp tours to update ListView
            if (isTemp) {
                for (javafx.scene.Node node : nameRow.getChildren()) {
                    if (node instanceof TextField) {
                        ((TextField) node).textProperty().addListener((obs, oldVal, newVal) -> {
                            tour.setName(newVal);
                            lvTours.refresh();
                        });
                        break;
                    }
                }
            }

            // Description
            HBox descRow = createEditableTextAreaRow("Description", isTemp ? "" : tour.getDescription(), prefix + "_desc");
            vboxTourEditor.getChildren().add(descRow);

            // Duration
            HBox durRow = createEditableFieldRow("Duration", isTemp ? "" : tour.getRecommendedDuration(), prefix + "_dur");
            vboxTourEditor.getChildren().add(durRow);
        }

        // Tour sites section with ordering
        vboxTourEditor.getChildren().add(createTourSitesSection(tour));

        // Buttons
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        Button btnDelete = new Button("Delete Tour");
        btnDelete.getStyleClass().add("delete-button");
        btnDelete.setOnAction(e -> onDeleteTour(tour));
        btnBox.getChildren().add(btnDelete);

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
        if (savedTourSitesPerTour.containsKey(tour.getId())) {
            currentTourSites = FXCollections.observableArrayList(savedTourSitesPerTour.get(tour.getId()));
        } else {
            currentTourSites = FXCollections.observableArrayList(
                    tour.getSites() != null ? tour.getSites() : new ArrayList<>()
            );
        }
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
            applyChangeHighlight(row, btnReset, !origVal.equals(newVal));
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
            applyChangeHighlight(row, btnReset, !origVal.equals(newVal));
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
            applyChangeHighlight(row, btnReset, !Objects.equals(selected, cb.getValue()));
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
            applyChangeHighlight(row, btnReset, initialValue != newVal);
        });

        btnReset.setOnAction(e -> cb.setSelected(initialValue));

        row.getChildren().addAll(lbl, cb, btnReset);
        fieldRows.put(fieldKey, row);
        return row;
    }

    private void applyChangeHighlight(HBox row, Button btnReset, boolean changed) {
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
    }

    private Label createPlaceholderLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("placeholder-label");
        return lbl;
    }

    // ==================== CHANGE TRACKING ====================

    private void recalculateChangeCount() {
        int count = countFieldChanges() + countMapStructureChanges() + countTourStructureChanges();
        updateChangeCountLabel(count);
    }

    private int countFieldChanges() {
        int count = 0;
        for (HBox row : fieldRows.values()) {
            if (row.getStyleClass().contains("field-row-changed")) count++;
        }
        return count;
    }

    private int countMapStructureChanges() {
        int count = 0;
        for (GCMMap map : cityMaps) {
            int mapId = map.getId();

            // Determine sites for this map: live state if current, saved state otherwise
            List<Site> sites;
            List<SiteMarker> markers;
            if (currentEditingMap != null && currentEditingMap.getId() == mapId) {
                sites = currentMapOnMapSites != null ? new ArrayList<>(currentMapOnMapSites) : null;
                markers = pendingMarkers;
            } else if (savedMapSitesPerMap.containsKey(mapId) || pendingMarkersPerMap.containsKey(mapId)) {
                sites = savedMapSitesPerMap.get(mapId);
                markers = pendingMarkersPerMap.getOrDefault(mapId, new ArrayList<>());
            } else {
                continue; // never edited
            }

            // Site assignment changes
            if (sites != null) {
                Set<Integer> currentIds = new HashSet<>();
                for (Site s : sites) currentIds.add(s.getId());
                Set<Integer> origIds = originalMapSiteIds.getOrDefault(mapId, new HashSet<>());
                if (!currentIds.equals(origIds)) count++;
            }

            // Marker changes
            List<SiteMarker> origMarkers = originalMapMarkers.getOrDefault(mapId, new ArrayList<>());
            if (!markersEqual(markers, origMarkers)) count++;
        }
        return count;
    }

    private int countTourStructureChanges() {
        int count = 0;
        for (Tour tour : cityTours) {
            int tourId = tour.getId();
            List<Site> tourSites;
            if (currentEditingTour != null && currentEditingTour.getId() == tourId) {
                tourSites = currentTourSites != null ? new ArrayList<>(currentTourSites) : null;
            } else if (savedTourSitesPerTour.containsKey(tourId)) {
                tourSites = savedTourSitesPerTour.get(tourId);
            } else {
                continue;
            }
            if (tourSites != null) {
                List<Integer> currentIds = new ArrayList<>();
                for (Site s : tourSites) currentIds.add(s.getId());
                List<Integer> origIds = originalTourSiteIds.getOrDefault(tourId, new ArrayList<>());
                if (!currentIds.equals(origIds)) count++;
            }
        }
        return count;
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

    // ==================== SUBMISSION & CHANGE REQUESTS ====================

    @FXML
    private void onSubmitAll() {
        // Flush current map/tour state before collecting
        saveCurrentMapState();
        saveCurrentTourState();

        String validationError = validateBeforeSubmit();
        if (validationError != null) {
            AlertHelper.showWarning("Validation Error", validationError);
            return;
        }

        List<ContentChangeRequest> toSubmit = collectAllChangeRequests();

        if (toSubmit.isEmpty()) {
            AlertHelper.showInfo("Info", "No changes to submit.");
            return;
        }

        int count = toSubmit.size();
        AlertHelper.showConfirmation(
                "Submit Changes",
                "Submit " + count + " change(s) for approval?",
                "Each change will appear as a separate item in the approval queue."
        ).ifPresent(response -> {
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
                        AlertHelper.showInfo("Submitted", finalSuccess + "/" + toSubmit.size() + " changes submitted for approval.");
                        // Close the edit mode window
                        ((Stage) cbCitySelector.getScene().getWindow()).close();
                    });
                }).start();
            }
        });
    }

    private String validateBeforeSubmit() {
        // Validate: no map should end up with zero sites (only if the editor removed them)
        for (GCMMap map : cityMaps) {
            List<Site> sites = savedMapSitesPerMap.get(map.getId());
            Set<Integer> origIds = originalMapSiteIds.getOrDefault(map.getId(), new HashSet<>());
            if (sites != null && sites.isEmpty() && !origIds.isEmpty()) {
                return "Map \"" + map.getName() + "\" must contain at least one site. Please add a site before submitting.";
            }
        }
        return validateAllFields();
    }

    private List<ContentChangeRequest> collectAllChangeRequests() {
        List<ContentChangeRequest> toSubmit = new ArrayList<>();

        ContentChangeRequest cityReq = buildCityChangeRequest();
        if (cityReq != null) toSubmit.add(cityReq);

        // Build change requests for ALL maps (not just current)
        for (GCMMap map : cityMaps) {
            ContentChangeRequest mapReq = buildMapChangeRequestForMap(map);
            if (mapReq != null) toSubmit.add(mapReq);
        }

        // Build change requests for ALL sites (including temp new sites)
        for (Site site : citySites) {
            if (isTempSite(site)) {
                ContentChangeRequest siteReq = buildNewSiteChangeRequest(site);
                if (siteReq != null) toSubmit.add(siteReq);
            } else {
                ContentChangeRequest siteReq = buildSiteChangeRequestForSite(site);
                if (siteReq != null) toSubmit.add(siteReq);
            }
        }

        // Build change requests for ALL tours (including temp new tours)
        for (Tour tour : cityTours) {
            if (isTempTour(tour)) {
                ContentChangeRequest tourReq = buildNewTourChangeRequest(tour);
                if (tourReq != null) toSubmit.add(tourReq);
            } else {
                ContentChangeRequest tourReq = buildTourChangeRequestForTour(tour);
                if (tourReq != null) toSubmit.add(tourReq);
            }
        }

        return toSubmit;
    }

    private ContentChangeRequest buildCityChangeRequest() {
        if (currentCity == null) return null;
        if (!hasChangedFieldsWithPrefix("city")) return null;

        String name = getFieldValue("cityName");
        String desc = getTextAreaValue("cityDesc");
        if (name == null || name.trim().isEmpty()) return null;

        String json = buildCityJson(name, desc);
        return makeChangeRequest(ContentActionType.EDIT, ContentType.CITY,
                currentCity.getId(), currentCity.getName(), json);
    }

    private ContentChangeRequest buildMapChangeRequestForMap(GCMMap map) {
        if (map == null || currentCity == null) return null;
        int mapId = map.getId();

        // Skip maps that were never opened in the editor
        if (!pendingMarkersPerMap.containsKey(mapId) && !savedMapSitesPerMap.containsKey(mapId)) {
            return null;
        }

        // Determine markers and sites for this map
        List<SiteMarker> markers = pendingMarkersPerMap.getOrDefault(mapId, new ArrayList<>());
        List<Site> sites = savedMapSitesPerMap.get(mapId);

        // Check if any map field changed
        boolean changed = false;
        String nameKey = "mapName_" + mapId;
        String descKey = "mapDesc_" + mapId;
        HBox nameRow = fieldRows.get(nameKey);
        HBox descRow = fieldRows.get(descKey);
        if (nameRow != null && nameRow.getStyleClass().contains("field-row-changed")) changed = true;
        if (descRow != null && descRow.getStyleClass().contains("field-row-changed")) changed = true;

        // Check site assignment changes
        if (sites != null) {
            Set<Integer> currentIds = new HashSet<>();
            for (Site s : sites) currentIds.add(s.getId());
            Set<Integer> origIds = originalMapSiteIds.getOrDefault(mapId, new HashSet<>());
            if (!currentIds.equals(origIds)) changed = true;
        }

        // Check marker changes
        List<SiteMarker> origMarkers = originalMapMarkers.getOrDefault(mapId, new ArrayList<>());
        if (!markersEqual(markers, origMarkers)) changed = true;

        if (!changed) return null;

        String name = getFieldValue(nameKey);
        String desc = getTextAreaValue(descKey);
        String json = buildMapJsonForMap(map, name, desc, markers, sites);
        return makeChangeRequest(ContentActionType.EDIT, ContentType.MAP,
                mapId, currentCity.getName() + " - " + map.getName(), json);
    }

    private ContentChangeRequest buildNewSiteChangeRequest(Site site) {
        if (currentCity == null || site == null) return null;

        String prefix = "site_" + site.getId();

        // Skip if fields were never created (shouldn't happen, but be safe)
        if (!fieldRows.containsKey(prefix + "_name")) return null;

        String name = getFieldValue(prefix + "_name");
        String desc = getTextAreaValue(prefix + "_desc");
        String location = getFieldValue(prefix + "_loc");
        if (name == null || name.trim().isEmpty()) return null;

        SiteCategory category = getComboValue(prefix + "_cat");
        Boolean accessible = getCheckBoxValue(prefix + "_acc");
        SiteDuration duration = getComboValue(prefix + "_dur");
        String json = buildSiteJson(name, desc, category, accessible, duration, location);

        return makeChangeRequest(ContentActionType.ADD, ContentType.SITE,
                currentCity.getId(), currentCity.getName() + " - " + name, json);
    }

    private ContentChangeRequest buildNewTourChangeRequest(Tour tour) {
        if (currentCity == null || tour == null) return null;

        String prefix = "tour_" + tour.getId();

        // Skip if fields were never created
        if (!fieldRows.containsKey(prefix + "_name")) return null;

        String name = getFieldValue(prefix + "_name");
        String desc = getTextAreaValue(prefix + "_desc");
        String duration = getFieldValue(prefix + "_dur");
        if (name == null || name.trim().isEmpty()) return null;

        // Get tour sites from saved state
        String siteIds = "";
        List<Site> sites = savedTourSitesPerTour.get(tour.getId());
        if (currentEditingTour != null && currentEditingTour.getId() == tour.getId() && currentTourSites != null) {
            sites = new ArrayList<>(currentTourSites);
        }
        if (sites != null) {
            siteIds = sites.stream()
                    .map(s -> String.valueOf(s.getId()))
                    .collect(Collectors.joining(","));
        }
        String json = buildTourJson(name, desc, duration, siteIds);

        return makeChangeRequest(ContentActionType.ADD, ContentType.TOUR,
                currentCity.getId(), currentCity.getName() + " - " + name, json);
    }

    private ContentChangeRequest buildSiteChangeRequestForSite(Site site) {
        if (site == null || currentCity == null) return null;
        String prefix = "site_" + site.getId();

        // Skip sites that were never opened in the editor
        if (!fieldRows.containsKey(prefix + "_name")) return null;

        if (!hasChangedFieldsWithPrefix(prefix)) return null;

        String name = getFieldValue(prefix + "_name");
        String desc = getTextAreaValue(prefix + "_desc");
        String location = getFieldValue(prefix + "_loc");
        if (name == null || name.trim().isEmpty()) return null;

        SiteCategory category = getComboValue(prefix + "_cat");
        Boolean accessible = getCheckBoxValue(prefix + "_acc");
        SiteDuration duration = getComboValue(prefix + "_dur");
        String json = buildSiteJson(name, desc, category, accessible, duration, location);

        return makeChangeRequest(ContentActionType.EDIT, ContentType.SITE,
                site.getId(), currentCity.getName() + " - " + site.getName(), json);
    }

    private ContentChangeRequest buildTourChangeRequestForTour(Tour tour) {
        if (tour == null || currentCity == null) return null;
        int tourId = tour.getId();
        String prefix = "tour_" + tourId;

        // Skip tours that were never opened in the editor
        if (!fieldRows.containsKey(prefix + "_name")) return null;

        // Check if any tour field changed
        boolean changed = hasChangedFieldsWithPrefix(prefix);

        // Check tour site list changes
        List<Site> sites;
        if (currentEditingTour != null && currentEditingTour.getId() == tourId) {
            sites = currentTourSites != null ? new ArrayList<>(currentTourSites) : null;
        } else {
            sites = savedTourSitesPerTour.get(tourId);
        }
        if (sites != null) {
            List<Integer> currentIds = new ArrayList<>();
            for (Site s : sites) currentIds.add(s.getId());
            List<Integer> origIds = originalTourSiteIds.getOrDefault(tourId, new ArrayList<>());
            if (!currentIds.equals(origIds)) changed = true;
        }

        if (!changed) return null;

        String name = getFieldValue(prefix + "_name");
        String desc = getTextAreaValue(prefix + "_desc");
        String duration = getFieldValue(prefix + "_dur");
        if (name == null || name.trim().isEmpty()) return null;

        String siteIds = "";
        if (sites != null) {
            siteIds = sites.stream()
                    .map(s -> String.valueOf(s.getId()))
                    .collect(Collectors.joining(","));
        }
        String json = buildTourJson(name, desc, duration, siteIds);

        return makeChangeRequest(ContentActionType.EDIT, ContentType.TOUR,
                tourId, currentCity.getName() + " - " + tour.getName(), json);
    }

    private void submitContentChange(ContentActionType actionType, ContentType contentType,
                                      int targetId, String targetName, String json, String successMsg) {
        ContentChangeRequest changeRequest = makeChangeRequest(actionType, contentType, targetId, targetName, json);

        new Thread(() -> {
            try {
                Message request = new Message(ActionType.SUBMIT_CONTENT_CHANGE_REQUEST, changeRequest);
                Message response = (Message) client.sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getAction() == ActionType.SUBMIT_CONTENT_CHANGE_RESPONSE) {
                        boolean success = (Boolean) response.getMessage();
                        if (success) {
                            AlertHelper.showInfo("Success", successMsg);
                        } else {
                            AlertHelper.showWarning("Error", "Failed to submit change.");
                        }
                    } else {
                        AlertHelper.showWarning("Error", "Server error.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertHelper.showWarning("Error", "Error: " + e.getMessage()));
            }
        }).start();
    }

    // ==================== DELETION HANDLERS ====================

    private void onDeleteSite(Site site) {
        if (site == null || currentCity == null) return;

        if (isTempSite(site)) {
            removeTempItem("Site", site.getName(), () -> {
                citySites.remove(site);
                newSites.remove(site);
                String prefix = "site_" + site.getId();
                fieldRows.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
                originalValues.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
                currentEditingSite = null;
                vboxSiteEditor.getChildren().clear();
                vboxSiteEditor.getChildren().add(createPlaceholderLabel("Select a site to edit"));
            });
        } else {
            String json = "{\"name\":\"" + escapeJson(site.getName()) + "\"}";
            submitDeletion("Site", ContentType.SITE, site.getId(), site.getName(),
                    json, "Site deletion submitted for approval.");
        }
    }

    private void onDeleteTour(Tour tour) {
        if (tour == null || currentCity == null) return;

        if (isTempTour(tour)) {
            removeTempItem("Tour", tour.getName(), () -> {
                cityTours.remove(tour);
                newTours.remove(tour);
                String prefix = "tour_" + tour.getId();
                fieldRows.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
                originalValues.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
                savedTourSitesPerTour.remove(tour.getId());
                currentEditingTour = null;
                vboxTourEditor.getChildren().clear();
                vboxTourEditor.getChildren().add(createPlaceholderLabel("Select a tour to edit"));
            });
        } else {
            String json = "{\"name\":\"" + escapeJson(tour.getName()) + "\"}";
            submitDeletion("Tour", ContentType.TOUR, tour.getId(), tour.getName(),
                    json, "Tour deletion submitted for approval.");
        }
    }

    private void onDeleteMap(GCMMap map) {
        if (map == null || currentCity == null) return;

        AlertHelper.showConfirmation(
                "Confirm Map Deletion",
                "Delete Map: " + map.getName(),
                "This will revert the map to the external system, removing all GCM-specific data " +
                "(sites, markers, price). Continue?"
        ).ifPresent(response -> {
            if (response == ButtonType.OK) {
                String json = "{\"mapId\":" + map.getId() +
                        ",\"mapName\":\"" + escapeJson(map.getName()) +
                        "\",\"version\":\"" + escapeJson(map.getVersion()) + "\"}";
                submitContentChange(ContentActionType.DELETE, ContentType.MAP, map.getId(),
                        currentCity.getName() + " - " + map.getName(), json,
                        "Map deletion (revert to external) submitted for approval.");
            }
        });
    }

    private void removeTempItem(String entityType, String name, Runnable cleanupAction) {
        AlertHelper.showConfirmation(
                "Remove New " + entityType,
                "Remove: " + name,
                "This will discard the new " + entityType.toLowerCase() + "."
        ).ifPresent(response -> {
            if (response == ButtonType.OK) {
                cleanupAction.run();
                recalculateChangeCount();
            }
        });
    }

    private void submitDeletion(String entityType, ContentType contentType, int id, String name,
                                 String json, String successMsg) {
        AlertHelper.showConfirmation(
                "Confirm Deletion",
                "Delete " + entityType + ": " + name,
                "This will submit a deletion request for approval."
        ).ifPresent(response -> {
            if (response == ButtonType.OK) {
                submitContentChange(ContentActionType.DELETE, contentType, id,
                        currentCity.getName() + " - " + name, json, successMsg);
            }
        });
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

    private String buildMapJsonForMap(GCMMap map, String name, String description,
                                      List<SiteMarker> markers, List<Site> sites) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"mapName\":\"").append(escapeJson(name != null ? name : "")).append("\"");
        sb.append(",\"description\":\"").append(escapeJson(description != null ? description : "")).append("\"");

        // Site markers JSON (unquoted array)
        if (markers != null && !markers.isEmpty()) {
            StringBuilder markersSb = new StringBuilder("[");
            for (int i = 0; i < markers.size(); i++) {
                SiteMarker mk = markers.get(i);
                if (i > 0) markersSb.append(",");
                markersSb.append(String.format(java.util.Locale.US, "{\"siteId\":%d,\"x\":%.4f,\"y\":%.4f}",
                        mk.getSiteId(), mk.getX(), mk.getY()));
            }
            markersSb.append("]");
            sb.append(",\"siteMarkersJson\":").append(markersSb);
        }

        // Site IDs (comma-separated)
        if (sites != null && !sites.isEmpty()) {
            String siteIds = sites.stream()
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

    // ==================== UTILITIES ====================

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

    private boolean hasChangedFieldsWithPrefix(String prefix) {
        for (String key : fieldRows.keySet()) {
            if (key.startsWith(prefix) && fieldRows.get(key).getStyleClass().contains("field-row-changed")) {
                return true;
            }
        }
        return false;
    }

    private ContentChangeRequest makeChangeRequest(ContentActionType action, ContentType type,
                                                    int targetId, String targetName, String json) {
        User currentUser = client.getCurrentUser();
        Integer requesterId = currentUser != null ? currentUser.getId() : null;
        return new ContentChangeRequest(requesterId, action, type, targetId, targetName, json);
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
        pendingMarkersPerMap.clear();
        savedMapSitesPerMap.clear();
        savedTourSitesPerTour.clear();
        currentEditingMap = null;
        currentEditingSite = null;
        currentEditingTour = null;
        lvOnMapSites = null;
        currentMapOnMapSites = null;
        currentTourSites = null;
        newSites.clear();
        newTours.clear();
        tempSiteIdCounter = -1;
        tempTourIdCounter = -1;
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

    private String validateAllFields() {
        for (Map.Entry<String, HBox> entry : fieldRows.entrySet()) {
            String key = entry.getKey();

            // Only validate name and description fields
            boolean isNameField = key.equals("cityName")
                    || key.startsWith("mapName_")
                    || key.endsWith("_name");
            boolean isDescField = key.equals("cityDesc")
                    || key.startsWith("mapDesc_")
                    || key.endsWith("_desc");

            if (!isNameField && !isDescField) continue;

            // Get the text value from the row's TextField or TextArea
            String value = isDescField ? getTextAreaValue(key) : getFieldValue(key);
            if (value == null) continue;

            String label = getEntityLabelFromKey(key);

            if (value.trim().isEmpty()) {
                return label + " cannot be empty.";
            }
            if (value.startsWith(" ")) {
                return label + " cannot start with a space.";
            }
        }
        return null;
    }

    private String getEntityLabelFromKey(String key) {
        if (key.equals("cityName")) return "City name";
        if (key.equals("cityDesc")) return "City description";
        if (key.startsWith("mapName_")) return "Map name";
        if (key.startsWith("mapDesc_")) return "Map description";
        if (key.startsWith("site_") && key.endsWith("_name")) return "Site name";
        if (key.startsWith("site_") && key.endsWith("_desc")) return "Site description";
        if (key.startsWith("tour_") && key.endsWith("_name")) return "Tour name";
        if (key.startsWith("tour_") && key.endsWith("_desc")) return "Tour description";
        return "Field (" + key + ")";
    }
}
