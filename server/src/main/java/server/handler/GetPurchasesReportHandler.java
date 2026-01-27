package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import common.report.PurchasesReport;
import org.hibernate.SessionFactory;
import server.report.ReportManager;
import common.enums.ReportType;

import java.time.LocalDate;
import java.util.ArrayList;

public class GetPurchasesReportHandler implements RequestHandler {

    private final ReportManager reportManager;

    public GetPurchasesReportHandler(ReportManager reportManager) {
        this.reportManager = reportManager;
    }

    @Override
    public Message handle(Message request) {

        ArrayList<Object> payload = (ArrayList<Object>) request.getMessage();
        LocalDate from = (LocalDate) payload.get(0);
        LocalDate to = (LocalDate) payload.get(1);
        Integer cityId = (Integer) payload.get(2);

        PurchasesReport report = reportManager.generate(
                ReportType.PURCHASES,
                PurchasesReport.class,
                from, to, cityId
        );

        return new Message(ActionType.GET_PURCHASES_REPORT_RESPONSE, report);
    }
}
