package controllers;

import client.GCMClient;
import common.content.City;
import common.enums.ActionType;
import common.messaging.Message;
import common.report.AllClientsReport;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportPageController {

    @FXML private ComboBox<String> cmbReportType;
    @FXML private ComboBox<City> cmbCity;
    @FXML private Button btnGenerate;

    @FXML private BarChart<String, Number> barChart;

    @FXML private TableView<AllClientsReport.ClientRow> tableView;
    @FXML private TableColumn<AllClientsReport.ClientRow, Number> colId;
    @FXML private TableColumn<AllClientsReport.ClientRow, String> colUsername;
    @FXML private TableColumn<AllClientsReport.ClientRow, String> colEmail;
    @FXML private TableColumn<AllClientsReport.ClientRow, String> colFirstName;
    @FXML private TableColumn<AllClientsReport.ClientRow, String> colLastName;
    @FXML private TableColumn<AllClientsReport.ClientRow, String> colCreatedAt;

    private static final DateTimeFormatter CREATED_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {

        // report options stay the same
        cmbReportType.setItems(FXCollections.observableArrayList(
                "Clients report", "Sales report", "Purchases report", "Users report"
        ));

        // ✅ Start state: only report enabled
        cmbReportType.getSelectionModel().clearSelection();
        cmbReportType.setDisable(false);

        cmbCity.setDisable(true);
        btnGenerate.setDisable(true);

        // converter stays
        cmbCity.setConverter(new StringConverter<>() {
            @Override public String toString(City city) {
                return (city == null) ? "" : city.getName();
            }
            @Override public City fromString(String s) { return null; }
        });

        setupClientsTableColumns();

        // ✅ listeners that enable/disable controls based on selection
        wireUiStateListeners();
        applyUiState();

        // keep your existing city loading
        loadCitiesFromServer();
    }


    private void setupClientsTableColumns() {
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().userId));
        colUsername.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().username));
        colEmail.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().email));
        colFirstName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().firstName));
        colLastName.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().lastName));
        colCreatedAt.setCellValueFactory(data -> {
            var dt = data.getValue().createdAt;
            return new javafx.beans.property.SimpleStringProperty(dt == null ? "" : CREATED_FMT.format(dt));
        });
    }

    private void loadCitiesFromServer() {
        new Thread(() -> {
            try {
                Message req = new Message(ActionType.GET_CITIES_REQUEST, null);
                Message res = (Message) GCMClient.getInstance().sendRequest(req);

                Platform.runLater(() -> {
                    if (res != null && res.getAction() == ActionType.GET_CITIES_RESPONSE) {
                        List<City> cities = (List<City>) res.getMessage();
                        cmbCity.setItems(FXCollections.observableArrayList(cities));
                        if (cities != null && !cities.isEmpty()) cmbCity.getSelectionModel().select(0);
                    } else {
                        showAlert("Error", "Failed to load cities (GET_CITIES_RESPONSE not received).");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Network Error", "Could not load cities: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onGenerate() {
        String selected = cmbReportType.getValue();
        if (!"Clients report".equals(selected)) {
            showAlert("Not implemented yet", "Only 'Clients report' is implemented right now.");
            return;
        }

        generateClientsReport();
    }

    private void generateClientsReport() {
        // (City not used for this report right now, but we already load it for later)
        new Thread(() -> {
            try {
                Message req = new Message(ActionType.GET_ALL_CLIENTS_REPORT_REQUEST, null);
                Message res = (Message) GCMClient.getInstance().sendRequest(req);

                Platform.runLater(() -> {
                    if (res != null && res.getAction() == ActionType.GET_ALL_CLIENTS_REPORT_RESPONSE) {
                        AllClientsReport report = (AllClientsReport) res.getMessage();
                        fillClientsTable(report);
                        fillClientsBarChart(report);
                    } else {
                        showAlert("Error", "Failed to generate clients report.");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Network Error", "Could not generate report: " + e.getMessage()));
            }
        }).start();
    }

    private void fillClientsTable(AllClientsReport report) {
        if (report == null || report.clientsNewestFirst == null) {
            tableView.setItems(FXCollections.observableArrayList());
            return;
        }
        tableView.setItems(FXCollections.observableArrayList(report.clientsNewestFirst));
    }

    private void fillClientsBarChart(AllClientsReport report) {
        barChart.getData().clear();
        barChart.setAnimated(false);

        if (report == null || report.last5Months == null) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("New clients");

        for (AllClientsReport.MonthCount mc : report.last5Months) {
            String label = mc.month.getMonthValue() + "/" + mc.month.getYear(); // e.g. 2026-01
            series.getData().add(new XYChart.Data<>(label, mc.count));
        }

        barChart.getData().add(series);
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void applyUiState() {
        // Default: only report enabled
        String report = cmbReportType.getValue();

        boolean hasReport = report != null && !report.isBlank();
        boolean isClients = "Clients report".equals(report);

        // City enabled only for non-clients reports (future reports)
        cmbCity.setDisable(!hasReport || isClients);

        // Generate enabled:
        // - Clients report: enabled as soon as selected
        // - Others: enabled only when city is selected
        boolean cityChosen = cmbCity.getValue() != null;
        btnGenerate.setDisable(!hasReport || (!isClients && !cityChosen));
    }

    private void wireUiStateListeners() {
        cmbReportType.valueProperty().addListener((obs, oldV, newV) -> {
            // if switching to Clients report, clear city selection (optional but clean)
            if ("Clients report".equals(newV)) {
                cmbCity.getSelectionModel().clearSelection();
            }
            applyUiState();
        });

        cmbCity.valueProperty().addListener((obs, oldV, newV) -> applyUiState());
    }

}
