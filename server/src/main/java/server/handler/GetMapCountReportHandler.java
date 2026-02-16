package server.handler;

import common.enums.ActionType;
import common.enums.ReportType;
import common.messaging.Message;
import common.report.MapCountReport;
import server.report.ReportManager;

public class GetMapCountReportHandler implements RequestHandler {

    private final ReportManager reportManager;

    public GetMapCountReportHandler(ReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public Message handle(Message request) {

        MapCountReport report = reportManager.generate(
                ReportType.MAP_COUNT,
                MapCountReport.class
        );

        return new Message(ActionType.GET_MAP_COUNT_REPORT_RESPONSE, report);
    }
}
