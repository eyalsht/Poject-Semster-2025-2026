package homepage;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import entities.User;
import javafx.scene.control.Label;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

public class HomePageController {

    @FXML private AnchorPane centerHost;
    @FXML private Button btnCatalog;
    @FXML private Button btnProfile;
    @FXML private Button btnManagement;
    @FXML private Button loginBtn;
    @FXML private Label lblWelcome; // ה-Label מתןך ה-SceneBuilder

    private User currentUser = null; // משתנה חדש במקום ה-boolean loggedIn

    private boolean loggedIn = false;

    @FXML
    public void initialize() {
        showPage("/org/example/homepage/WelcomePage.fxml");
        updateUI();  // קורא לפונקציה החדשה שמסדרת את הכפתורים
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
      /*  if (!loggedIn) {
            showPage("/org/example/homepage/LoginPage.fxml");
        } else {
            showPage("/org/example/homepage/ProfilePage.fxml");
        }*/
        if (currentUser == null) {
            showPage("/org/example/homepage/LoginPage.fxml");
        } else {
            showPage("/org/example/homepage/ProfilePage.fxml");
        }
    }
    @FXML
    private void onCloseApp(ActionEvent event) {
        // Close the window that contains the button
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();

        // If you want to shut down the entire JavaFX app (all windows / threads):
        Platform.exit();
        // Optional "hard stop" if you still have non-daemon threads running:
        // System.exit(0);
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

    public void setLoggedInUser(User user) {
        this.currentUser = user;
        updateUI(); // עדכון השם והכפתור
        onCatalog(); // החזרה לקטלוג הראשי (אופציונלי)
    }

    private void updateUI() {
        if (currentUser == null) {
            // מצב אורח
            loginBtn.setText("Login");
            if (lblWelcome != null) lblWelcome.setText("Welcome, Guest");
        } else {
            // מצב מחובר - שימוש בשם האמיתי
            loginBtn.setText("Profile");
            if (lblWelcome != null) lblWelcome.setText("Welcome, " + currentUser.getFirstName());
        }
    }

    @FXML
    void onManagement(javafx.event.ActionEvent event) {
        System.out.println("Management button clicked!");
        // בעתיד: showPage("/gui/EmployeeDashboard.fxml");
    }
}
