package server.handler;

import common.content.*;
import common.enums.ActionType;
import common.messaging.Message;
import server.repository.CityRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handler for getting full city data including map images, markers, sites, and tours.
 * Used by Edit Mode to load everything needed for content editing.
 */
public class GetCityFullDetailsHandler implements RequestHandler {

    private final CityRepository cityRepository = CityRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            int cityId = (Integer) request.getMessage();

            Optional<City> optionalCity = cityRepository.findByIdWithAll(cityId);

            if (optionalCity.isEmpty()) {
                return new Message(ActionType.ERROR, "City not found with ID: " + cityId);
            }

            City dbCity = optionalCity.get();

            // Build clean detached City with ALL data including images and markers
            City cleanCity = new City();
            cleanCity.setId(dbCity.getId());
            cleanCity.setName(dbCity.getName());
            cleanCity.setDescription(dbCity.getDescription());
            cleanCity.setPriceSub(dbCity.getPriceSub());
            cleanCity.setImagePath(dbCity.getImagePath());

            // Build clean sites list
            List<Site> cleanSites = new ArrayList<>();
            if (dbCity.getSites() != null) {
                for (Site s : dbCity.getSites()) {
                    Site cleanSite = new Site();
                    cleanSite.setId(s.getId());
                    cleanSite.setName(s.getName());
                    cleanSite.setDescription(s.getDescription());
                    cleanSite.setCategory(s.getCategory());
                    cleanSite.setAccessible(s.isAccessible());
                    cleanSite.setRecommendedVisitDuration(s.getRecommendedVisitDuration());
                    cleanSite.setLocation(s.getLocation());
                    cleanSites.add(cleanSite);
                }
            }
            cleanCity.setSites(cleanSites);

            // Build clean maps list WITH mapImage bytes and siteMarkersJson
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
                    cleanMap.setMapImage(m.getMapImage());
                    cleanMap.setSiteMarkersJson(m.getSiteMarkersJson());

                    // Copy site references for this map
                    List<Site> mapSites = new ArrayList<>();
                    if (m.getSites() != null) {
                        for (Site ms : m.getSites()) {
                            // Find matching clean site by ID
                            for (Site cs : cleanSites) {
                                if (cs.getId() == ms.getId()) {
                                    mapSites.add(cs);
                                    break;
                                }
                            }
                        }
                    }
                    cleanMap.setSites(mapSites);
                    cleanMaps.add(cleanMap);
                }
            }
            cleanCity.setMaps(cleanMaps);

            // Build clean tours list with ordered site references
            List<Tour> cleanTours = new ArrayList<>();
            if (dbCity.getTours() != null) {
                for (Tour t : dbCity.getTours()) {
                    Tour cleanTour = new Tour();
                    cleanTour.setId(t.getId());
                    cleanTour.setName(t.getName());
                    cleanTour.setDescription(t.getDescription());
                    cleanTour.setRecommendedDuration(t.getRecommendedDuration());

                    // Copy ordered site references
                    List<Site> tourSites = new ArrayList<>();
                    if (t.getSites() != null) {
                        for (Site ts : t.getSites()) {
                            for (Site cs : cleanSites) {
                                if (cs.getId() == ts.getId()) {
                                    tourSites.add(cs);
                                    break;
                                }
                            }
                        }
                    }
                    cleanTour.setSites(tourSites);
                    cleanTours.add(cleanTour);
                }
            }
            cleanCity.setTours(cleanTours);

            return new Message(ActionType.GET_CITY_FULL_DETAILS_RESPONSE, cleanCity);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Error fetching full city details: " + e.getMessage());
        }
    }
}
