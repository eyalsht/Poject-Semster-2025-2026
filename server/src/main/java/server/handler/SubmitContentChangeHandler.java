package server.handler;

import common.dto.ContentChangeRequest;
import common.enums.ActionType;
import common.enums.ContentActionType;
import common.messaging.Message;
import common.user.User;
import server.GcmServer;
import server.repository.PendingContentRequestRepository;
import server.repository.UserRepository;

public class SubmitContentChangeHandler implements RequestHandler {

    private final PendingContentRequestRepository repository = PendingContentRequestRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            ContentChangeRequest changeRequest = (ContentChangeRequest) request.getMessage();

            // Duplicate/conflict detection for non-ADD actions
            if (changeRequest.getActionType() != ContentActionType.ADD) {
                // Block exact duplicate (same target, type, action already OPEN)
                if (repository.hasPendingRequest(changeRequest.getTargetId(),
                        changeRequest.getContentType(), changeRequest.getActionType())) {
                    System.out.println("Blocked duplicate " + changeRequest.getActionType() +
                            " request for " + changeRequest.getContentType() +
                            " ID " + changeRequest.getTargetId());
                    return new Message(ActionType.SUBMIT_CONTENT_CHANGE_RESPONSE, false);
                }
                // Block EDIT if a DELETE is already pending for the same target
                if (changeRequest.getActionType() == ContentActionType.EDIT &&
                        repository.hasPendingRequest(changeRequest.getTargetId(),
                                changeRequest.getContentType(), ContentActionType.DELETE)) {
                    System.out.println("Blocked EDIT request — DELETE already pending for " +
                            changeRequest.getContentType() + " ID " + changeRequest.getTargetId());
                    return new Message(ActionType.SUBMIT_CONTENT_CHANGE_RESPONSE, false);
                }
            }

            User requester = null;
            if (changeRequest.getRequesterId() != null) {
                requester = userRepository.findById(changeRequest.getRequesterId()).orElse(null);
            }

            boolean success = repository.createPendingRequest(
                requester,
                changeRequest.getActionType(),
                changeRequest.getContentType(),
                changeRequest.getTargetId(),
                changeRequest.getTargetName(),
                changeRequest.getContentDetailsJson()
            );

            if (success) {
                // Notify all clients so approval counts refresh
                GcmServer server = GcmServer.getInstance();
                if (server != null) {
                    server.sendToAllClients(new Message(ActionType.CATALOG_UPDATED_NOTIFICATION, null));
                }
            }

            return new Message(ActionType.SUBMIT_CONTENT_CHANGE_RESPONSE, success);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Error submitting content change: " + e.getMessage());
        }
    }
}
