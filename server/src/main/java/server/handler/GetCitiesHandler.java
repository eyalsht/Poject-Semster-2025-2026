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
            List<City> allCities = cp.findAll();
            ArrayList<City> safeCities = new ArrayList<>();
            for (City c : allCities) {
                City cleanCity = new City();
                cleanCity.setId(c.getId());
                cleanCity.setName(c.getName());

                cleanCity.setSites(null);
                cleanCity.setMaps(null);

                safeCities.add(cleanCity);
            }

            return new Message(ActionType.GET_CITIES_RESPONSE, safeCities);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Cities - Database error: " + e.getMessage());
        }
    }

}