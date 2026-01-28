package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import common.support.ListClientSupportRepliesRequest;
import common.support.ListClientSupportRepliesResponse;
import server.support.SupportTicketService;

public class ListClientSupportRepliesHandler implements RequestHandler {
    @Override
    public Message handle(Message request) {
        ListClientSupportRepliesRequest req = (ListClientSupportRepliesRequest) request.getMessage();
        var rows = SupportTicketService.listRepliesForClient(req.getUserId());
        return new Message(ActionType.LIST_CLIENT_SUPPORT_REPLIES, new ListClientSupportRepliesResponse(rows));
    }
}
