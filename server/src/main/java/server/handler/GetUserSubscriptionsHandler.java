package server.handler;

import common.dto.SubscriptionStatusDTO;
import common.enums.ActionType;
import common.messaging.Message;
import common.purchase.Subscription;
import server.repository.PurchaseRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GetUserSubscriptionsHandler implements RequestHandler {

    @Override
    public Message handle(Message request) {
        try {
            int userId = (Integer) request.getMessage();
            PurchaseRepository repo = PurchaseRepository.getInstance();

            List<Subscription> subs = repo.findActiveSubscriptionsByUserId(userId);
            ArrayList<SubscriptionStatusDTO> result = new ArrayList<>();

            for (Subscription sub : subs) {
                LocalDate expiry = sub.getExpirationDate();
                boolean active = expiry != null && !expiry.isBefore(LocalDate.now());

                String cityName = sub.getCity() != null ? sub.getCity().getName() : "Unknown";
                int cityId = sub.getCity() != null ? sub.getCity().getId() : 0;
                double pricePerMonth = sub.getCity() != null ? sub.getCity().getPriceSub() : 0;

                result.add(new SubscriptionStatusDTO(active, expiry, cityName, cityId, pricePerMonth));
            }

            return new Message(ActionType.GET_USER_SUBSCRIPTIONS_RESPONSE, result);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.GET_USER_SUBSCRIPTIONS_RESPONSE, new ArrayList<>());
        }
    }
}
