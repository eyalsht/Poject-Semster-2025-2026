package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import server.GcmServer;
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
            String error = repository.approveWithError(pendingId, null);

            if (error == null) {
                GcmServer server = GcmServer.getInstance();
                if (server != null) {
                    server.sendToAllClients(new Message(ActionType.CATALOG_UPDATED_NOTIFICATION, null));
                }
                return new Message(ActionType.APPROVE_CONTENT_RESPONSE, "OK");
            } else {
                System.err.println("Approval failed for ID " + pendingId + ": " + error);
                return new Message(ActionType.APPROVE_CONTENT_RESPONSE, error);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.APPROVE_CONTENT_RESPONSE, "Exception: " + e.getMessage());
        }
    }
}
