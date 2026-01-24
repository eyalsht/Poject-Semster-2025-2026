package controllers;

import client.GCMClient;
import common.content.City;
import common.content.Site;
import common.content.Tour;
import common.dto.ContentChangeRequest;
import common.enums.*;
import common.messaging.Message;
import common.user.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

public class CityUpdatePageController
{
    private final GCMClient client = GCMClient.getInstance();
    private ObservableList<Site> availableSites = FXCollections.observableArrayList();
    private ObservableList<City> availableCities = FXCollections.observableArrayList();
    private ObservableList<Site> sitesToDelete = FXCollections.observableArrayList();
    private ObservableList<Site> tourSitesToDelete = FXCollections.observableArrayList();
    private List<Site> modifiedSites = new ArrayList<>();


    private ObservableList<Tour> availableTours = FXCollections.observableArrayList();
    private ObservableList<Tour> toursToDelete = FXCollections.observableArrayList();
    private ObservableList<Site> allTourSites = FXCollections.observableArrayList();
    private List<Tour> modifiedTours = new ArrayList<>();

    private String prevSiteName;

    //Labels
    @FXML private Label lblChooseCity;
    @FXML private Label lblSites;
    @FXML private Label lblSiteName;

    //City Choice
    @FXML private ComboBox <City> cbChooseCity;

    //Site Details
    @FXML private TextField tfNewSite;
    @FXML private ComboBox<SiteCategory> categoryComboBox;
    @FXML private CheckBox Accessibility;
    @FXML private ComboBox <SiteDuration> visitDuration;
    @FXML private TextField tfLocation;
    @FXML private TextArea taDescription;
    @FXML private ListView<Site> lvAvailableSites;

    //Tours Section
    @FXML private ListView<Site> lvTours;
    @FXML private TextField tfNewTour;
    @FXML private TextArea taDescriptionTour;
    @FXML private ComboBox<Tour> cbChooseTour;
    @FXML private Button btnAddToTour;
    @FXML private Button btnRemoveFromTour;
    @FXML private Button btnConfirmEditTour;
    @FXML private Button btnAddTour;
    @FXML private Button btnEditTour;
    @FXML private Button btnDeleteTour;
    @FXML private Button btnCreateTour;

    @FXML private Label lblNewTour;
    @FXML private Label lblTours;

    @FXML private Button btnEditSite;
    @FXML private Button btnDeleteSite;
    @FXML private Button btnAddSite;
    @FXML private Button btnConfirmEdit;
    @FXML private Button btnSubmit;


    //---------Factory design pattern -----------

