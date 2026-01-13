package server.handler;

import common.content.GCMMap;
import common.enums.ActionType;
import common.messaging.Message;
import common.user.User;
import server.repository.*;

import java.util.ArrayList;
import java.util.Optional;

public class UpdatePriceHandler implements RequestHandler {

    private final MapRepository mapRepository = MapRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();
    private final PendingPriceUpdateRepository pendingRepository = PendingPriceUpdateRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            ArrayList<Object> params = (ArrayList<Object>) request.getMessage();

            String cityName = (String) params.get(0);
            String mapName = (String) params.get(1);
            String version = (String) params.get(2);
            double newPrice = (double) params.get(3);
            Integer requesterId = (Integer) params.get(4);

            // Find the map
            Optional<GCMMap> optionalMap = mapRepository.findByCityNameAndVersion(cityName, mapName, version);
            if (optionalMap.isEmpty()) {
                return new Message(ActionType.UPDATE_PRICE_RESPONSE, false);
            }

            // Find the requester (optional)
            User requester = null;
            if (requesterId != null) {
                requester = userRepository.findById(requesterId).orElse(null);
            }

            // Create pending price update
            boolean success = pendingRepository.createPendingUpdate(
                optionalMap.get(), 
                requester, 
                newPrice
            );

            return new Message(ActionType.UPDATE_PRICE_RESPONSE, success);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.UPDATE_PRICE_RESPONSE, false);
        }
    }
}
