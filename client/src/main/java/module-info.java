module com.gcm.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.gcm.common;

    opens controllers to javafx.fxml;
    opens client to javafx.fxml;
    
    exports client;
    exports controllers;
}

