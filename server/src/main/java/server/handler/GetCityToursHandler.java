package server.handler;

import common.content.Tour;
import common.enums.ActionType;
import common.messaging.Message;
import server.repository.TourRepository;

import java.util.ArrayList;
import java.util.List;

public class GetCityToursHandler implements RequestHandler{
    private TourRepository tp = TourRepository.getInstance();
    @Override
    public Message handle(Message request) {
        try{
            List<Tour> allCityTours = new ArrayList<>();
            String cityName = (String) request.getMessage();
            allCityTours = tp.findToursByCityName(cityName);
            return new Message(ActionType.GET_CITY_TOURS_RESPONSE,allCityTours);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Tours - Handler error: " + e.getMessage());
        }
    }
}
