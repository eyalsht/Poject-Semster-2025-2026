package server.handler;

import common.enums.ActionType;
import common.enums.ReportType;
import common.messaging.Message;
import common.report.ActivityReport;
import server.report.ReportManager;

import java.time.LocalDate;
import java.util.ArrayList;

public class GetActivityReportHandler implements RequestHandler {

    private final ReportManager reportManager;

    public GetActivityReportHandler(ReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public Message handle(Message request) {

        ArrayList<Object> payload = (ArrayList<Object>) request.getMessage();
        LocalDate from = (LocalDate) payload.get(0);
        LocalDate to = (LocalDate) payload.get(1);
        Integer cityId = (Integer) payload.get(2);

        ActivityReport report = reportManager.generate(
                ReportType.ACTIVITY,
                ActivityReport.class,
                from, to, cityId
        );

        return new Message(ActionType.GET_ACTIVITY_REPORT_RESPONSE, report);
    }
}
