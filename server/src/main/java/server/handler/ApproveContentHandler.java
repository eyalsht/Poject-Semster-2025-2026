package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import server.repository.PendingContentRequestRepository;

/**
 * Handler for approving content change requests.
 * Used by Content Managers to approve add/edit/delete requests from workers.
 */
public class ApproveContentHandler implements RequestHandler {

    private final PendingContentRequestRepository repository = PendingContentRequestRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            int pendingId = (Integer) request.getMessage();
            
            // TODO: Get the approver user from the request if needed
            // For now, passing null as approver
            boolean success = repository.approve(pendingId, null);
            
            return new Message(ActionType.APPROVE_CONTENT_RESPONSE, success);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Error approving content request: " + e.getMessage());
        }
    }
}
