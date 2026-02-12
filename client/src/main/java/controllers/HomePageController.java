package controllers;

import client.GCMClient;
import client.MainApplication;
import common.enums.ActionType;
import common.messaging.Message;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import common.user.User;
import javafx.scene.control.Label;
import javafx.event.ActionEvent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import common.enums.EmployeeRole;
import common.user.Client;
import common.user.Employee;

public class HomePageController
{

    @FXML private AnchorPane centerHost;
    @FXML private Button btnCatalog;
    @FXML private Button btnProfile;
    @FXML private Button btnManagement;
    @FXML private Button loginBtn;
    @FXML private Button btnLogout;
    @FXML private Label lblWelcome;
    @FXML private Button btnCatalogUpdate;
    @FXML private Button btnPriceUpdate;
    @FXML private Button btnReports;
    @FXML private ImageView imgHomePage;
    @FXML private Button btnSupport;
    @FXML private Button btnMySubscriptions;
    @FXML private Button btnMyMaps;

    private User currentUser = null;

    private boolean loggedIn = false;

    @FXML
    public void initialize()
    {
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
        if (currentUser instanceof Employee emp && emp.getRole() == EmployeeRole.SUPPORT_AGENT) {
            showPage("/GUI/SupportTasksPage.fxml");
        } else {
            showPage("/GUI/SupportPage.fxml");
        }
    }


    @FXML
    private void onMySubscriptions() {
        showPage("/GUI/MySubscriptionsPage.fxml");
    }

    @FXML
    private void onMyMaps() {
        showPage("/GUI/MyMapsPage.fxml");
    }

    @FXML
    private void onInfo() {
        showPage("/GUI/InfoPage.fxml");
    }

    @FXML
    private void onReports() {showPage("/GUI/ReportPage.fxml");}

    @FXML
    private void onLoginOrProfile()
    {

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
    @FXML
    private void onLogout(ActionEvent event) {
        // Notify server (fire-and-forget on background thread)
        new Thread(() -> {
            try {
                GCMClient.getInstance().sendToServer(
                        new Message(ActionType.LOGOUT_REQUEST, null));
            } catch (Exception e) {
                System.err.println("Failed to send logout request: " + e.getMessage());
            }
        }).start();

        GCMClient.getInstance().setCurrentUser(null);
        this.currentUser = null;
        this.loggedIn = false;
        updateUI();
        showPage("/GUI/WelcomePage.fxml");

        System.out.println("Logout successful: Redirecting to Catalog as Guest.");
    }
    private void showPage(String fxmlPath) {
        try {
            System.out.println("Attempting to load: " + fxmlPath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            if (loader.getLocation() == null) {
                System.err.println("Error: FXML file not found at " + fxmlPath);
                return;
            }
            Node view = loader.load();
            Object ctrl = loader.getController();

            if (ctrl != null) {
                System.out.println("Loaded controller: " + ctrl.getClass().getName());

                if (ctrl instanceof LoginPageController loginCtrl) {
                    loginCtrl.setHomePageController(this);
                }

                if (ctrl instanceof WelcomePageController welcomeCtrl) {
                    welcomeCtrl.setUser(currentUser);
                }
                if (ctrl instanceof ProfilePageController profileCtrl) {
                    profileCtrl.setUser(currentUser);
                }
            } else {
                System.out.println("Loaded controller: (none) for " + fxmlPath);
            }


            if (centerHost == null) {
                System.err.println("Error: centerHost AnchorPane is NULL. Check fx:id in FXML.");
                return;
            }

            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);

            centerHost.getChildren().setAll(view);
        } catch (Exception e) {
            System.err.println("Failed to load page: " + fxmlPath);
            e.printStackTrace();
        }
    }
    public void setLoggedInUser(User user) {
        this.currentUser = user;
        this.loggedIn=true;
        updateUI();
        onCatalog();
    }

    private void updateUI()
    {
        if (currentUser == null)
        {
            btnLogout.setDisable(true);
            btnLogout.setVisible(false);
            loginBtn.setText("Login");
            if (lblWelcome != null) lblWelcome.setText("Welcome, Guest");
        }
        else
        {
            loginBtn.setText("Profile");
            btnLogout.setDisable(false);
            btnLogout.setVisible(true);
            if (lblWelcome != null) lblWelcome.setText("Welcome, " + currentUser.getFirstName());
        }

        // ===== minimal Reports button logic (ONLY company manager) =====
        if (btnReports != null)
        {
            boolean showReports = false;

            if (currentUser instanceof Employee emp) {
                showReports = (emp.getRole() == EmployeeRole.COMPANY_MANAGER);
            }
            btnReports.setDisable(!showReports);
            btnReports.setVisible(showReports);
            btnReports.setManaged(showReports); // removes VBox space when hidden
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

        // Show subscription/maps buttons only for Client users
        if (btnMySubscriptions != null && btnMyMaps != null) {
            boolean isClient = (currentUser instanceof Client);
            btnMySubscriptions.setVisible(isClient);
            btnMySubscriptions.setManaged(isClient);
            btnMyMaps.setVisible(isClient);
            btnMyMaps.setManaged(isClient);
        }

        if (btnSupport != null) {
            boolean isAgent = (currentUser instanceof Employee emp && emp.getRole() == EmployeeRole.SUPPORT_AGENT);
            btnSupport.setText(isAgent ? "Tasks" : "Support");
        }

    }

    @FXML
    void onManagement(javafx.event.ActionEvent event) {
        System.out.println("Management button clicked!");

    }
    private void showProfilePage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/GUI/ProfilePage.fxml")
            );

            AnchorPane view = loader.load();

            ProfilePageController controller = loader.getController();
            controller.setUser(currentUser);

            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);

            centerHost.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void onLoginSuccess(User user) {
        this.currentUser = user;
        this.loggedIn=true;
        updateUI();
        showPage("/GUI/WelcomePage.fxml");
    }
    private void setReportsButton(boolean show) {
        if (btnReports == null) return;
        btnReports.setVisible(show);
        btnReports.setManaged(show);
        btnReports.setDisable(!show);
    }
}
