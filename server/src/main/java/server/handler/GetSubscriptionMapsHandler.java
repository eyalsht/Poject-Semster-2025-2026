package server.handler;

import common.content.City;
import common.content.GCMMap;
import common.enums.ActionType;
import common.messaging.Message;
import server.repository.CityRepository;
import server.repository.PurchaseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handler for getting live maps from a user's active city subscriptions.
 * Returns current catalog versions (not snapshots) grouped by city.
 */
public class GetSubscriptionMapsHandler implements RequestHandler {

    private final PurchaseRepository purchaseRepository = PurchaseRepository.getInstance();
    private final CityRepository cityRepository = CityRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            int userId = (Integer) request.getMessage();

            // Get active subscriptions (each row: city_id, expiry, city_name, price_sub)
            List<Object[]> activeSubs = purchaseRepository.findActiveSubscriptionDTOsNative(userId);

            ArrayList<City> result = new ArrayList<>();

            for (Object[] sub : activeSubs) {
                int cityId = ((Number) sub[0]).intValue();

                Optional<City> optionalCity = cityRepository.findByIdWithMaps(cityId);
                if (optionalCity.isEmpty()) continue;

                City dbCity = optionalCity.get();

                // Build clean detached objects (same pattern as GetCityDetailsHandler)
                City cleanCity = new City();
                cleanCity.setId(dbCity.getId());
                cleanCity.setName(dbCity.getName());
                cleanCity.setDescription(dbCity.getDescription());
                cleanCity.setPriceSub(dbCity.getPriceSub());
                cleanCity.setImagePath(dbCity.getImagePath());

                boolean hasMap = false;
                if (dbCity.getMaps() != null) {
                    for (GCMMap m : dbCity.getMaps()) {
                        GCMMap cleanMap = new GCMMap(
                                m.getId(), m.getName(), m.getDescription(),
                                m.getVersion(), m.getPrice(), m.getStatus()
                        );
                        cleanMap.setImagePath(m.getImagePath());
                        // Add directly to internal list, bypassing City.setMaps() TreeSet logic
                        cleanCity.getMaps().add(cleanMap);
                        hasMap = true;
                        System.out.println("[SubMaps] City=" + dbCity.getName()
                                + " Map=" + cleanMap.getName()
                                + " Desc=" + cleanMap.getDescription());
                    }
                }

                if (hasMap) {
                    result.add(cleanCity);
                }
            }

            System.out.println("[SubMaps] Returning " + result.size() + " cities for userId=" + userId);
            return new Message(ActionType.GET_SUBSCRIPTION_MAPS_RESPONSE, result);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.GET_SUBSCRIPTION_MAPS_RESPONSE, new ArrayList<>());
        }
    }
}
