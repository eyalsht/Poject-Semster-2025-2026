package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApplication extends Application {

    private static final int PORT = 5555;

    // keep reference so controllers can close the app
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Start client (connects to server on Cloud machine: 20.250.162.225:443)
        // Note: Server must be running separately before starting the client
        GCMClient.getInstance();

        // Load UI
        FXMLLoader loader = new FXMLLoader(
                MainApplication.class.getResource("/GUI/HomePage.fxml")
        );

        Scene scene = new Scene(loader.load(), 1000, 650);
        stage.setTitle("Global City Map");
        stage.setScene(scene);

        // X button shuts down client
        stage.setOnCloseRequest(e -> {
            e.consume();        // stop default close
            shutdownApp();      // our shutdown
        });

        stage.show();
    }

    public static void shutdownApp() {
        System.out.println("Shutting down client...");

        // close client
        try {
            GCMClient.getInstance().quit();
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
