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
        // keep report names simple for now, but only "Clients report" will actually work
        cmbReportType.setItems(FXCollections.observableArrayList(
                "Clients report", "Sales report", "Purchases report", "Users report"
        ));
        cmbReportType.getSelectionModel().select("Clients report");

        // show City objects nicely in ComboBox
        cmbCity.setConverter(new StringConverter<>() {
            @Override public String toString(City city) {
                return (city == null) ? "" : city.getName();
            }
            @Override public City fromString(String s) { return null; }
        });

        setupClientsTableColumns();

        loadCitiesFromServer();   // ✅ this fills the city combo from DB using Hibernate (server side)
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

        if (report == null || report.last5Months == null) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("New clients");

        for (AllClientsReport.MonthCount mc : report.last5Months) {
            String label = (mc.month == null) ? "" : mc.month.toString(); // e.g. 2026-01
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
}
