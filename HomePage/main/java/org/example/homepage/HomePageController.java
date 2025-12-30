package HomePage.main.java.org.example.homepage;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;

public class HomePageController {

    @FXML private AnchorPane centerHost;
    @FXML private Button loginBtn;

    private boolean loggedIn = false;

    @FXML
    public void initialize() {
        onLoginOrProfile();   // Login first
        updateLoginButton();
    }

    @FXML
    private void onCatalog() {
        showPage("/org/example/homepage/CatalogPage.fxml");
    }

    @FXML
    private void onSupport() {
        showPage("/org/example/homepage/SupportPage.fxml");
    }

    @FXML
    private void onInfo() {
        showPage("/org/example/homepage/InfoPage.fxml");
    }

    @FXML
    private void onLoginOrProfile() {
        if (!loggedIn) {
            showPage("/org/example/homepage/LoginPage.fxml");
        } else {
            showPage("/org/example/homepage/ProfilePage.fxml");
        }
    }

    private void updateLoginButton() {
        loginBtn.setText(loggedIn ? "Profile" : "Login");
    }

    private void showPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();

            // Make the loaded page fill the center AnchorPane:
            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);

            centerHost.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
