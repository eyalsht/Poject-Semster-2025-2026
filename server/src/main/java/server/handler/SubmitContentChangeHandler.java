package server.handler;

import common.dto.ContentChangeRequest;
import common.enums.ActionType;
import common.messaging.Message;
import common.user.User;
import server.repository.PendingContentRequestRepository;
import server.repository.UserRepository;

public class SubmitContentChangeHandler implements RequestHandler {

    private final PendingContentRequestRepository repository = PendingContentRequestRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            ContentChangeRequest changeRequest = (ContentChangeRequest) request.getMessage();

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

            return new Message(ActionType.SUBMIT_CONTENT_CHANGE_RESPONSE, success);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Error submitting content change: " + e.getMessage());
        }
    }
}
