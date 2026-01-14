package server.handler;

import common.dto.PendingContentApprovalsResponse;
import common.enums.ActionType;
import common.messaging.Message;
import common.workflow.PendingContentRequest;
import server.repository.PendingContentRequestRepository;

import java.util.List;

public class GetPendingContentApprovalsHandler implements RequestHandler {

    private final PendingContentRequestRepository repository = PendingContentRequestRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            List<PendingContentRequest> pending = repository.findAllPending();
            PendingContentApprovalsResponse response = new PendingContentApprovalsResponse(pending);
            return new Message(ActionType.GET_PENDING_CONTENT_APPROVALS_RESPONSE, response);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Error fetching pending content approvals: " + e.getMessage());
        }
    }
}
