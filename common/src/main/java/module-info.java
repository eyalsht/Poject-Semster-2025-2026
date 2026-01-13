module com.gcm.common {
    requires java.sql;
    requires jakarta.persistence;
    exports common.dto;
    exports common.user;
    exports common.content;
    exports common.purchase;
    exports common.workflow;
    exports common.enums;
    exports common.messaging;
    exports common.report;
}

