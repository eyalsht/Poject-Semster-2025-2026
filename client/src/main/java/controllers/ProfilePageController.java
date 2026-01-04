package controllers;

import common.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ProfilePageController {

    @FXML private Label lblWelcome;
    @FXML private Label lblName;
    @FXML private Label lblEmail;
    @FXML private Label lblPayment;
    @FXML private Label lblInbox;
    @FXML private Label lblHistory;

    public void setUser(User user) {
        if (user == null) return;

        String first = user.getFirstName();
        String last  = user.getLastName();

        String fullName = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
        if (fullName.isBlank()) fullName = user.getUsername();

        lblWelcome.setText("Welcome " + (first != null && !first.isBlank() ? first : user.getUsername()));
        lblName.setText(fullName);
        lblEmail.setText(user.getEmail() != null ? user.getEmail() : user.getUsername());

        lblPayment.setText("Not set");
        lblInbox.setText("You have 0 new messages");
        lblHistory.setText("(coming soon)");
    }
}
