package server.handler;

import common.dto.PendingApprovalsResponse;
import common.enums.ActionType;
import common.messaging.Message;
import common.workflow.PendingPriceUpdate;
import server.repository.PendingPriceUpdateRepository;

import java.util.List;

/**
 * Handler for getting pending approvals.
 * Returns both the list and count in a single response.
 */
public class GetPendingApprovalsHandler implements RequestHandler {

    private final PendingPriceUpdateRepository repository = PendingPriceUpdateRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            List<PendingPriceUpdate> pending = repository.findAllPending();
            PendingApprovalsResponse response = new PendingApprovalsResponse(pending);
            return new Message(ActionType.GET_PENDING_APPROVALS_RESPONSE, response);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Error fetching pending approvals: " + e.getMessage());
        }
    }
}
