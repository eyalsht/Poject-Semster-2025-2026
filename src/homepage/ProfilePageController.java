package homepage;

import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ProfilePageController {

    @FXML
    private Label lblWelcome;

    public void setUser(User user) {
        if (user == null) return;

        lblWelcome.setText("Welcome " + user.getFirstName());

        /*lblName.setText(user.getFirstName());
        lblEmail.setText(user.getEmail());

        lblPayment.setText("Not set");
        lblInbox.setText("You have 0 new messages");
        lblHistory.setText("(coming soon)");*/
    }

}