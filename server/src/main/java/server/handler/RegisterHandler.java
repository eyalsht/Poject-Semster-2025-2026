package server.handler;

import common.dto.AuthResponse;
import common.enums.ActionType;
import common.messaging.Message;
import server.repository.UserRepository;

import java.util.ArrayList;

public class RegisterHandler implements RequestHandler {

    private final UserRepository userRepository = UserRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            ArrayList<String> data = (ArrayList<String>) request.getMessage();

            String firstName = data.get(0);
            String lastName = data.get(1);
            String idNumber = data.get(2);
            String email = data.get(3);
            String password = data.get(4);
            String card = data.get(5);

            boolean success = userRepository.registerClient(
                firstName, lastName, idNumber, email, password, card
            );

            if (success) {
                return new Message(ActionType.REGISTER_RESPONSE,
                    AuthResponse.success("Registration successful! You can now log in."));
            } else {
                return new Message(ActionType.REGISTER_RESPONSE,
                    AuthResponse.failure("User already exists with this email or ID."));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.REGISTER_RESPONSE,
                AuthResponse.failure("Server error during registration."));
        }
    }
}
