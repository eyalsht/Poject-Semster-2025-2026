package server.handler;

import common.content.City;
import common.content.GCMMap;
import common.enums.ActionType;
import common.messaging.Message;
import server.repository.CityRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handler for getting detailed city information with maps loaded.
 * Creates clean detached objects to avoid Hibernate serialization issues
 * (LazyInitializationException, MultipleBagFetchException).
 */
public class GetCityDetailsHandler implements RequestHandler {

    private final CityRepository cityRepository = CityRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            int cityId = (Integer) request.getMessage();

            Optional<City> optionalCity = cityRepository.findByIdWithMaps(cityId);

            if (optionalCity.isEmpty()) {
                return new Message(ActionType.ERROR, "City not found with ID: " + cityId);
            }

            City dbCity = optionalCity.get();

            // Build clean objects free of Hibernate proxies and circular references
            City cleanCity = new City();
            cleanCity.setId(dbCity.getId());
            cleanCity.setName(dbCity.getName());
            cleanCity.setDescription(dbCity.getDescription());
            cleanCity.setPriceSub(dbCity.getPriceSub());
            cleanCity.setImagePath(dbCity.getImagePath());

            List<GCMMap> cleanMaps = new ArrayList<>();
            if (dbCity.getMaps() != null) {
                for (GCMMap m : dbCity.getMaps()) {
                    GCMMap cleanMap = new GCMMap();
                    cleanMap.setId(m.getId());
                    cleanMap.setName(m.getName());
                    cleanMap.setDescription(m.getDescription());
                    cleanMap.setVersion(m.getVersion());
                    cleanMap.setPrice(m.getPrice());
                    cleanMap.setStatus(m.getStatus());
                    cleanMap.setImagePath(m.getImagePath());
                    cleanMaps.add(cleanMap);
                }
            }
            cleanCity.setMaps(cleanMaps);

            return new Message(ActionType.GET_CITY_DETAILS_RESPONSE, cleanCity);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Error fetching city details: " + e.getMessage());
        }
    }
}
