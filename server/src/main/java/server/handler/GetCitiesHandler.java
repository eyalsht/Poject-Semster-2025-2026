package server.handler;

import common.content.City;
import common.enums.ActionType;
import common.messaging.Message;
import server.repository.CityRepository;

import java.util.ArrayList;
import java.util.List;

public class GetCitiesHandler implements RequestHandler
{
    private final CityRepository cp = CityRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            // 1. Fetch from DB
            List<City> allCities = cp.findAll();

            // 2. "Strip" the objects to avoid Hibernate serialization crashes
            ArrayList<City> safeCities = new ArrayList<>();
            for (City c : allCities) {
                City cleanCity = new City();
                cleanCity.setId(c.getId());
                cleanCity.setName(c.getName());

                // EXPLICITLY null these out if they exist in your City class
                // This prevents the Serializer from trying to "fetch" sites/maps
                cleanCity.setSites(null);
                cleanCity.setMaps(null);

                safeCities.add(cleanCity);
            }

            // 3. Return the response message
            return new Message(ActionType.GET_CITIES_RESPONSE, safeCities);

        } catch (Exception e) {
            e.printStackTrace();
            // CRITICAL: Return an error message so the client isn't left waiting!
            return new Message(ActionType.ERROR, "Database error: " + e.getMessage());
        }
    }

}