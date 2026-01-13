package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import server.repository.PendingPriceUpdateRepository;

public class ApprovePendingHandler implements RequestHandler {

    private final PendingPriceUpdateRepository repository = PendingPriceUpdateRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            int pendingId = (Integer) request.getMessage();
            boolean success = repository.approve(pendingId);
            return new Message(ActionType.APPROVE_PENDING_RESPONSE, success);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.APPROVE_PENDING_RESPONSE, false);
        }
    }
}
