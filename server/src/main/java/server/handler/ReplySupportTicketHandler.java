package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import common.support.ReplySupportTicketRequest;
import server.support.SupportTicketService;

public class ReplySupportTicketHandler implements RequestHandler {
    @Override
    public Message handle(Message request) {
        ReplySupportTicketRequest req = (ReplySupportTicketRequest) request.getMessage();
        SupportTicketService.replyToTicket(req);
        return new Message(ActionType.REPLY_SUPPORT_TICKET, "OK");
    }
}
