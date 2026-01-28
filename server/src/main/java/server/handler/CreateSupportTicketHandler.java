package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import common.support.CreateSupportTicketRequest;
import common.support.CreateSupportTicketResponse;
import server.support.SupportTicketService;

public class CreateSupportTicketHandler implements RequestHandler {
    @Override
    public Message handle(Message request) {
        CreateSupportTicketRequest req = (CreateSupportTicketRequest) request.getMessage();
        int id = SupportTicketService.createTicket(req);
        return new Message(ActionType.CREATE_SUPPORT_TICKET, new CreateSupportTicketResponse(id));
    }
}
