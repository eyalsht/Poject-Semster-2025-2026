package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import server.repository.PendingContentRequestRepository;

/**
 * Handler for denying content change requests.
 * Used by Content Managers to reject add/edit/delete requests from workers.
 */
public class DenyContentHandler implements RequestHandler {

    private final PendingContentRequestRepository repository = PendingContentRequestRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            int pendingId = (Integer) request.getMessage();
            
            // TODO: Get the denier user from the request if needed
            // For now, passing null as denier
            boolean success = repository.deny(pendingId, null);
            
            return new Message(ActionType.DENY_CONTENT_RESPONSE, success);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Error denying content request: " + e.getMessage());
        }
    }
}
