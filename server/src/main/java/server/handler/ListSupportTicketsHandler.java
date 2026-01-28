package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import common.support.ListSupportTicketsRequest;
import common.support.ListSupportTicketsResponse;
import server.support.SupportTicketService;

public class ListSupportTicketsHandler implements RequestHandler {
    @Override
    public Message handle(Message request) {
        ListSupportTicketsRequest req = (ListSupportTicketsRequest) request.getMessage();
        var rows = SupportTicketService.listAllTicketsForAgent(req.getAgentId());
        return new Message(ActionType.LIST_SUPPORT_TICKETS, new ListSupportTicketsResponse(rows));
    }
}
