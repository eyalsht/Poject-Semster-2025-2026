package server.handler;

import common.content.Site;
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
            String cityName = (String) request.getMessage();
            List<Tour> allCityTours = tp.findToursByCityName(cityName);
            // Tour -> Sites -> City -> (all Tours, Sites, Maps) -> ...
            for (Tour tour : allCityTours) {
                tour.setCity(null);
                if (tour.getSites() != null) {
                    for (Site site : tour.getSites()) {
                        site.setCity(null);
                        site.setMaps(new ArrayList<>());
                        site.setTours(new ArrayList<>());
                    }
                }
            }

            return new Message(ActionType.GET_CITY_TOURS_RESPONSE, allCityTours);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Tours - Handler error: " + e.getMessage());
        }
    }
}
