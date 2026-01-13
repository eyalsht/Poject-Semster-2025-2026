package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class ReportPageController
{
    @FXML
    private ComboBox<String> reportCombo;
    @FXML
    private ComboBox<String> cityCombo;

    @FXML
    public void initialize()
    {
        reportCombo.setItems(FXCollections.observableArrayList("Activity Report"));
        cityCombo.setDisable(true);
        cityCombo.getItems().clear();
        reportCombo.valueProperty().addListener((obs, oldVal, newVal) ->
        {
            boolean reportChosen = (newVal != null && !newVal.isBlank());
            cityCombo.setDisable(!reportChosen);
            if (!reportChosen)
            {
                cityCombo.getItems().clear();
                cityCombo.setValue(null);
            }
            loadCitiesIntoCityCombo();
        });
    }

    private void loadCitiesIntoCityCombo()
    {
        ObservableList<String> cities = getCitiesFromServerOrDB();
        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("All cities");
        items.addAll(cities);
        cityCombo.setItems(items);
        cityCombo.getSelectionModel().selectFirst();
    }

    private ObservableList<String> getCitiesFromServerOrDB()
    {
        return FXCollections.observableArrayList(
                "Haifa",
                "Tel Aviv",
                "Jerusalem"
        );
    }


}
