package server.handler;

import common.content.City;
import common.dto.SubscriptionStatusDTO;
import common.enums.ActionType;
import common.messaging.Message;
import common.purchase.Subscription;
import server.repository.CityRepository;
import server.repository.PurchaseRepository;

import java.time.LocalDate;
import java.util.ArrayList;

public class CheckSubscriptionStatusHandler implements RequestHandler {

    @Override
    public Message handle(Message request) {
        try {
            ArrayList<Integer> params = (ArrayList<Integer>) request.getMessage();
            int userId = params.get(0);
            int cityId = params.get(1);

            PurchaseRepository purchaseRepo = PurchaseRepository.getInstance();
            Subscription latest = purchaseRepo.findLatestSubscription(userId, cityId);

            if (latest == null) {
                // No subscription found - return inactive status with city info
                City city = CityRepository.getInstance().findById(cityId).orElse(null);
                double pricePerMonth = city != null ? city.getPriceSub() : 0;
                String cityName = city != null ? city.getName() : "Unknown";

                return new Message(ActionType.CHECK_SUBSCRIPTION_STATUS_RESPONSE,
                    new SubscriptionStatusDTO(false, null, cityName, cityId, pricePerMonth));
            }

            LocalDate expiry = latest.getExpirationDate();
            boolean active = expiry != null && !expiry.isBefore(LocalDate.now());

            String cityName = latest.getCity() != null ? latest.getCity().getName() : "Unknown";
            double pricePerMonth = latest.getCity() != null ? latest.getCity().getPriceSub() : 0;

            return new Message(ActionType.CHECK_SUBSCRIPTION_STATUS_RESPONSE,
                new SubscriptionStatusDTO(active, expiry, cityName, cityId, pricePerMonth));

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.CHECK_SUBSCRIPTION_STATUS_RESPONSE,
                new SubscriptionStatusDTO(false, null, "Error", 0, 0));
        }
    }
}
