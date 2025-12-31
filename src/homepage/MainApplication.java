package homepage;

import client.GCMClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import server.ServerLauncher;

public class MainApplication extends Application {

    private static final int PORT = 5555;

    // keep reference so controllers can close the app
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // 1) Start server automatically
        ServerLauncher.startServer(PORT);

        // 2) Start client (connects to localhost:5555)
        GCMClient.getInstance();

        // 3) Load UI
        FXMLLoader loader = new FXMLLoader(
                MainApplication.class.getResource("/org/example/homepage/HomePage.fxml")
        );

        Scene scene = new Scene(loader.load(), 1000, 650);
        stage.setTitle("Global City Map");
        stage.setScene(scene);

        // X button also shuts down server+client
        stage.setOnCloseRequest(e -> {
            e.consume();        // stop default close
            shutdownApp();      // our shutdown
        });

        stage.show();
    }

    // ✅ MUST be outside start()
    public static void shutdownApp() {
        System.out.println("Shutting down app...");

        // close client
        try {
            GCMClient.getInstance().quit();
        } catch (Exception ignored) {}

        // stop server
        try {
            ServerLauncher.stopServer();
        } catch (Exception ignored) {}

        // close UI
        if (primaryStage != null) {
            Platform.runLater(() -> primaryStage.close());
        }

        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
