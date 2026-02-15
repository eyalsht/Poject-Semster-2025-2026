package server.handler;

import common.content.*;
import common.enums.ActionType;
import common.enums.MapStatus;
import common.messaging.Message;
import org.hibernate.Session;
import server.HibernateUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for getting full city data including map images, markers, sites, and tours.
 * Used by Edit Mode to load everything needed for content editing.
 *
 * Fetches each collection separately to avoid MultipleBagFetchException.
 */
public class GetCityFullDetailsHandler implements RequestHandler {

    @Override
    public Message handle(Message request) {
        try {
            int cityId = (Integer) request.getMessage();

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {

                // 1. Fetch city with sites
                City cityWithSites = session.createQuery(
                        "FROM City c LEFT JOIN FETCH c.sites WHERE c.id = :id", City.class)
                        .setParameter("id", cityId)
                        .uniqueResult();

                if (cityWithSites == null) {
                    return new Message(ActionType.ERROR, "City not found with ID: " + cityId);
                }

                // 2. Fetch city with maps (separate query, same session)
                City cityWithMaps = session.createQuery(
                        "FROM City c LEFT JOIN FETCH c.maps WHERE c.id = :id", City.class)
                        .setParameter("id", cityId)
                        .uniqueResult();

                // 3. Fetch city with tours (separate query, same session)
                City cityWithTours = session.createQuery(
                        "FROM City c LEFT JOIN FETCH c.tours WHERE c.id = :id", City.class)
                        .setParameter("id", cityId)
                        .uniqueResult();

                // Now build clean detached objects
                City cleanCity = new City();
                cleanCity.setId(cityWithSites.getId());
                cleanCity.setName(cityWithSites.getName());
                cleanCity.setDescription(cityWithSites.getDescription());
                cleanCity.setPriceSub(cityWithSites.getPriceSub());
                cleanCity.setImagePath(cityWithSites.getImagePath());

                // Build clean sites list
                List<Site> cleanSites = new ArrayList<>();
                if (cityWithSites.getSites() != null) {
                    for (Site s : cityWithSites.getSites()) {
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
                if (cityWithMaps != null && cityWithMaps.getMaps() != null) {
                    for (GCMMap m : cityWithMaps.getMaps()) {
                        // Filter out EXTERNAL maps (they belong to the external repository)
                        if (m.getStatus() == MapStatus.EXTERNAL) continue;
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

                        // Copy site references for this map (sites are eagerly fetched on GCMMap)
                        List<Site> mapSites = new ArrayList<>();
                        if (m.getSites() != null) {
                            for (Site ms : m.getSites()) {
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
                if (cityWithTours != null && cityWithTours.getTours() != null) {
                    for (Tour t : cityWithTours.getTours()) {
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
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Error fetching full city details: " + e.getMessage());
        }
    }
}
