module com.gcm.server {
    requires com.gcm.common;
    requires java.sql;
    requires java.naming;
    requires mysql.connector.j;
    exports server;

    requires org.hibernate.orm.core;
    requires jakarta.persistence;
}

