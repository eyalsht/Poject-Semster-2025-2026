package server.handler;

import common.dto.SubscriptionStatusDTO;
import common.enums.ActionType;
import common.messaging.Message;
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

            // Use native SQL to avoid loading full entities (which corrupts connection pool)
            Object[] row = purchaseRepo.checkSubscriptionStatusNative(userId, cityId);

            if (row == null) {
                // No subscription found - get city info via lightweight query
                Object[] cityData = CityRepository.getInstance().findNameAndPrice(cityId);
                double pricePerMonth = cityData != null ? ((Number) cityData[1]).doubleValue() : 0;
                String cityName = cityData != null ? (String) cityData[0] : "Unknown";

                return new Message(ActionType.CHECK_SUBSCRIPTION_STATUS_RESPONSE,
                    new SubscriptionStatusDTO(false, null, cityName, cityId, pricePerMonth));
            }

            // row = { expiration_date (java.sql.Date), city_name (String), price_sub (Double) }
            LocalDate expiry = null;
            if (row[0] != null) {
                if (row[0] instanceof java.sql.Date) {
                    expiry = ((java.sql.Date) row[0]).toLocalDate();
                } else if (row[0] instanceof LocalDate) {
                    expiry = (LocalDate) row[0];
                } else {
                    expiry = LocalDate.parse(row[0].toString());
                }
            }

            boolean active = expiry != null && !expiry.isBefore(LocalDate.now());
            String cityName = (String) row[1];
            double pricePerMonth = ((Number) row[2]).doubleValue();

            return new Message(ActionType.CHECK_SUBSCRIPTION_STATUS_RESPONSE,
                new SubscriptionStatusDTO(active, expiry, cityName, cityId, pricePerMonth));

        } catch (Exception e) {
            return new Message(ActionType.CHECK_SUBSCRIPTION_STATUS_RESPONSE,
                new SubscriptionStatusDTO(false, null, "Error", 0, 0));
        }
    }
}
