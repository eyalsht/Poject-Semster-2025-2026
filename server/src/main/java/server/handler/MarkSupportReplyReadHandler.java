package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import common.support.MarkSupportReplyReadRequest;
import server.support.SupportTicketService;

public class MarkSupportReplyReadHandler implements RequestHandler {
    @Override
    public Message handle(Message request) {
        MarkSupportReplyReadRequest req = (MarkSupportReplyReadRequest) request.getMessage();
        SupportTicketService.markRead(req.getUserId(), req.getTicketId());
        return new Message(ActionType.MARK_SUPPORT_REPLY_READ, "OK");
    }
}
