package server.handler;

import common.content.Site;
import common.dto.GetSitesResponse;
import common.enums.ActionType;
import common.messaging.Message;
import server.repository.SiteRepository;

import java.util.List;

public class GetSitesHandler implements RequestHandler{

    private final SiteRepository siteRepository = SiteRepository.getInstance();

    @Override
    public Message handle(Message request) {
        String cityName = (String) request.getMessage();
        try{
            List<Site> sites = siteRepository.findSitesByCityName(cityName);
            GetSitesResponse responseBody = new GetSitesResponse(sites);
            return new Message(ActionType.GET_CITY_SITES_RESPONSE, responseBody);
        }
        catch(Exception e){
            e.printStackTrace();
            return new Message(ActionType.ERROR, "GetSitesHandler - Database error: " + e.getMessage());
        }
    }
}
