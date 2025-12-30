package homepage;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        System.out.println(MainApplication.class.getResource(
                "/org.example.homepage/images/loginBackground.png"
        ));
        FXMLLoader loader = new FXMLLoader(
                MainApplication.class.getResource(
                        "/org/example/homepage/HomePage.fxml"
                )
        );

        Scene scene = new Scene(loader.load(), 1000, 650);
        stage.setTitle("Global City Map");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
