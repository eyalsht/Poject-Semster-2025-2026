package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import common.support.SupportSubmitRequest;
import common.support.SupportSubmitResponse;
import server.support.SupportService;

public class SubmitSupportHandler implements RequestHandler {
    @Override
    public Message handle(Message request) {
        SupportSubmitRequest req = (SupportSubmitRequest) request.getMessage();
        SupportSubmitResponse res = SupportService.handleSupport(req);
        return new Message(ActionType.SUBMIT_SUPPORT_RESPONSE, res);
    }
}
