package server.handler;

import common.content.City;
import common.enums.ActionType;
import common.messaging.Message;
import server.repository.CityRepository;

import java.util.List;

/**
 * Temporary handler for creating new cities.
 * Receives a List containing: [cityName, description].
 */
public class AddCityHandler implements RequestHandler {

    private final CityRepository cityRepository = CityRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> params = (List<String>) request.getMessage();

            String cityName = params.get(0);
            String description = params.size() > 1 ? params.get(1) : "";

            if (cityName == null || cityName.isBlank()) {
                return new Message(ActionType.ADD_CITY_RESPONSE, false);
            }

            // Check if city already exists
            if (cityRepository.findByName(cityName).isPresent()) {
                return new Message(ActionType.ERROR, "City '" + cityName + "' already exists.");
            }

            City city = new City();
            city.setName(cityName);
            city.setDescription(description != null ? description : "");
            city.setPriceSub(0);
            cityRepository.save(city);

            System.out.println("Created new city: " + cityName);
            return new Message(ActionType.ADD_CITY_RESPONSE, true);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ADD_CITY_RESPONSE, false);
        }
    }
}
