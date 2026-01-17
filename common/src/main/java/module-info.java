module com.gcm.common {
    requires jakarta.persistence;
    requires java.sql;
    requires org.hibernate.orm.core;
    //requires com.gcm.common;  // ADD THIS LINE - needed for proxy generation

    exports common.dto;
    exports common.user;
    exports common.content;
    exports common.purchase;
    exports common.workflow;
    exports common.enums;
    exports common.messaging;
    exports common.report;

    // Open packages to Hibernate for reflection access to private/protected fields
    opens common.user to org.hibernate.orm.core;
    opens common.content to org.hibernate.orm.core;
    opens common.purchase to org.hibernate.orm.core;
    opens common.workflow to org.hibernate.orm.core;
    opens common.report to org.hibernate.orm.core;
}

