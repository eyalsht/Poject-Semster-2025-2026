module com.gcm.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.gcm.common;
    requires java.desktop;

    opens controllers to javafx.fxml;
    opens client to javafx.fxml;
    
    exports client;
    exports controllers;
}

