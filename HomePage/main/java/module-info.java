module org.example.homepage {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens org.example.homepage to javafx.fxml;
    exports org.example.homepage;
}