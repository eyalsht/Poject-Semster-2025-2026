package controllers;

import client.GCMClient;
import common.content.City;
import common.content.Site;
import common.enums.ActionType;
import common.messaging.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CityUpdatePageController
{
    private String mode;
    private ObservableList<Site> availableSites = FXCollections.observableArrayList();
    private ObservableList<City> availableCities = FXCollections.observableArrayList();
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
        if (lvAvailableSites != null)
        {
            lvAvailableSites.setCellFactory(possibleSite -> new ListCell<Site>() {
                @Override
                protected void updateItem(Site item, boolean empty)
                {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
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
            case "create":
                configureAddMode();
                break;
        }
    }

    @FXML
    private void onAddSite()
    {
        String siteName = tfNewSite.getText().trim();
        if (siteName.length() >= 3 && !isSiteDuplicate(siteName) ) //make a verify site name method and replace the >=3 condition
        {
            City selectedCity = cbChooseCity.getSelectionModel().getSelectedItem();
            if (selectedCity == null) {
                showAlert("Error", "Please select a city first.");
                return;
            }
            Site newSite = new Site(siteName, selectedCity);
            availableSites.add(newSite);
            tfNewSite.clear();
        }
        else
            showAlert("Invalid Name", "The site name entered is not legal.");

    }
    @FXML
    private void onDeleteSite()
    {
        Site selectedSite = lvAvailableSites.getSelectionModel().getSelectedItem();
        if (selectedSite != null)
            availableSites.remove(selectedSite);
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
        System.out.println("Sumbit");
    }

    private void configureAddMode()
    {
        lblChooseCity.setText("Add City");
        lblChooseCity.setVisible(false);
        cbChooseCity.setVisible(false);
        btnConfirmEdit.setVisible(false);

    }

    private void configureEditMode()
    {
        lblCurrentTitle.setText("Edit City");
        lblNewCity.setVisible(false);
        tfNewCity.setVisible(false);
        lblChooseCity.setVisible(true);
        cbChooseCity.setVisible(true);
        btnConfirmEdit.setVisible(false);
        cbChooseCity.getSelectionModel().selectedItemProperty().addListener((obs, oldCity, newCity) -> {
            if (newCity != null)
                updateSitesList(newCity.getName());
        });
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

    private boolean isSiteDuplicate(String name)
    {
        return availableSites.stream().anyMatch(site -> site.getName().equalsIgnoreCase(name));
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
                        if(cities != null)
                        {
                            this.availableCities.setAll(cities);
                            cbChooseCity.setItems(FXCollections.observableArrayList(availableCities));
                        }
                    }
                });
            }
            catch (Exception e)
            {
                Platform.runLater(() -> showAlert("Network Error", "Could not connect: " + e.getMessage()));
            }
        }).start();
    }
}