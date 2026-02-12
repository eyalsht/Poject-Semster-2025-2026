package controllers;

import client.GCMClient;
import common.content.City;
import common.enums.ActionType;
import common.messaging.Message;
import common.report.AllClientsReport;
import common.report.ActivityReport;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import javafx.scene.layout.StackPane;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import common.dto.SubscriptionStatusDTO;
import common.purchase.PurchasedMapSnapshot;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

public class ReportPageController
{

    @FXML private ComboBox<String> cmbReportType;
    @FXML private ComboBox<City> cmbCity;
    @FXML private Button btnGenerate;

    @FXML private Label lblChooseReport;
    @FXML private HBox reportContent;

    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;

    @FXML private BarChart<String, Number> barChart;
    @FXML private StackPane reportArea;

    @FXML private TableView<AllClientsReport.ClientRow> tableView;
    @FXML private TableColumn<AllClientsReport.ClientRow, Number> colId;
    @FXML private TableColumn<AllClientsReport.ClientRow, String> colUsername;
    @FXML private TableColumn<AllClientsReport.ClientRow, String> colEmail;
    @FXML private TableColumn<AllClientsReport.ClientRow, String> colFirstName;
    @FXML private TableColumn<AllClientsReport.ClientRow, String> colLastName;
    @FXML private TableColumn<AllClientsReport.ClientRow, String> colCreatedAt;

    @FXML private Button btnExportPdf;

    private long currentRequestId = 0;


    private static final DateTimeFormatter CREATED_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ✅ The report currently shown on screen (changes only after Generate)
    private String displayedReport = null;

    @FXML
    public void initialize()
    {

        cmbReportType.setItems(FXCollections.observableArrayList(
                "Clients report",
                "Sales report",
                "Purchases report",
                "Users report",
                "Activity report"
        ));

        // Start state
        cmbReportType.getSelectionModel().clearSelection();
        cmbReportType.setDisable(false);

        cmbCity.setDisable(true);
        btnGenerate.setDisable(true);

        cmbCity.setConverter(new StringConverter<>() {
            @Override public String toString(City city) {
                return (city == null) ? "" : city.getName();
            }
            @Override public City fromString(String s) { return null; }

        });

        setupClientsTableColumns();
        setupClientsTableInteractions();

        wireUiStateListeners();
        applyUiState();

        // ✅ initial view: placeholder only
        displayedReport = null;
        applyReportLayout();

        loadCitiesFromServer();

        dpFrom.setDisable(true);
        dpTo.setDisable(true);

        // nice defaults (last 7 days)
        dpTo.setValue(LocalDate.now());
        dpFrom.setValue(LocalDate.now().minusDays(6));

        barChart.prefHeightProperty().bind(reportArea.heightProperty());
        tableView.prefHeightProperty().bind(reportArea.heightProperty());
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
        if (selected == null || selected.isBlank()) return;

        // ✅ NEW: create a unique request id for THIS generate click
        long reqId = ++currentRequestId;

        displayedReport = selected;
        applyReportLayout();

        barChart.getData().clear();
        tableView.getItems().clear();

        lblChooseReport.setText("Loading...");
        lblChooseReport.setVisible(true);
        lblChooseReport.setManaged(true);

        switch (selected) {
            case "Clients report" -> generateClientsReport(reqId);
            case "Activity report" -> generateActivityReport(reqId);
            case "Purchases report" -> generatePurchasesReport(reqId);
            default -> {
                lblChooseReport.setText("This report is not implemented yet.");
                lblChooseReport.setVisible(true);
                lblChooseReport.setManaged(true);

                reportContent.setVisible(false);
                reportContent.setManaged(false);
            }
        }
    }


