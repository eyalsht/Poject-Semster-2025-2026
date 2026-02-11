package server.handler;

import common.dto.SubscriptionStatusDTO;
import common.enums.ActionType;
import common.messaging.Message;
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

            // Use native SQL to avoid loading full entities (which corrupts connection pool)
            List<Object[]> rows = repo.findActiveSubscriptionDTOsNative(userId);
            ArrayList<SubscriptionStatusDTO> result = new ArrayList<>();

            for (Object[] row : rows) {
                // row = { city_id (Integer), expiration_date (java.sql.Date), city_name (String), price_sub (Double) }
                int cityId = ((Number) row[0]).intValue();

                LocalDate expiry = null;
                if (row[1] != null) {
                    if (row[1] instanceof java.sql.Date) {
                        expiry = ((java.sql.Date) row[1]).toLocalDate();
                    } else if (row[1] instanceof LocalDate) {
                        expiry = (LocalDate) row[1];
                    } else {
                        expiry = LocalDate.parse(row[1].toString());
                    }
                }

                boolean active = expiry != null && !expiry.isBefore(LocalDate.now());
                String cityName = (String) row[2];
                double pricePerMonth = ((Number) row[3]).doubleValue();

                result.add(new SubscriptionStatusDTO(active, expiry, cityName, cityId, pricePerMonth));
            }

            return new Message(ActionType.GET_USER_SUBSCRIPTIONS_RESPONSE, result);

        } catch (Exception e) {
            System.err.println("GetUserSubscriptionsHandler failed: " + e.getMessage());
            e.printStackTrace();
            return new Message(ActionType.GET_USER_SUBSCRIPTIONS_RESPONSE, new ArrayList<>());
        }
    }
}
