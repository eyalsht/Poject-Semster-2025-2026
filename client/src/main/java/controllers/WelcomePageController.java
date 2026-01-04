package controllers;

import common.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class WelcomePageController {

    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;

    public void setUser(User user) {
        if (user == null) {
            lblTitle.setText("Welcome to GCM System");
            lblSubtitle.setText("Please login to view your personal area or browse the catalog");
            return;
        }

        String name = user.getFirstName();
        if (name == null || name.isBlank()) name = user.getUsername();

        lblTitle.setText("Welcome, " + name + "!");
        lblSubtitle.setText("You are logged in. Use Profile to view your details.");
    }
}
