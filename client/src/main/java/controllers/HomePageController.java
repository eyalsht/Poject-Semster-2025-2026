package controllers;

import client.MainApplication;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import common.User;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class HomePageController {

    @FXML private AnchorPane centerHost;
    @FXML private Button btnCatalog;
    @FXML private Button btnProfile;
    @FXML private Button btnManagement;
    @FXML private Button loginBtn;
    @FXML private Label lblWelcome; // ה-Label מתןך ה-SceneBuilder
    @FXML private Button btnCatalogUpdate;
    @FXML private Button btnPriceUpdate;
    @FXML private Button btnReports;
    @FXML private ImageView imgHomePage;

    private User currentUser = null; // משתנה חדש במקום ה-boolean loggedIn

    private boolean loggedIn = false;

    @FXML
    public void initialize() {
        // Load the home page image
        if (imgHomePage != null) {
            try {
                Image image = new Image(getClass().getResourceAsStream("/images/gcmhomepage.png"));
                imgHomePage.setImage(image);
            } catch (Exception e) {
                System.err.println("Failed to load home page image: " + e.getMessage());
            }
        }
        showPage("/GUI/WelcomePage.fxml");
        updateUI();
    }


    @FXML
    private void onCatalog() {
        showPage("/GUI/CatalogPage.fxml");
    }

    @FXML
    private void onSupport() {
        showPage("/GUI/SupportPage.fxml");
    }

    @FXML
    private void onInfo() {
        showPage("/GUI/InfoPage.fxml");
    }

    @FXML
    private void onLoginOrProfile() {

        if (currentUser == null) {
            showPage("/GUI/LoginPage.fxml");
        } else {
            showPage("/GUI/ProfilePage.fxml");
        }
    }
    @FXML
    private void onCloseApp(ActionEvent event) {
        MainApplication.shutdownApp();
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
        /*EmployeeRole role = currentUser.getEmployeeRole();

        if (role == null) {
            // CLIENT
            setEmployeeButtons(false, false, false);
            return;
        }

        switch (role) {
            case Company_Manager:
                setEmployeeButtons(true, true, true);
                break;

            case Content_Manager:
            case Content_Worker:
                setEmployeeButtons(true, false, false);
                break;

            default: // Support_Agent etc.
                setEmployeeButtons(false, false, false);
        }*/
    }

    @FXML
    void onManagement(javafx.event.ActionEvent event) {
        System.out.println("Management button clicked!");
        // בעתיד: showPage("/gui/EmployeeDashboard.fxml");
    }
    private void showProfilePage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/GUI/ProfilePage.fxml")
            );

            AnchorPane view = loader.load();

            ProfilePageController controller = loader.getController();
            controller.setUser(currentUser); // 👈 PASS USER HERE

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
