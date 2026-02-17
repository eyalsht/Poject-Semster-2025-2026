package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import server.repository.UserRepository;

import java.util.ArrayList;

public class CheckRegistrationFieldsHandler implements RequestHandler {

    private final UserRepository userRepository = UserRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            ArrayList<String> data = (ArrayList<String>) request.getMessage();
            String username = data.get(0);
            String email = data.get(1);

            if (userRepository.isUsernameTaken(username)) {
                return new Message(ActionType.CHECK_REGISTRATION_FIELDS_RESPONSE,
                        "Username is already taken.");
            }

            if (userRepository.isEmailTaken(email)) {
                return new Message(ActionType.CHECK_REGISTRATION_FIELDS_RESPONSE,
                        "Email is already in use.");
            }

            // null = no problems
            return new Message(ActionType.CHECK_REGISTRATION_FIELDS_RESPONSE, null);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.CHECK_REGISTRATION_FIELDS_RESPONSE,
                    "Server error while checking availability.");
        }
    }
}