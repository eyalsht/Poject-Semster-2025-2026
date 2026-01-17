package controllers;

import client.GCMClient;
import common.content.Site;
import common.dto.GetSitesResponse;
import common.enums.ActionType;
import common.messaging.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class CityUpdatePageController {

    private final GCMClient client = GCMClient.getInstance();
    private String mode;
    private CatalogPageController catalogController;
    private ObservableList<Site> availableSites = FXCollections.observableArrayList();
    private String prevSiteName;

    @FXML private Label lblCurrentTitle;
    @FXML private Label lblNewCity;
    @FXML private Label lblChooseCity;
    @FXML private Label lblSites;
    @FXML private Label lblSiteName;

    @FXML private TextField tfNewCity;
    @FXML private TextField tfNewSite;

    @FXML private ComboBox <String> cbChooseCity;

    @FXML private ListView<Site> lvAvailableSites;

    @FXML private Button btnEditSite;
    @FXML private Button btnDeleteSite;
    @FXML private Button btnAddSite;
    @FXML private Button btnConfirmEdit;


    //---------Factory design pattern -----------

    @FXML
    public void initialize() {
        if (lvAvailableSites != null) {
            lvAvailableSites.setItems(availableSites);

            lvAvailableSites.setCellFactory(possibleSite -> new ListCell<Site>() {
                @Override
                protected void updateItem(Site item, boolean empty)
                {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getName());
                }
            });
        }
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
        if (siteName.length() >= 3 && !isSiteDuplicate(siteName)) //TODO - make a verify site name method and replace the >=3 condition
        {
            Site newSite = new Site();
            newSite.setName(siteName);
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

        if (catalogController != null && catalogController.getLastCatalogResponse() != null)
        {
            List<String> cities = catalogController.getLastCatalogResponse().getAvailableCities();
            cbChooseCity.setItems(FXCollections.observableArrayList(cities));
            cbChooseCity.getSelectionModel().selectedItemProperty().addListener((obs, oldCity, newCity) ->
            {
                if (newCity != null)
                    updateSitesList(newCity);
            });
        }
        else
        {
            System.out.println("one of them is null");

        }
    }
    private void updateSitesList(String cityName)
    {
        availableSites.clear();
        new Thread(() -> {
            try {
                // 1. Create message and send directly via Singleton client
                Message request = new Message(ActionType.GET_CITY_SITES_REQUEST, cityName);
                Message response = (Message) client.sendRequest(request);

                // 2. Process response on JavaFX thread
                Platform.runLater(() -> {
                    if (response != null && response.getAction() == ActionType.GET_CITY_SITES_RESPONSE) {
                        GetSitesResponse dto = (GetSitesResponse) response.getMessage();
                        if (dto != null && dto.getSites() != null) {
                            availableSites.setAll(dto.getSites());
                        }
                    } else {
                        showAlert("Error", "Failed to retrieve sites from server.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Network Error", "Could not connect: " + e.getMessage()));
            }
        }).start();
    }

    public void handleGetSitesResponse(GetSitesResponse response)
    {
        if (response != null && response.getSites() != null) {
            availableSites.setAll(response.getSites());
        }
    }

    public void setCatalogController(CatalogPageController controller) {this.catalogController = controller;}

    private boolean isSiteDuplicate(String name) {
        return availableSites.stream().anyMatch(s -> s.getName().equalsIgnoreCase(name));
    }

    private void showAlert(String title, String message)
    {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