    private void generateClientsReport(long reqId)
    {
        new Thread(() -> {
            try {
                Message req = new Message(ActionType.GET_ALL_CLIENTS_REPORT_REQUEST, null);
                Message res = (Message) GCMClient.getInstance().sendRequest(req);

                Platform.runLater(() -> {

                    if (res != null && res.getAction() == ActionType.GET_ALL_CLIENTS_REPORT_RESPONSE) {
                        AllClientsReport report = (AllClientsReport) res.getMessage();

                        // Hide loading text
                        lblChooseReport.setVisible(false);
                        lblChooseReport.setManaged(false);

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
            String label = mc.month.getMonthValue() + "/" + mc.month.getYear();
            series.getData().add(new XYChart.Data<>(label, mc.count));
        }

        barChart.getData().add(series);
    }

    private void generateActivityReport(long reqId)
    {
        City city = cmbCity.getValue();
        if (city == null) {
            showAlert("Missing city", "Please choose a city for Activity report.");
            return;
        }

        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();

        if (from == null || to == null) {
            showAlert("Missing dates", "Please choose From and To dates.");
            return;
        }
        if (from.isAfter(to)) {
            showAlert("Invalid dates", "'From' must be before (or equal to) 'To'.");
            return;
        }

        new Thread(() -> {
            try {
                ArrayList<Object> payload = new ArrayList<>();
                payload.add(from);
                payload.add(to);
                payload.add(city.getId());

                Message req = new Message(ActionType.GET_ACTIVITY_REPORT_REQUEST, payload);
                Message res = (Message) GCMClient.getInstance().sendRequest(req);

                Platform.runLater(() -> {
                    if (res != null && res.getAction() == ActionType.GET_ACTIVITY_REPORT_RESPONSE) {
                        ActivityReport report = (ActivityReport) res.getMessage();

                        // Hide loading text
                        lblChooseReport.setVisible(false);
                        lblChooseReport.setManaged(false);

                        fillActivityBarChart(report);

                    } else {
                        showAlert("Error", "Failed to generate activity report.");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Network Error", "Could not generate activity report: " + e.getMessage()));
            }
        }).start();
    }

    private void fillActivityBarChart(ActivityReport report)
    {
        barChart.getData().clear();
        barChart.setAnimated(false);

        if (report == null || report.rows == null || report.rows.isEmpty()) return;

        ActivityReport.CityRow row = report.rows.get(0);

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Activity (" + row.cityName + ")");

        s.getData().add(new XYChart.Data<>("Maps", row.maps));
        s.getData().add(new XYChart.Data<>("One-time", row.oneTimePurchases));
        s.getData().add(new XYChart.Data<>("Subscriptions", row.subscriptions));
        s.getData().add(new XYChart.Data<>("Renewals", row.renewals));
        s.getData().add(new XYChart.Data<>("Views", row.views));
        s.getData().add(new XYChart.Data<>("Downloads", row.downloads));

        barChart.getData().add(s);
    }

    private void showAlert(String title, String msg)
    {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void applyUiState()
    {
        String report = cmbReportType.getValue();

        boolean hasReport = report != null && !report.isBlank();
        boolean isClients = "Clients report".equals(report);

        boolean supportsDateRange = "Activity report".equals(report) || "Purchases report".equals(report);

        // Enable/disable date pickers
        dpFrom.setDisable(!supportsDateRange);
        dpTo.setDisable(!supportsDateRange);

        // City enabled only for non-clients reports
        cmbCity.setDisable(!hasReport || isClients);

        boolean cityChosen = cmbCity.getValue() != null;

        // Date validation only when date range is required
        boolean datesOk = true;
        if (supportsDateRange) {
            LocalDate from = dpFrom.getValue();
            LocalDate to = dpTo.getValue();

            datesOk = (from != null && to != null && !from.isAfter(to));
        }

        // Generate enabled:
        // - Clients report: as soon as selected
        // - Others: need city, and if date-range report -> valid dates too
        btnGenerate.setDisable(!hasReport || (!isClients && (!cityChosen || !datesOk)));
    }


    // ✅ Layout is based on displayedReport (changes only after Generate)
    private void applyReportLayout()
    {
        String report = displayedReport;

        boolean hasReport = report != null && !report.isBlank();
        boolean isClients = "Clients report".equals(report);
        boolean isActivity = "Activity report".equals(report);
        boolean isPurchases = "Purchases report".equals(report);

        // Placeholder visible only when no displayed report
        lblChooseReport.setVisible(!hasReport);
        lblChooseReport.setManaged(!hasReport);

        // Content visible only when a report is displayed
        reportContent.setVisible(hasReport);
        reportContent.setManaged(hasReport);

        barChart.setVisible(false);
        barChart.setManaged(false);

        tableView.setVisible(false);
        tableView.setManaged(false);

        if (isClients) {
            barChart.setVisible(true);
            barChart.setManaged(true);
            barChart.setPrefWidth(285);

            tableView.setVisible(true);
            tableView.setManaged(true);

            reportContent.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            reportContent.setSpacing(20);

        } else if (isActivity || isPurchases) {
            barChart.setVisible(true);
            barChart.setManaged(true);
            barChart.setPrefWidth(650);

            reportContent.setAlignment(javafx.geometry.Pos.CENTER);
            reportContent.setSpacing(0);

            tableView.getItems().clear();

        } else if (hasReport) {
            lblChooseReport.setText("This report is not implemented yet.");
            lblChooseReport.setVisible(true);
            lblChooseReport.setManaged(true);
            reportContent.setVisible(false);
            reportContent.setManaged(false);
        } else {
            lblChooseReport.setText("Choose a report!");
        }
    }

    private void wireUiStateListeners()
    {

        cmbReportType.valueProperty().addListener((obs, oldV, newV) -> {

            // Keep city selection logic (but do NOT change layout here)
            if ("Clients report".equals(newV)) {
                cmbCity.getSelectionModel().clearSelection();
            } else {
                if (cmbCity.getValue() == null && cmbCity.getItems() != null && !cmbCity.getItems().isEmpty()) {
                    cmbCity.getSelectionModel().select(0);
                }
            }

            applyUiState();
            // ❌ no applyReportLayout() here (layout changes only on Generate)
        });

        cmbCity.valueProperty().addListener((obs, oldV, newV) -> applyUiState());

        dpFrom.valueProperty().addListener((obs, oldV, newV) -> applyUiState());
        dpTo.valueProperty().addListener((obs, oldV, newV) -> applyUiState());
    }
    private void generatePurchasesReport(long reqId) {

        City city = cmbCity.getValue();
        if (city == null) {
            showAlert("Missing city", "Please choose a city for Purchases report.");
            return;
        }

        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();

        if (from == null || to == null) {
            showAlert("Missing dates", "Please choose From and To dates.");
            return;
        }
        if (from.isAfter(to)) {
            showAlert("Invalid dates", "'From' must be before (or equal to) 'To'.");
            return;
        }

        new Thread(() -> {
            try {
                ArrayList<Object> payload = new ArrayList<>();
                payload.add(from);
                payload.add(to);
                payload.add(city.getId());

                Message req = new Message(ActionType.GET_PURCHASES_REPORT_REQUEST, payload);
                Message res = (Message) GCMClient.getInstance().sendRequest(req);

                Platform.runLater(() -> {

                    if (res != null && res.getAction() == ActionType.GET_PURCHASES_REPORT_RESPONSE) {
                        common.report.PurchasesReport report = (common.report.PurchasesReport) res.getMessage();
                        fillPurchasesBarChart(report);

                        lblChooseReport.setVisible(false);
                        lblChooseReport.setManaged(false);
                    } else {
                        if (reqId == currentRequestId)
                        showAlert("Error", "Failed to generate purchases report.");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Network Error", "Could not generate report: " + e.getMessage()));
            }
        }).start();
    }
    private void fillPurchasesBarChart(common.report.PurchasesReport report) {
        barChart.getData().clear();
        barChart.setAnimated(false);

        if (report == null) return;

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Purchases (" + report.cityName + ")");

        s.getData().add(new XYChart.Data<>("One-time", report.oneTime));
        s.getData().add(new XYChart.Data<>("Subscriptions", report.subscriptions));
        s.getData().add(new XYChart.Data<>("Renewals", report.renewals));

        barChart.getData().add(s);
    }

    @FXML
    private void onExportPdf() {

        // You export exactly what user sees in the report area (chart + table)
        if (reportArea == null) {
            showAlert("Error", "Nothing to export.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Save report as PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));
        fc.setInitialFileName("report.pdf");

        File out = fc.showSaveDialog(reportArea.getScene().getWindow());
        if (out == null) return;

        try {
            // 1) snapshot of the report area
            SnapshotParameters params = new SnapshotParameters();
            WritableImage fxImg = reportArea.snapshot(params, null);
            BufferedImage bImg = SwingFXUtils.fromFXImage(fxImg, null);

            // 2) create pdf and draw the image
            try (PDDocument doc = new PDDocument()) {

                // A4 page
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                var pdImage = LosslessFactory.createFromImage(doc, bImg);

                float pageW = page.getMediaBox().getWidth();
                float pageH = page.getMediaBox().getHeight();

                float imgW = pdImage.getWidth();
                float imgH = pdImage.getHeight();

                // scale to fit page with margins
                float margin = 36; // 0.5 inch
                float maxW = pageW - 2 * margin;
                float maxH = pageH - 2 * margin;

                float scale = Math.min(maxW / imgW, maxH / imgH);

                float drawW = imgW * scale;
                float drawH = imgH * scale;

                float x = (pageW - drawW) / 2;
                float y = (pageH - drawH) / 2;

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(pdImage, x, y, drawW, drawH);
                }

                doc.save(out);
            }

            showAlert("Saved", "PDF exported:\n" + out.getAbsolutePath());

        } catch (IOException ex) {
            showAlert("Export failed", ex.getMessage());
        }
    }

    private void setupClientsTableInteractions() {
        tableView.setRowFactory(tv -> {
            TableRow<AllClientsReport.ClientRow> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    AllClientsReport.ClientRow selected = row.getItem();
                    openClientPurchaseHistory(selected);
                }
            });
            return row;
        });
    }

    private void openClientPurchaseHistory(AllClientsReport.ClientRow clientRow) {
        if (clientRow == null) return;

        int userId = clientRow.userId;

        new Thread(() -> {
            try {
                // 1) subscriptions
                Message subReq = new Message(ActionType.GET_USER_SUBSCRIPTIONS_REQUEST, userId);
                Message subRes = (Message) GCMClient.getInstance().sendRequest(subReq);

                List<SubscriptionStatusDTO> subs = java.util.Collections.emptyList();
                if (subRes != null && subRes.getAction() == ActionType.GET_USER_SUBSCRIPTIONS_RESPONSE) {
                    subs = (List<SubscriptionStatusDTO>) subRes.getMessage();
                }

                // 2) purchases
                Message purReq = new Message(ActionType.GET_USER_PURCHASED_MAPS_REQUEST, userId);
                Message purRes = (Message) GCMClient.getInstance().sendRequest(purReq);

                List<PurchasedMapSnapshot> purchases = java.util.Collections.emptyList();
                if (purRes != null && purRes.getAction() == ActionType.GET_USER_PURCHASED_MAPS_RESPONSE) {
                    purchases = (List<PurchasedMapSnapshot>) purRes.getMessage();
                }

                List<SubscriptionStatusDTO> finalSubs = (subs == null) ? java.util.Collections.emptyList() : subs;
                List<PurchasedMapSnapshot> finalPurchases = (purchases == null) ? java.util.Collections.emptyList() : purchases;

                Platform.runLater(() -> showClientPurchaseHistoryDialog(clientRow, finalSubs, finalPurchases));

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", "Could not load purchase history: " + e.getMessage()));
            }
        }).start();
    }

    private void showClientPurchaseHistoryDialog(AllClientsReport.ClientRow clientRow,
                                                 List<SubscriptionStatusDTO> subscriptions,
                                                 List<PurchasedMapSnapshot> purchases) {

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Client purchase history");

        String displayName = (clientRow.firstName != null && !clientRow.firstName.isBlank())
                ? clientRow.firstName
                : clientRow.username;

        Label title = new Label(displayName + ", here is your purchase history");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // LEFT: Subscriptions
        TableView<SubscriptionStatusDTO> tblSubs = new TableView<>();
        tblSubs.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<SubscriptionStatusDTO, String> colCity = new TableColumn<>("City");
        colCity.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCityName() == null ? "" : d.getValue().getCityName()
        ));

        TableColumn<SubscriptionStatusDTO, String> colExpiry = new TableColumn<>("Expires");
        colExpiry.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getExpirationDate() == null ? "" : d.getValue().getExpirationDate().toString()
        ));

        TableColumn<SubscriptionStatusDTO, String> colActive = new TableColumn<>("Active");
        colActive.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().isActive() ? "YES" : "NO"
        ));

        TableColumn<SubscriptionStatusDTO, String> colPrice = new TableColumn<>("Price/mo");
        colPrice.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.2f", d.getValue().getPricePerMonth())
        ));

        tblSubs.getColumns().addAll(colCity, colExpiry, colActive, colPrice);
        tblSubs.setItems(FXCollections.observableList(subscriptions));

        VBox left = new VBox(8, new Label("Subscriptions"), tblSubs);
        left.setPrefWidth(430);

        // RIGHT: One-time purchases
        TableView<PurchasedMapSnapshot> tblMaps = new TableView<>();
        tblMaps.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<PurchasedMapSnapshot, String> colPCity = new TableColumn<>("City");
        colPCity.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getCityName() == null ? "" : d.getValue().getCityName()
        ));

        TableColumn<PurchasedMapSnapshot, String> colMap = new TableColumn<>("Map");
        colMap.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getMapName() == null ? "" : d.getValue().getMapName()
        ));

        TableColumn<PurchasedMapSnapshot, String> colVer = new TableColumn<>("Version");
        colVer.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPurchasedVersion() == null ? "" : d.getValue().getPurchasedVersion()
        ));

        TableColumn<PurchasedMapSnapshot, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getPurchaseDate() == null ? "" : d.getValue().getPurchaseDate().toString()
        ));

        TableColumn<PurchasedMapSnapshot, String> colPaid = new TableColumn<>("Paid");
        colPaid.setCellValueFactory(d -> new SimpleStringProperty(
                String.format("%.2f", d.getValue().getPricePaid())
        ));

        tblMaps.getColumns().addAll(colPCity, colMap, colVer, colDate, colPaid);
        tblMaps.setItems(FXCollections.observableList(purchases));

        VBox right = new VBox(8, new Label("One-time purchases"), tblMaps);
        right.setPrefWidth(430);

        HBox tables = new HBox(12, left, right);

        VBox root = new VBox(12, title, tables);
        root.setPrefSize(920, 520);

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
}
