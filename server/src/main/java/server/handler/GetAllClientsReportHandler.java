package server.handler;

import common.enums.ActionType;
import common.enums.ReportType;
import common.messaging.Message;
import common.report.AllClientsReport;
import server.report.ReportManager;

public class GetAllClientsReportHandler implements RequestHandler {

    private final ReportManager reportManager;

    public GetAllClientsReportHandler(ReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public Message handle(Message request) {

        AllClientsReport report = reportManager.generate(
                ReportType.CLIENTS,
                AllClientsReport.class
        );

        return new Message(ActionType.GET_ALL_CLIENTS_REPORT_RESPONSE, report);
    }
}
