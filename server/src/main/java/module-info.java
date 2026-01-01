module com.gcm.server {
    requires com.gcm.common;
    requires java.sql;
    // MySQL Connector/J may not be a named module - will be automatic module if needed
    
    exports server;
}

