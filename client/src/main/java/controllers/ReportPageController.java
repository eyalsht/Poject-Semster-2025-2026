package controllers;

import client.GCMClient;
import common.content.City;
import common.dto.SubscriptionStatusDTO;
import common.enums.ActionType;
import common.messaging.Message;
import common.purchase.PurchasedMapSnapshot;
import common.report.ActivityReport;
import common.report.AllClientsReport;
import common.report.SupportRequestsReport;
import common.support.SupportTicketRowDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportPageController {

    // ===== UI =====
    @FXML private ComboBox<String> cmbReportType;
    @FXML private ComboBox<City> cmbCity;
    @FXML private Button btnGenerate;

    @FXML private Label lblChooseReport;
    @FXML private HBox reportContent;

    @FXML private DatePicker dpFrom;
    @FXML private DatePicker dpTo;

    @FXML private BarChart<String, Number> barChart;
    @FXML private StackPane reportArea;

    // IMPORTANT: Generic table so CellValueFactory lambdas work
    @FXML private TableView<Object> tableView;

    @FXML private TableColumn<Object, Number> colId;
    @FXML private TableColumn<Object, String> colUsername;
    @FXML private TableColumn<Object, String> colEmail;
    @FXML private TableColumn<Object, String> colFirstName;
    @FXML private TableColumn<Object, String> colLastName;
    @FXML private TableColumn<Object, String> colCreatedAt;

    @FXML private Button btnExportPdf;

    // ===== STATE =====
    private long currentRequestId = 0;
    private String displayedReport = null;

    private static final DateTimeFormatter CREATED_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ===== INIT =====
    @FXML
    public void initialize() {

        cmbReportType.setItems(FXCollections.observableArrayList(
                "Clients report",
                "Sales report",
                "Purchases report",
                "Users report",
                "Activity report",
                "Support Requests report"   // <-- EXACT string used in switch
        ));

        cmbReportType.getSelectionModel().clearSelection();
        cmbCity.setDisable(true);
        btnGenerate.setDisable(true);

        dpFrom.setDisable(true);
        dpTo.setDisable(true);

        // defaults (last 7 days)
        dpTo.setValue(LocalDate.now());
        dpFrom.setValue(LocalDate.now().minusDays(6));

        cmbCity.setConverter(new StringConverter<>() {
            @Override public String toString(City city) {
                return (city == null) ? "" : city.getName();
            }
            @Override public City fromString(String s) { return null; }
        });

        // initial table mode = clients (safe default)
        setupClientsTableColumns();
        setupClientsTableInteractions();

        wireUiStateListeners();
        applyUiState();

        displayedReport = null;
        applyReportLayout();

        loadCitiesFromServer();

        // sizing
        barChart.prefHeightProperty().bind(reportArea.heightProperty());
        tableView.prefHeightProperty().bind(reportArea.heightProperty());
    }

    // ===== UI STATE =====
    private void wireUiStateListeners() {

        cmbReportType.valueProperty().addListener((obs, oldV, newV) -> {
            // city selection logic
            if ("Clients report".equals(newV) || "Support Requests report".equals(newV)) {
                cmbCity.getSelectionModel().clearSelection();
            } else {
                if (cmbCity.getValue() == null && cmbCity.getItems() != null && !cmbCity.getItems().isEmpty()) {
                    cmbCity.getSelectionModel().select(0);
                }
            }
            applyUiState();
        });

        cmbCity.valueProperty().addListener((obs, o, n) -> applyUiState());
        dpFrom.valueProperty().addListener((obs, o, n) -> applyUiState());
        dpTo.valueProperty().addListener((obs, o, n) -> applyUiState());
    }

    private void applyUiState() {
        String report = cmbReportType.getValue();

        boolean hasReport = report != null && !report.isBlank();
        boolean isClients = "Clients report".equals(report);
        boolean isSupport = "Support Requests report".equals(report);

        boolean supportsDateRange = "Activity report".equals(report) || "Purchases report".equals(report);

        dpFrom.setDisable(!supportsDateRange);
        dpTo.setDisable(!supportsDateRange);

        // City should be disabled for Clients + Support Requests
        boolean needsCity = !(isClients || isSupport);
        cmbCity.setDisable(!hasReport || !needsCity);

        boolean cityChosen = cmbCity.getValue() != null;

        boolean datesOk = true;
        if (supportsDateRange) {
            LocalDate from = dpFrom.getValue();
            LocalDate to = dpTo.getValue();
            datesOk = (from != null && to != null && !from.isAfter(to));
        }

        // Generate enabled when:
        // - report chosen
        // - if city is needed -> city chosen
        // - if dates are needed -> dates ok
        btnGenerate.setDisable(!hasReport || (needsCity && !cityChosen) || (supportsDateRange && !datesOk));
    }


    // layout changes only after Generate
    private void applyReportLayout() {
        String report = displayedReport;

        boolean hasReport = report != null && !report.isBlank();
        boolean isClients = "Clients report".equals(report);
        boolean isActivity = "Activity report".equals(report);
        boolean isPurchases = "Purchases report".equals(report);
        boolean isSupport = "Support Requests report".equals(report);

        lblChooseReport.setVisible(!hasReport);
        lblChooseReport.setManaged(!hasReport);

        reportContent.setVisible(hasReport);
        reportContent.setManaged(hasReport);

        barChart.setVisible(false);
        barChart.setManaged(false);

        tableView.setVisible(false);
        tableView.setManaged(false);

        if (isClients || isSupport) {
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

    // ===== LOAD CITIES =====
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

    // ===== GENERATE =====
    @FXML
    private void onGenerate() {

        String selected = cmbReportType.getValue();
        if (selected == null || selected.isBlank()) return;

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
            case "Support Requests report" -> generateSupportRequestsReport(reqId);
            default -> {
                lblChooseReport.setText("This report is not implemented yet.");
                lblChooseReport.setVisible(true);
                lblChooseReport.setManaged(true);

                reportContent.setVisible(false);
                reportContent.setManaged(false);
            }
        }
    }

    // ===== CLIENTS REPORT =====
    private void generateClientsReport(long reqId) {
        new Thread(() -> {
            try {
                Message req = new Message(ActionType.GET_ALL_CLIENTS_REPORT_REQUEST, null);
                Message res = (Message) GCMClient.getInstance().sendRequest(req);

                Platform.runLater(() -> {
                    if (reqId != currentRequestId) return;

                    if (res != null && res.getAction() == ActionType.GET_ALL_CLIENTS_REPORT_RESPONSE) {
                        AllClientsReport report = (AllClientsReport) res.getMessage();

                        lblChooseReport.setVisible(false);
                        lblChooseReport.setManaged(false);

                        setupClientsTableColumns();
                        setupClientsTableInteractions();

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

    private void setupClientsTableColumns() {

        colId.setText("ID");
        colUsername.setText("Username");
        colEmail.setText("Email");
        colFirstName.setText("First name");
        colLastName.setText("Last name");
        colCreatedAt.setText("Created");

        colId.setCellValueFactory(data -> {
            AllClientsReport.ClientRow r = (AllClientsReport.ClientRow) data.getValue();
            return new javafx.beans.property.SimpleIntegerProperty(r.userId);
        });

        colUsername.setCellValueFactory(data -> {
            AllClientsReport.ClientRow r = (AllClientsReport.ClientRow) data.getValue();
            return new javafx.beans.property.SimpleStringProperty(r.username);
        });

        colEmail.setCellValueFactory(data -> {
            AllClientsReport.ClientRow r = (AllClientsReport.ClientRow) data.getValue();
            return new javafx.beans.property.SimpleStringProperty(r.email);
        });

        colFirstName.setCellValueFactory(data -> {
            AllClientsReport.ClientRow r = (AllClientsReport.ClientRow) data.getValue();
            return new javafx.beans.property.SimpleStringProperty(r.firstName);
        });

        colLastName.setCellValueFactory(data -> {
            AllClientsReport.ClientRow r = (AllClientsReport.ClientRow) data.getValue();
            return new javafx.beans.property.SimpleStringProperty(r.lastName);
        });

        colCreatedAt.setCellValueFactory(data -> {
            AllClientsReport.ClientRow r = (AllClientsReport.ClientRow) data.getValue();
            var dt = r.createdAt;
            return new javafx.beans.property.SimpleStringProperty(dt == null ? "" : CREATED_FMT.format(dt));
        });
    }

    private void setupClientsTableInteractions() {
        tableView.setRowFactory(tv -> {
            TableRow<Object> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    Object item = row.getItem();
                    if (item instanceof AllClientsReport.ClientRow clientRow) {
                        openClientPurchaseHistory(clientRow);
                    }
                }
            });
            return row;
        });
    }

    // ===== ACTIVITY REPORT =====
    private void generateActivityReport(long reqId) {

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
                    if (reqId != currentRequestId) return;

                    if (res != null && res.getAction() == ActionType.GET_ACTIVITY_REPORT_RESPONSE) {
                        ActivityReport report = (ActivityReport) res.getMessage();

                        lblChooseReport.setVisible(false);
                        lblChooseReport.setManaged(false);

                        fillActivityBarChart(report);

                    } else {
                        showAlert("Error", "Failed to generate activity report.");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Network Error", "Could not generate report: " + e.getMessage()));
            }
        }).start();
    }

    private void fillActivityBarChart(ActivityReport report) {
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

    // ===== PURCHASES REPORT =====
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
                    if (reqId != currentRequestId) return;

                    if (res != null && res.getAction() == ActionType.GET_PURCHASES_REPORT_RESPONSE) {
                        common.report.PurchasesReport report = (common.report.PurchasesReport) res.getMessage();
                        fillPurchasesBarChart(report);

                        lblChooseReport.setVisible(false);
                        lblChooseReport.setManaged(false);
                    } else {
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

    // ===== SUPPORT REQUESTS REPORT =====
    private void generateSupportRequestsReport(long reqId) {

        new Thread(() -> {
            try {
                Message req = new Message(ActionType.GET_SUPPORT_REQUESTS_REPORT_REQUEST, null);
                Message res = (Message) GCMClient.getInstance().sendRequest(req);

                Platform.runLater(() -> {
                    if (reqId != currentRequestId) return;

                    if (res != null && res.getAction() == ActionType.GET_SUPPORT_REQUESTS_REPORT_RESPONSE) {

                        SupportRequestsReport report = (SupportRequestsReport) res.getMessage();

                        lblChooseReport.setVisible(false);
                        lblChooseReport.setManaged(false);

                        setupSupportTableColumns();
                        setupSupportTableInteractions();

                        fillSupportRequestsTable(report);
                        fillSupportRequestsBarChart(report);

                    } else {
                        showAlert("Error", "Failed to generate support requests report.");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Network Error", "Could not generate report: " + e.getMessage()));
            }
        }).start();
    }

    private void setupSupportTableColumns() {

        colId.setText("Ticket");
        colUsername.setText("Client");
        colEmail.setText("Topic");
        colFirstName.setText("Status");
        colLastName.setText("Created");
        colCreatedAt.setText("Preview");

        colId.setCellValueFactory(data -> {
            SupportTicketRowDTO r = (SupportTicketRowDTO) data.getValue();
            return new javafx.beans.property.SimpleIntegerProperty(r.getTicketId());
        });

        colUsername.setCellValueFactory(data -> {
            SupportTicketRowDTO r = (SupportTicketRowDTO) data.getValue();
            return new javafx.beans.property.SimpleStringProperty(r.getClientUsername()); // ✅ correct
        });

        colEmail.setCellValueFactory(data -> {
            SupportTicketRowDTO r = (SupportTicketRowDTO) data.getValue();
            return new javafx.beans.property.SimpleStringProperty(r.getTopic());
        });

        colFirstName.setCellValueFactory(data -> {
            SupportTicketRowDTO r = (SupportTicketRowDTO) data.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    r.getStatus() == null ? "" : r.getStatus().name()
            );
        });

        colLastName.setCellValueFactory(data -> {
            SupportTicketRowDTO r = (SupportTicketRowDTO) data.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                    r.getCreatedAt() == null ? "" : CREATED_FMT.format(r.getCreatedAt())
            );
        });

        colCreatedAt.setCellValueFactory(data -> {
            SupportTicketRowDTO r = (SupportTicketRowDTO) data.getValue();
            String txt = r.getClientText();
            if (txt == null) txt = "";
            txt = txt.replace("\n", " ").trim();
            if (txt.length() > 35) txt = txt.substring(0, 35) + "...";
            return new javafx.beans.property.SimpleStringProperty(txt);
        });
    }

    private void fillSupportRequestsTable(SupportRequestsReport report) {
        if (report == null || report.rows == null) {
            tableView.setItems(FXCollections.observableArrayList());
            return;
        }
        tableView.setItems(FXCollections.observableArrayList(report.rows));
    }

    private void fillSupportRequestsBarChart(SupportRequestsReport report) {
        barChart.getData().clear();
        barChart.setAnimated(false);

        if (report == null) return;

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Support requests");

        XYChart.Data<String, Number> pending = new XYChart.Data<>("Pending", report.pendingCount);
        XYChart.Data<String, Number> done    = new XYChart.Data<>("Done", report.doneCount);

        // Color each bar separately (JavaFX creates the Node later, so we hook nodeProperty)
        pending.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) newNode.setStyle("-fx-bar-fill: #8e44ad;"); // purple
        });

        done.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) newNode.setStyle("-fx-bar-fill: #2ecc71;"); // green
        });

        s.getData().add(pending);
        s.getData().add(done);

        barChart.getData().add(s);
    }


    private void setupSupportTableInteractions() {
        tableView.setRowFactory(tv -> {
            TableRow<Object> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    Object item = row.getItem();
                    if (item instanceof SupportTicketRowDTO dto) {
                        openSupportTicketPopup(dto);
                    }
                }
            });
            return row;
        });
    }

    private void openSupportTicketPopup(SupportTicketRowDTO row) {

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ticket #" + row.getTicketId());

        TextArea clientBody = new TextArea(row.getClientText() == null ? "" : row.getClientText());
        clientBody.setEditable(false);
        clientBody.setWrapText(true);

        TextArea agentBody = new TextArea(row.getAgentReply() == null ? "" : row.getAgentReply());
        agentBody.setEditable(false);
        agentBody.setWrapText(true);

        VBox box = new VBox(10,
                new Label("Client request:"), clientBody,
                new Label("Support response:"), agentBody
        );

        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    // ===== CLIENT PURCHASE HISTORY POPUP =====
    private void openClientPurchaseHistory(AllClientsReport.ClientRow clientRow) {
        if (clientRow == null) return;

        int userId = clientRow.userId;

        new Thread(() -> {
            try {
                Message subReq = new Message(ActionType.GET_USER_SUBSCRIPTIONS_REQUEST, userId);
                Message subRes = (Message) GCMClient.getInstance().sendRequest(subReq);

                List<SubscriptionStatusDTO> subs = java.util.Collections.emptyList();
                if (subRes != null && subRes.getAction() == ActionType.GET_USER_SUBSCRIPTIONS_RESPONSE) {
                    subs = (List<SubscriptionStatusDTO>) subRes.getMessage();
                }

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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/PurchaseHistoryDialog.fxml"));
            Parent root = loader.load();

            PurchaseHistoryDialogController controller = loader.getController();

            String displayName = (clientRow.firstName != null && !clientRow.firstName.isBlank())
                    ? clientRow.firstName : clientRow.username;

            controller.initForClient(displayName, subscriptions, purchases);

            Stage stage = new Stage();
            stage.setTitle("Client purchase history");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            showAlert("Error", "Could not open history window: " + e.getMessage());
        }
    }

    // ===== EXPORT PDF =====
    @FXML
    private void onExportPdf() {

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
            SnapshotParameters params = new SnapshotParameters();
            WritableImage fxImg = reportArea.snapshot(params, null);
            BufferedImage bImg = SwingFXUtils.fromFXImage(fxImg, null);

            try (PDDocument doc = new PDDocument()) {

                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                var pdImage = LosslessFactory.createFromImage(doc, bImg);

                float pageW = page.getMediaBox().getWidth();
                float pageH = page.getMediaBox().getHeight();

                float imgW = pdImage.getWidth();
                float imgH = pdImage.getHeight();

                float margin = 36;
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

    // ===== UTILS =====
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
