package server.handler;

import common.content.City;
import common.enums.ActionType;
import common.messaging.Message;
import server.repository.CityRepository;

import java.util.Optional;

/**
 * Handler for getting detailed city information with all relationships loaded.
 */
public class GetCityDetailsHandler implements RequestHandler {

    private final CityRepository cityRepository = CityRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            int cityId = (Integer) request.getMessage();
            
            Optional<City> optionalCity = cityRepository.findByIdWithAll(cityId);
            
            if (optionalCity.isEmpty()) {
                return new Message(ActionType.ERROR, "City not found with ID: " + cityId);
            }
            
            return new Message(ActionType.GET_CITY_DETAILS_RESPONSE, optionalCity.get());

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Error fetching city details: " + e.getMessage());
        }
    }
}
