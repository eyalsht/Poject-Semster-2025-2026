package controllers;

import client.GCMClient;
import common.content.City;
import common.content.GCMMap;
import common.enums.ActionType;
import common.messaging.Message;
import common.purchase.PurchasedMapSnapshot;
import common.user.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class MyMapsPageController {

    @FXML private FlowPane flowPaneMaps;
    @FXML private Label lblEmpty;
    @FXML private VBox vboxSubscriptionSection;
    @FXML private Label lblSubEmpty;
    @FXML private VBox vboxSubMaps;

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public void initialize() {
        loadPurchasedMaps();
        loadSubscriptionMaps();
    }

    private void loadPurchasedMaps() {
        User user = GCMClient.getInstance().getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            try {
                Message request = new Message(ActionType.GET_USER_PURCHASED_MAPS_REQUEST, user.getId());
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    flowPaneMaps.getChildren().clear();

                    if (response != null && response.getAction() == ActionType.GET_USER_PURCHASED_MAPS_RESPONSE) {
                        ArrayList<PurchasedMapSnapshot> list = (ArrayList<PurchasedMapSnapshot>) response.getMessage();
                        if (list != null && !list.isEmpty()) {
                            lblEmpty.setVisible(false);
                            lblEmpty.setManaged(false);
                            for (PurchasedMapSnapshot snapshot : list) {
                                flowPaneMaps.getChildren().add(createMapCard(snapshot));
                            }
                        } else {
                            lblEmpty.setVisible(true);
                            lblEmpty.setManaged(true);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadSubscriptionMaps() {
        User user = GCMClient.getInstance().getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            try {
                Message request = new Message(ActionType.GET_SUBSCRIPTION_MAPS_REQUEST, user.getId());
                Message response = (Message) GCMClient.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getAction() == ActionType.GET_SUBSCRIPTION_MAPS_RESPONSE) {
                        ArrayList<City> cities = (ArrayList<City>) response.getMessage();

                        if (cities != null && !cities.isEmpty()) {
                            vboxSubscriptionSection.setVisible(true);
                            vboxSubscriptionSection.setManaged(true);
                            vboxSubMaps.getChildren().clear();

                            for (City city : cities) {
                                // City name header
                                Label cityHeader = new Label(city.getName());
                                cityHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #3498db;");
                                cityHeader.setPadding(new Insets(10, 0, 5, 0));
                                vboxSubMaps.getChildren().add(cityHeader);

                                // FlowPane for this city's maps
                                FlowPane cityMapsFlow = new FlowPane();
                                cityMapsFlow.setHgap(20);
                                cityMapsFlow.setVgap(15);
                                cityMapsFlow.setPrefWrapLength(900);

                                if (city.getMaps() != null) {
                                    for (GCMMap map : city.getMaps()) {
                                        try {
                                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GUI/MapCard.fxml"));
                                            Parent card = loader.load();
                                            MapCardController controller = loader.getController();
                                            controller.setViewOnly(true);
                                            controller.setData(map);
                                            cityMapsFlow.getChildren().add(card);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                vboxSubMaps.getChildren().add(cityMapsFlow);
                            }
                        } else {
                            // No active subscriptions - hide section entirely
                            vboxSubscriptionSection.setVisible(false);
                            vboxSubscriptionSection.setManaged(false);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private VBox createMapCard(PurchasedMapSnapshot snapshot) {
        VBox card = new VBox(8);
        card.setPrefWidth(219);
        card.setPrefHeight(240);
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #dcdde1;");
        card.setPadding(new Insets(10, 10, 10, 10));
        card.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        DropShadow shadow = new DropShadow();
        shadow.setRadius(4.5);
        shadow.setColor(Color.rgb(0, 0, 0, 0.1));
        card.setEffect(shadow);

        // Map image
        ImageView imgView = new ImageView();
        imgView.setFitWidth(180);
        imgView.setFitHeight(120);
        imgView.setPreserveRatio(true);

        byte[] imageData = snapshot.getMapImageData();
        if (imageData != null && imageData.length > 0) {
            try {
                imgView.setImage(new Image(new ByteArrayInputStream(imageData)));
            } catch (Exception e) {
                // fallback: no image
            }
        }

        Label lblName = new Label(snapshot.getMapName());
        lblName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2f3640;");

        Label lblCity = new Label("City: " + snapshot.getCityName());
        lblCity.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        Label lblVersion = new Label("Version: " + snapshot.getPurchasedVersion());
        lblVersion.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        String dateStr = snapshot.getPurchaseDate() != null ? dtf.format(snapshot.getPurchaseDate()) : "-";
        Label lblDate = new Label("Purchased: " + dateStr);
        lblDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");

        Label lblPrice = new Label(String.format("Paid: $%.2f", snapshot.getPricePaid()));
        lblPrice.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #e67e22;");

        card.getChildren().addAll(imgView, lblName, lblCity, lblVersion, lblDate, lblPrice);
        return card;
    }

}
