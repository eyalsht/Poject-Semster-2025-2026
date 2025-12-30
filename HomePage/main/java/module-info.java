module org.example.homepage {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens org.example.homepage to javafx.fxml;
    opens Entities to javafx.base;
    exports org.example.homepage;
}