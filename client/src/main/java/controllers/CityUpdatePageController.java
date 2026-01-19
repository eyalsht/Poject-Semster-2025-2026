package controllers;

import client.GCMClient;
import common.content.City;
import common.content.Site;
import common.dto.ContentChangeRequest;
import common.enums.ActionType;
import common.enums.ContentActionType;
import common.enums.ContentType;
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
    private String mode;
    private ObservableList<Site> availableSites = FXCollections.observableArrayList();
    private ObservableList<City> availableCities = FXCollections.observableArrayList();
    private ObservableList<Site> sitesToDelete = FXCollections.observableArrayList();
    private String prevSiteName;

    @FXML private Label lblCurrentTitle;
    @FXML private Label lblNewCity;
    @FXML private Label lblChooseCity;
    @FXML private Label lblSites;
    @FXML private Label lblSiteName;

    @FXML private TextField tfNewCity;
    @FXML private TextField tfNewSite;

    @FXML private ComboBox <City> cbChooseCity;

    @FXML private ListView<Site> lvAvailableSites;

    @FXML private Button btnEditSite;
    @FXML private Button btnDeleteSite;
    @FXML private Button btnAddSite;
    @FXML private Button btnConfirmEdit;
    @FXML private Button btnSubmit;


    //---------Factory design pattern -----------

    @FXML
    public void initialize()
    {
        System.out.println("Initializing CityUpdatePageController");
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
                if(newCity.getName().equals("----------Add city----------")) {
                    configureAddMode();
                }
                else
                {
                    configureEditMode();
                    updateSitesList(newCity.getName());
                }
            }
        });
    }

    public void setMode(String mode)
    {
        this.mode = mode;
        updateUIForMode();
    }

    private void updateUIForMode()
    {
        switch (mode) {
            case "edit":
                configureEditMode();
                break;
            case "add":
                configureAddMode();
                break;
        }
    }

    private void configureAddMode()
    {
        this.mode = "add";
        btnConfirmEdit.setVisible(false);
        tfNewCity.setDisable(false);
    }

    private void configureEditMode()
    {
        this.mode = "edit";
        tfNewCity.setDisable(true);
        btnConfirmEdit.setVisible(false);
    }

    @FXML
    private void onAddSite()
    {
        String siteName = tfNewSite.getText().trim();
        if (siteName.length() >= 3 && !isSiteDuplicate(siteName)) //make a verify site name method and replace the >=3 condition
        {
            if(this.mode.equals("add") && tfNewCity.getText().isEmpty())
            {
                showAlert("Error", "No city name input");
                return;
            }
            City selectedCity = this.mode.equals("add") ? new City(tfNewCity.getText().trim()) : cbChooseCity.getSelectionModel().getSelectedItem();
            if (selectedCity == null) {
                showAlert("Error", "Please select a city first.");
                return;
            }
            Site newSite = new Site(siteName, selectedCity);
            availableSites.add(newSite);
            lvAvailableSites.refresh();
            tfNewSite.clear();
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
            tfNewSite.setText(selectedSite.getName());
            prevSiteName = selectedSite.getName();
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
        if (newName.length() < 3 && isSiteDuplicate(newName)) {
            showAlert("Invalid Name", "Name must be at least 3 characters.");
            return;
        }
        // Update the existing object in the list
        Site toEdit = availableSites.stream()
                .filter(s -> s.getName().equalsIgnoreCase(prevSiteName))
                .findFirst()
                .orElse(null);

        toEdit.setName(newName);
        lvAvailableSites.refresh();
        tfNewSite.clear();
        btnConfirmEdit.setVisible(false);
        btnAddSite.setDisable(false);
        btnEditSite.setDisable(false);
        btnDeleteSite.setDisable(false);
    }

    @FXML
    private void onSubmit()
    {
        new Thread(() -> {
            try {
                if ("add".equals(mode))
                    onSubmitAdd();
                else
                {
                    onSubmitEdit();
                    onSubmitDelete();
                }
                Platform.runLater(() -> {
                    showAlert("Success", "All changes submitted for approval.");
                    onClose();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Submission failed: " + e.getMessage()));
            }
        }).start();
    }

    private void onSubmitAdd()
    {
        if(this.mode.equals("edit"))
            return;
        User currentUser = GCMClient.getInstance().getCurrentUser();
        Integer requesterId = (currentUser != null) ? currentUser.getId() : null;
        try
        {
            String cityName = tfNewCity.getText().trim();
            if (cityName.isEmpty())
            {
                showAlert("Error", "Enter city name.");
                return;
            }
            String Json = buildFullCityJson(cityName, new ArrayList<>(availableSites));
            ContentChangeRequest req = new ContentChangeRequest(
                    requesterId,
                    ContentActionType.ADD,
                    ContentType.CITY,
                    null,
                    cityName,
                    Json);
            Message request = new Message(ActionType.SUBMIT_CONTENT_CHANGE_REQUEST, req);
            Message response = (Message) client.sendRequest(request);

            Platform.runLater(() -> handleContentChangeResponse(response, ContentActionType.ADD));
            showAlert("Success", "New city and its sites submitted as one package.");
        }
        catch (Exception e)
        {
            Platform.runLater(() -> showAlert("Error", "Error: " + e.getMessage()));
        }
    }

    private void onSubmitEdit()
    {
        if(this.mode.equals("add"))
            return;
        User currentUser = GCMClient.getInstance().getCurrentUser();
        Integer requesterId = (currentUser != null) ? currentUser.getId() : null;
        City selectedCity = cbChooseCity.getSelectionModel().getSelectedItem();
        if (selectedCity == null)
            return;
        try
        {
            for (Site site : availableSites)
            {
                ContentActionType action = (site.getID() <= 0) ? ContentActionType.ADD : ContentActionType.EDIT;
                String Json = buildSiteJson(site);
                String targetName = site.getCityName() + " - " + site.getName();
                Integer targetId = (action == ContentActionType.ADD) ? selectedCity.getId() : site.getID();
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
                Platform.runLater(() -> handleContentChangeResponse(response, ContentActionType.EDIT));
            }
        }
        catch (Exception e)
        {
            Platform.runLater(() -> showAlert("Error", "Error: " + e.getMessage()));
        }

    }

    private void onSubmitDelete()
    {
        if(this.mode.equals("add"))
            return;
        User currentUser = GCMClient.getInstance().getCurrentUser();
        Integer requesterId = (currentUser != null) ? currentUser.getId() : null;
        for (Site site : sitesToDelete) {
            String json = buildSiteJson(site);
            String targetName = site.getCityName() + " - " + site.getName();
            ContentChangeRequest req = new ContentChangeRequest(
                    requesterId,
                    ContentActionType.DELETE,
                    ContentType.SITE,
                    site.getID(),
                    targetName,
                    json
            );
            client.sendRequest(new Message(ActionType.SUBMIT_CONTENT_CHANGE_REQUEST, req));
        }
        sitesToDelete.clear();
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
        System.out.println(">>> [CATALOG] Calling getCitiesComboBox()");
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
                        createCityOption.setName("----------Add city----------");
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
        json.append("\"name\":\"").append(site.getName().replace("\"", "\\\"")).append("\",");

        int cId = (site.getCity() != null) ? site.getCity().getId() : 0;
        json.append("\"cityId\":").append(cId);
        json.append("}");
        return json.toString();
    }

    private String buildFullCityJson(String cityName, List<Site> sites)
    {
        StringBuilder json = new StringBuilder("{");

        json.append("\"cityName\":\"").append(cityName.replace("\"", "\\\"")).append("\",");
        json.append("\"sites\":[");
        for (int i = 0; i < sites.size(); i++) {
            Site s = sites.get(i);
            json.append("{");
            json.append("\"name\":\"").append(s.getName().replace("\"", "\\\"")).append("\",");
            json.append("\"id\":").append(s.getID());
            json.append("}");

            if (i < sites.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        json.append("}");
        return json.toString();
    }

    private void handleContentChangeResponse(Message response, ContentActionType actionType) {
        if (response == null) {
            showAlert("Error", "No response from server.");
            return;
        }

        if (response.getAction() == ActionType.SUBMIT_CONTENT_CHANGE_RESPONSE) {
            boolean success = (Boolean) response.getMessage();
            if (success) {
                String actionName = actionType == ContentActionType.ADD ? "addition" : "update";
                showAlert("Success", "Map " + actionName + " submitted for approval!");
                onClose();
            } else {
                showAlert("Error", "Failed to submit content change.");
            }
        } else if (response.getAction() == ActionType.ERROR) {
            showAlert("Error", response.getMessage().toString());
        } else {
            showAlert("Error", "Unexpected response from server.");
        }
    }
    @FXML
    private void onClose() {
        Stage stage = (Stage) btnSubmit.getScene().getWindow();
        stage.close();
    }
}