package server.handler;

import common.content.Site;
import common.enums.ActionType;
import common.messaging.Message;
import server.repository.SiteRepository;

import java.util.ArrayList;
import java.util.List;

public class GetCitySitesHandler implements RequestHandler
{
    private final SiteRepository sp = SiteRepository.getInstance();
    @Override
    public Message handle(Message request) {
        try{
            List<Site> allCitySites = new ArrayList<>();
            String cityName = (String) request.getMessage();
            allCitySites = sp.findSitesByCityName(cityName);
            return new Message(ActionType.GET_CITY_SITES_RESPONSE,allCitySites);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Sites - Database error: " + e.getMessage());
        }
    }
}
