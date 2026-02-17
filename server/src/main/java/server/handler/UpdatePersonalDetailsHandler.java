package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import server.repository.UserRepository;

import java.util.ArrayList;

public class UpdatePersonalDetailsHandler implements RequestHandler {

    @Override
    public Message handle(Message request) {
        try {
            ArrayList<Object> data = (ArrayList<Object>) request.getMessage();
            int userId = (Integer) data.get(0);
            String firstName = (String) data.get(1);
            String lastName = (String) data.get(2);
            String email = (String) data.get(3);
            String phoneNumber = (String) data.get(4);

            UserRepository userRepo = UserRepository.getInstance();

            if (userRepo.isEmailTakenByOther(email, userId)) {
                return new Message(ActionType.UPDATE_PERSONAL_DETAILS_RESPONSE, "Email is already in use by another account.");
            }

            userRepo.updatePersonalDetails(userId, firstName, lastName, email, phoneNumber);

            return new Message(ActionType.UPDATE_PERSONAL_DETAILS_RESPONSE, "OK");

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.UPDATE_PERSONAL_DETAILS_RESPONSE, "Server error: " + e.getMessage());
        }
    }
}
