package server.handler;

import common.enums.ActionType;
import common.enums.SupportTicketStatus;
import common.messaging.Message;
import common.report.SupportRequestsReport;
import common.support.SupportTicketRowDTO;
import server.support.SupportTicketService;

import java.util.List;

public class GetSupportRequestsReportHandler implements RequestHandler {

    @Override
    public Message handle(Message request) {

        // NOTE: method ignores agentId, it returns all tickets ordered by status+createdAt
        List<SupportTicketRowDTO> rows = SupportTicketService.listAllTicketsForAgent(0);

        int pending = 0;
        int done = 0;

        for (SupportTicketRowDTO r : rows) {
            if (r.getStatus() == SupportTicketStatus.OPEN) pending++;
            else if (r.getStatus() == SupportTicketStatus.DONE) done++;
        }

        SupportRequestsReport report = new SupportRequestsReport(pending, done, rows);
        return new Message(ActionType.GET_SUPPORT_REQUESTS_REPORT_RESPONSE, report);
    }
}
