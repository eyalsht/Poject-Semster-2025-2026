module org.example.homepage {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.homepage to javafx.fxml;
    exports org.example.homepage;
}