package server.handler;

import common.content.City;
import common.content.GCMMap;
import common.enums.ActionType;
import common.messaging.Message;
import server.repository.MapRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for fetching maps available in the external repository (status=EXTERNAL).
 * Returns lightweight copies without blob data.
 */
public class GetExternalMapsHandler implements RequestHandler {

    private final MapRepository mapRepository = MapRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            List<GCMMap> externalMaps = mapRepository.findExternalMaps();

            // Build lightweight copies (no blobs)
            List<GCMMap> lightMaps = new ArrayList<>();
            for (GCMMap map : externalMaps) {
                GCMMap light = new GCMMap();
                light.setId(map.getId());
                light.setName(map.getName());
                light.setDescription(map.getDescription());
                light.setVersion(map.getVersion());
                light.setPrice(map.getPrice());
                light.setStatus(map.getStatus());
                light.setImagePath(map.getImagePath());

                if (map.getCity() != null) {
                    City lightCity = new City();
                    lightCity.setId(map.getCity().getId());
                    lightCity.setName(map.getCity().getName());
                    lightCity.setDescription(map.getCity().getDescription());
                    lightCity.setPriceSub(map.getCity().getPriceSub());
                    light.setCity(lightCity);
                }
                lightMaps.add(light);
            }

            return new Message(ActionType.GET_EXTERNAL_MAPS_RESPONSE, lightMaps);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Error fetching external maps: " + e.getMessage());
        }
    }
}
