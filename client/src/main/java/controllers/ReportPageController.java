package controllers;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;

public class ReportPageController {

    @FXML private ComboBox<String> cmbReportType;
    @FXML private ComboBox<String> cmbCity;
    @FXML private Button btnGenerate;

    @FXML private BarChart<String, Number> barChart;
    @FXML private CategoryAxis barXAxis;
    @FXML private NumberAxis barYAxis;
    @FXML private PieChart pieChart;

    @FXML private TableView<?> tableView;
    @FXML private TableColumn<?, ?> col1;
    @FXML private TableColumn<?, ?> col2;

    @FXML private Button btnExportPdf;

    @FXML
    public void initialize() {
        System.out.println("Report page loaded");

        // temporary test data
        cmbReportType.getItems().addAll("Sales report", "Purchases report", "Users report");
        cmbCity.getItems().addAll("Haifa", "Tel Aviv", "Jerusalem");
    }

    @FXML
    private void onGenerate() {
        System.out.println("Generate clicked");
    }
}