    @FXML
    public void initialize()
    {
        btnAddToTour.setDisable(true);
        btnRemoveFromTour.setDisable(true);
        for (SiteCategory category : SiteCategory.values())
            categoryComboBox.getItems().add(category);

        for (SiteDuration duration : SiteDuration.values())
            visitDuration.getItems().add(duration);

        getCitiesComboBox();
        if (lvAvailableSites != null)
        {
            lvAvailableSites.setCellFactory(lv -> new ListCell<Site>() {
                @Override
                protected void updateItem(Site item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.getName() == null) {
                        setText(null);
                    } else {
                        setText(item.getName());
                    }
                }
            });
        }
        lvAvailableSites.setItems(availableSites);
        if (lvTours != null) {
            lvTours.setCellFactory(lv -> new ListCell<Site>() {
                @Override
                protected void updateItem(Site item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.getName() == null) {
                        setText(null);
                    } else {
                        setText(item.getName());
                    }
                }
            });
            lvTours.setItems(allTourSites);
        }
        cbChooseCity.setConverter(new StringConverter<City>()
        {
            @Override
            public String toString(City city) {
                return (city == null) ? "" : city.getName();
            }

            @Override
            public City fromString(String string) {
                return null;
            }
        });
        cbChooseCity.getSelectionModel().selectedItemProperty().addListener((obs, oldCity, newCity) -> {
            if (newCity != null){
                if(newCity.getName().equals("--------Choose City--------")) {
                    availableSites.clear();
                }
                else
                {
                    updateSitesList(newCity.getName());
                    updateToursList(newCity.getName());
                }
            }
        });
        cbChooseTour.getSelectionModel().selectedItemProperty().addListener((obs, oldTour, newTour) -> {
            if (newTour != null) {
                btnAddToTour.setDisable(false);
                btnRemoveFromTour.setDisable(false);
                tfNewTour.clear();
                taDescriptionTour.clear();
                updateTourSitesList(newTour);
                allTourSites.clear();
            }
            else
            {
                btnAddToTour.setDisable(true);
                btnRemoveFromTour.setDisable(true);
                tfNewTour.clear();
                taDescriptionTour.clear();
            }
        });
    }
    @FXML
    private void onAddSite()
    {
        String siteName = tfNewSite.getText().trim();
        String siteLocation = tfLocation.getText().trim();
        SiteCategory category = categoryComboBox.getSelectionModel().getSelectedItem();
        boolean isAccessible = Accessibility.isSelected();
        SiteDuration duration = visitDuration.getSelectionModel().getSelectedItem();
        String description = taDescription.getText();

        if (siteName.length() >= 3 && !isSiteDuplicate(siteName) && siteLocation.length()>=3) //make a verify site name method and replace the >=3 condition
        {
            City selectedCity = cbChooseCity.getSelectionModel().getSelectedItem();
            if (selectedCity == null)
            {
                showAlert("Error", "Please select a city first.");
                return;
            }
            Site newSite = new Site(0,siteName, description,selectedCity,category,isAccessible,duration,siteLocation);
            availableSites.add(newSite);
            lvAvailableSites.refresh();
            boolean allgood;

        }
        else
            showAlert("Invalid Name", "The site name entered is not legal.");

    }
    @FXML
    private void onDeleteSite()
    {
        Site selectedSite = lvAvailableSites.getSelectionModel().getSelectedItem();
        if (selectedSite != null) {
            if(selectedSite.getID() > 0)
                sitesToDelete.add(selectedSite);

            availableSites.remove(selectedSite);
            lvAvailableSites.getSelectionModel().clearSelection();
            tfNewSite.clear();
        }
        else
            showAlert("Error", "Select a site to remove.");

    }
    @FXML
    private void onEditSite()
    {
        Site selectedSite = lvAvailableSites.getSelectionModel().getSelectedItem();
        if (selectedSite != null)
        {
            btnConfirmEdit.setVisible(true);

            tfNewSite.setText(selectedSite.getName());
            prevSiteName = selectedSite.getName();
            categoryComboBox.setValue(selectedSite.getCategory());
            Accessibility.setSelected(selectedSite.isAccessible());
            visitDuration.setValue(selectedSite.getRecommendedVisitDuration());
            tfLocation.setText(selectedSite.getLocation());
            taDescription.setText(selectedSite.getDescription());

            btnConfirmEdit.setVisible(true);
            btnEditSite.setDisable(true);
            btnDeleteSite.setDisable(true);
            btnAddSite.setDisable(true);
        }
        else
            showAlert("Error", "Select a site to edit.");
    }
    @FXML
    private void onConfirmEdit()
    {
        String newName = tfNewSite.getText().trim();
        if (newName.length() < 3) {
            showAlert("Invalid Name", "Name must be at least 3 characters.");
            return;
        }
        // Update the existing object in the list
        Site toEdit = availableSites.stream()
                .filter(s -> s.getName().equalsIgnoreCase(prevSiteName))
                .findFirst()
                .orElse(null);

        toEdit.setName(newName);
        if (toEdit.getID() > 0 && !modifiedSites.contains(toEdit))
            modifiedSites.add(toEdit);

        resetFields();
        btnConfirmEdit.setVisible(false);
        btnAddSite.setDisable(false);
        btnEditSite.setDisable(false);
        btnDeleteSite.setDisable(false);
    }
    @FXML
    private void onSubmit()
    {
        btnSubmit.setDisable(true);
        new Thread(() -> {
            try {
                boolean editResult = onSubmitEdit();
                boolean deleteResult = onSubmitDelete();

                Platform.runLater(() -> {
                    if (editResult && deleteResult) {
                        showAlert("Success", "All changes submitted for approval.");
                    } else {
                        showAlert("Error", "Some changes failed to submit. Please check your connection and try again.");
                        btnSubmit.setDisable(false);
                    }
                    onClose();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Error", "Fatal error during submission: " + e.getMessage());
                    btnSubmit.setDisable(false);
                });
            }
        }).start();
    }
    @FXML
    private void onClose()
    {
        Stage stage = (Stage) btnSubmit.getScene().getWindow();
        stage.close();
    }
    @FXML
    private void onAddToTour()
    {

        Site site = lvAvailableSites.getSelectionModel().getSelectedItem();
        if(site != null)
        {
            Tour currentTour = cbChooseTour.getSelectionModel().getSelectedItem();
            if(currentTour == null)
            {
                showAlert("Error","First choose tour or add a new tour to the list");
                return;
            }
            if(currentTour.doesSiteExist(site))
            {
                showAlert("Error","Chosen site is already part of the tour");
                return;
            }
            allTourSites.add(site);
            lvTours.refresh();

        }
        else
        {
            showAlert("Error","Choose a site to add to tour");
        }
    }
    @FXML
    private void onRemoveFromTour()
    {
        Site site = lvTours.getSelectionModel().getSelectedItem();
        if(site != null)
        {
            Tour currentTour = cbChooseTour.getSelectionModel().getSelectedItem();
            if(currentTour == null)
            {
                showAlert("Error", "First choose tour or add a new tour to the list");
                return;
            }
            allTourSites.remove(site);
            if(site.getID() > 0)
            {
                tourSitesToDelete.add(site);//TODO- when submiting remember to delete sites from the current tour
            }
        }
    }
    @FXML
    private void onCreateTour()
    {
        if(tfNewTour.getText().trim().length() < 3 || taDescriptionTour.getText().length() < 3
                || cbChooseCity.getSelectionModel().getSelectedItem().getTours().stream().anyMatch(s-> s.getName().equalsIgnoreCase(tfNewTour.getText().trim())))
        {
            showAlert("Error","illegal Tour name");
            return;
        }
        String tourName = tfNewTour.getText().trim();
        String tourDescription = taDescriptionTour.getText().trim();
        City selectedCity = cbChooseCity.getSelectionModel().getSelectedItem();
        Tour newTour = new Tour();
        newTour.setName(tourName);
        newTour.setDescription(tourDescription);
        newTour.setCity(selectedCity);
        availableTours.add(newTour);
        cbChooseTour.getItems().add(newTour);
        cbChooseTour.getSelectionModel().select(newTour);
        clearTourSwitch();
        System.out.println("Local Tour Created: " + tourName);
    }
    @FXML
    private void onAddTour()
    {

    }
    @FXML
    private void onDeleteTour()
    {

    }
    @FXML
    private void onEditTour()
    {

    }
    @FXML
    private void onConfirmEditTour()
    {

    }

    private boolean onSubmitEdit()
    {
        User currentUser = GCMClient.getInstance().getCurrentUser();
        Integer requesterId = (currentUser != null) ? currentUser.getId() : null;
        City selectedCity = cbChooseCity.getSelectionModel().getSelectedItem();
        boolean allGood = true;
        List<Site> sitesToSubmit = new ArrayList<>();

        for (Site s : availableSites)
            if (s.getID() <= 0)
                sitesToSubmit.add(s); // New sites

        sitesToSubmit.addAll(modifiedSites);
        if (selectedCity == null || selectedCity.equals("--------Choose City--------"))
            return false;
        try
        {
            for (Site site : sitesToSubmit)
            {
                ContentActionType action = (site.getID() <= 0) ? ContentActionType.ADD : ContentActionType.EDIT;
                String Json = buildSiteJson(site);
                String targetName = site.getCityName() +" - " + site.getName();
                Integer targetId = (action == ContentActionType.ADD) ? selectedCity.getId(): site.getID();
                ContentChangeRequest changeRequest = new ContentChangeRequest(
                        requesterId,
                        action,
                        ContentType.SITE,
                        targetId,
                        targetName,
                        Json
                );
                Message request = new Message(ActionType.SUBMIT_CONTENT_CHANGE_REQUEST, changeRequest);
                Message response = (Message) client.sendRequest(request);
                //Platform.runLater(() -> handleContentChangeResponse(response, ContentActionType.EDIT));
                if (response == null || response.getAction() == ActionType.ERROR) {
                    allGood = false;
                }
            }
        }
        catch (Exception e)
        {
            Platform.runLater(() -> showAlert("Error", "Error: " + e.getMessage()));
        }
        if (allGood)
            modifiedSites.clear();
        return allGood;
    }

    private boolean onSubmitDelete()
    {
        User currentUser = GCMClient.getInstance().getCurrentUser();
        Integer requesterId = (currentUser != null) ? currentUser.getId() : null;
        boolean allGood = true;
        List<Site> toDeleteCopy = new ArrayList<>(sitesToDelete);

        for (Site site : toDeleteCopy)
        {
            String json = buildSiteJson(site);
            String targetName = site.getCityName() + " - " + site.getName();
            ContentChangeRequest changeRequest = new ContentChangeRequest(
                    requesterId,
                    ContentActionType.DELETE,
                    ContentType.SITE,
                    site.getID(),
                    targetName,
                    json
            );
            Message request = new Message(ActionType.SUBMIT_CONTENT_CHANGE_REQUEST, changeRequest);
            Message response = (Message) client.sendRequest(request);
            if (response != null && response.getAction() != ActionType.ERROR) {
                sitesToDelete.remove(site);
            } else {
                allGood = false;
            }
        }
        return allGood;
    }

    private void updateSitesList(String cityName)
    {
        availableSites.clear();
        new Thread(() -> {
            try {
                Message request = new Message(ActionType.GET_CITY_SITES_REQUEST, cityName);
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

                Platform.runLater(() ->
                {
                    if (response != null && response.getAction() == ActionType.GET_CITY_SITES_RESPONSE)
                    {
                        List<Site> sites = (List<Site>) response.getMessage();
                        availableSites.clear();
                        availableSites.addAll(sites);
                    }
                    else
                    {
                        showAlert("Error", "Failed to retrieve sites from server.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Network Error", "Could not connect: " + e.getMessage()));
            }
        }).start();
    }

    private void updateToursList(String cityName)
    {
        new Thread(() -> {
            try {
                Message request = new Message(ActionType.GET_CITY_TOURS_REQUEST, cityName);
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getAction() == ActionType.GET_CITY_TOURS_RESPONSE) {
                        List<Tour> tours = (List<Tour>) response.getMessage();
                        cbChooseTour.getItems().clear();
                        if (tours != null)
                        {
                            cbChooseTour.getItems().addAll(tours);
                        }
                        cbChooseTour.setValue(null);
                    } else {
                        showAlert("Error", "Failed to retrieve tours for " + cityName);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Network Error", "Could not fetch tours: " + e.getMessage()));
            }
        }).start();
    }

    private void updateTourSitesList(Tour newTour)
    {
        if(newTour.getSites()!=null)
            allTourSites.addAll(newTour.getSites());

        lvTours.setItems(allTourSites);
    }

    private boolean isSiteDuplicate(String newName)
    {
        return availableSites.stream()
                .anyMatch(site -> site.getName().equalsIgnoreCase(newName));
    }

    private void showAlert(String title, String message)
    {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void getCitiesComboBox()
    {
        new Thread(() ->
        {
            try {
                Message request = new Message(ActionType.GET_CITIES_REQUEST,null);
                Message response = (Message) GCMClient.getInstance().sendRequest(request);


                Platform.runLater(() ->
                {
                    if (response != null && response.getAction() == ActionType.GET_CITIES_RESPONSE)
                    {
                        List<City> cities = (List<City>) response.getMessage();
                        City createCityOption = new City();
                        createCityOption.setName("--------Choose City--------");
                        createCityOption.setId(-1);
                        this.availableCities.clear();
                        this.availableCities.add(createCityOption);
                        if(cities != null)
                            this.availableCities.addAll(cities);

                        cbChooseCity.setItems(FXCollections.observableArrayList(availableCities));
                    }
                });
            }
            catch (Exception e)
            {
                Platform.runLater(() -> showAlert("Network Error", "Could not connect: " + e.getMessage()));
            }
        }).start();
    }

    private String buildSiteJson(Site site)
    {
        StringBuilder json = new StringBuilder("{");

        json.append("\"id\":").append(site.getID()).append(",");
        json.append("\"name\":\"").append(escapeJson(site.getName())).append("\",");
        json.append("\"description\":\"").append(escapeJson(site.getDescription())).append("\",");

        if (site.getCity() != null) {
            json.append("\"cityId\":").append(site.getCity().getId()).append(",");
            json.append("\"cityName\":\"").append(escapeJson(site.getCity().getName())).append("\",");
        } else {
            json.append("\"cityId\":0,");
            json.append("\"cityName\":\"Unknown\",");
        }

        json.append("\"category\":\"").append(site.getCategory()).append("\",");
        json.append("\"isAccessible\":").append(site.isAccessible()).append(",");
        json.append("\"recommendedVisitDuration\":\"").append(site.getRecommendedVisitDuration()).append("\",");

        json.append("\"location\":\"").append(escapeJson(site.getLocation())).append("\"");

        json.append("}");
        return json.toString();
    }

    private String escapeJson(String input)
    {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void resetFields()
    {
        Accessibility.setSelected(false);
        tfNewTour.clear();
        tfNewSite.clear();
        tfLocation.clear();
        taDescription.clear();
        categoryComboBox.getSelectionModel().clearSelection();
        visitDuration.getSelectionModel().clearSelection();
        tfNewTour.clear();
        taDescriptionTour.clear();
        cbChooseTour.getSelectionModel().clearSelection();
    }

    private void clearCitySwitch()
    {
        availableSites.clear();
        availableTours.clear();
        resetFields();
    }

    private void clearTourSwitch()
    {
        allTourSites.clear();
        toursToDelete.clear();
        modifiedTours.clear();
        taDescriptionTour.clear();
        tfNewTour.clear();
    }
}