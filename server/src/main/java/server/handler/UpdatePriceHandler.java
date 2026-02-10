package server.handler;

import common.content.City;
import common.content.GCMMap;
import common.dto.PriceChangeRequestDTO;
import common.enums.ActionType;
import common.messaging.Message;
import common.user.User;
import common.workflow.PendingPriceUpdate;
import server.repository.CityRepository;
import server.repository.MapRepository;
import server.repository.PendingPriceUpdateRepository;
import server.repository.UserRepository;

import java.util.Map;
import java.util.Optional;

/**
 * Handles UPDATE_PRICE_REQUEST.
 * Accepts a PriceChangeRequestDTO and creates PendingPriceUpdate records
 * for each changed map price. Does NOT apply changes directly.
 */
public class UpdatePriceHandler implements RequestHandler {

    private final MapRepository mapRepository = MapRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();
    private final CityRepository cityRepository = CityRepository.getInstance();
    private final PendingPriceUpdateRepository pendingRepository = PendingPriceUpdateRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            PriceChangeRequestDTO dto = (PriceChangeRequestDTO) request.getMessage();

            // Resolve requester (findById returns Optional<User>)
            User requester = null;
            if (dto.getRequesterId() != null) {
                Optional<User> optUser = userRepository.findById(dto.getRequesterId());
                requester = optUser.orElse(null);
            }

            int createdCount = 0;

            // 1) Handle individual map price changes
            Map<Integer, Double> newPrices = dto.getMapPriceChanges();
            Map<Integer, Double> oldPrices = dto.getMapOldPrices();

            for (Map.Entry<Integer, Double> entry : newPrices.entrySet()) {
                int mapId = entry.getKey();
                double newPrice = entry.getValue();
                Double oldPrice = oldPrices.get(mapId);

                if (oldPrice != null && Double.compare(oldPrice, newPrice) != 0) {
                    Optional<GCMMap> optMap = mapRepository.findById(mapId);
                    if (optMap.isPresent()) {
                        GCMMap map = optMap.get();
                        PendingPriceUpdate pending = new PendingPriceUpdate(map, requester, oldPrice, newPrice);
                        pendingRepository.save(pending);
                        createdCount++;
                    }
                }
            }

            // 2) Handle subscription price change
            if (Double.compare(dto.getOldSubscriptionPrice(), dto.getNewSubscriptionPrice()) != 0) {
                // PendingPriceUpdate requires a non-null map (map_id NOT NULL in DB),
                // so we use the city's first map as a carrier for the subscription change.
                Optional<City> optCity = cityRepository.findByIdWithMaps(dto.getCityId());
                if (optCity.isPresent()) {
                    City city = optCity.get();
                    if (city.getMaps() != null && !city.getMaps().isEmpty()) {
                        GCMMap firstMap = city.getMaps().get(0);
                        PendingPriceUpdate subPending = new PendingPriceUpdate(
                                firstMap, requester,
                                dto.getOldSubscriptionPrice(),
                                dto.getNewSubscriptionPrice()
                        );
                        pendingRepository.save(subPending);
                        createdCount++;
                    }
                }
            }

            return new Message(ActionType.UPDATE_PRICE_RESPONSE, createdCount > 0);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.UPDATE_PRICE_RESPONSE, false);
        }
    }
}
