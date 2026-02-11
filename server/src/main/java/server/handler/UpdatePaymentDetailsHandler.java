package server.handler;

import common.enums.ActionType;
import common.messaging.Message;
import common.purchase.PaymentDetails;
import common.user.Client;
import server.repository.UserRepository;

import java.util.ArrayList;
import java.util.Optional;

public class UpdatePaymentDetailsHandler implements RequestHandler {

    @Override
    public Message handle(Message request) {
        try {
            ArrayList<Object> data = (ArrayList<Object>) request.getMessage();
            int userId = (Integer) data.get(0);
            PaymentDetails newPayment = (PaymentDetails) data.get(1);

            UserRepository userRepo = UserRepository.getInstance();
            Optional<Client> optClient = userRepo.findClientById(userId);

            if (optClient.isEmpty()) {
                return new Message(ActionType.UPDATE_PAYMENT_DETAILS_RESPONSE, false);
            }

            userRepo.updatePaymentDetails(userId, newPayment);

            return new Message(ActionType.UPDATE_PAYMENT_DETAILS_RESPONSE, true);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.UPDATE_PAYMENT_DETAILS_RESPONSE, false);
        }
    }
}
