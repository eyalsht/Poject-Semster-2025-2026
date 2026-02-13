package server.handler;

import common.content.City;
import common.content.GCMMap;
import common.content.Site;
import common.content.Tour;
import common.enums.ActionType;
import common.messaging.Message;
import org.hibernate.Session;
import server.HibernateUtil;
import server.repository.CityRepository;
import server.repository.PurchaseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handler for getting live maps from a user's active city subscriptions.
 * Returns current catalog versions (not snapshots) grouped by city.
 */
public class GetSubscriptionMapsHandler implements RequestHandler {

    private final PurchaseRepository purchaseRepository = PurchaseRepository.getInstance();
    private final CityRepository cityRepository = CityRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            int userId = (Integer) request.getMessage();

            // Get active subscriptions (each row: city_id, expiry, city_name, price_sub)
            List<Object[]> activeSubs = purchaseRepository.findActiveSubscriptionDTOsNative(userId);

            ArrayList<City> result = new ArrayList<>();

            for (Object[] sub : activeSubs) {
                int cityId = ((Number) sub[0]).intValue();

                // Fetch city with maps and tours in separate queries to avoid MultipleBagFetchException
                try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                    City cityWithMaps = session.createQuery(
                            "FROM City c LEFT JOIN FETCH c.maps WHERE c.id = :id", City.class)
                            .setParameter("id", cityId)
                            .uniqueResult();

                    if (cityWithMaps == null) continue;

                    City cityWithTours = session.createQuery(
                            "FROM City c LEFT JOIN FETCH c.tours WHERE c.id = :id", City.class)
                            .setParameter("id", cityId)
                            .uniqueResult();

                    // Build clean detached objects
                    City cleanCity = new City();
                    cleanCity.setId(cityWithMaps.getId());
                    cleanCity.setName(cityWithMaps.getName());
                    cleanCity.setDescription(cityWithMaps.getDescription());
                    cleanCity.setPriceSub(cityWithMaps.getPriceSub());
                    cleanCity.setImagePath(cityWithMaps.getImagePath());

                    // Collect all clean sites (keyed by ID to reuse across maps and tours)
                    List<Site> allCleanSites = new ArrayList<>();

                    boolean hasMap = false;
                    if (cityWithMaps.getMaps() != null) {
                        for (GCMMap m : cityWithMaps.getMaps()) {
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
                            cleanMap.setCity(cleanCity);

                            // Copy sites for this map
                            List<Site> mapCleanSites = new ArrayList<>();
                            if (m.getSites() != null) {
                                for (Site s : m.getSites()) {
                                    Site cleanSite = findOrCreateCleanSite(allCleanSites, s);
                                    mapCleanSites.add(cleanSite);
                                }
                            }
                            cleanMap.setSites(mapCleanSites);

                            cleanCity.getMaps().add(cleanMap);
                            hasMap = true;
                        }
                    }

                    // Build clean tours so getAvailableTours() works in the popup
                    if (cityWithTours != null && cityWithTours.getTours() != null) {
                        List<Tour> cleanTours = new ArrayList<>();
                        for (Tour t : cityWithTours.getTours()) {
                            Tour cleanTour = new Tour();
                            cleanTour.setId(t.getId());
                            cleanTour.setName(t.getName());
                            cleanTour.setDescription(t.getDescription());
                            cleanTour.setRecommendedDuration(t.getRecommendedDuration());

                            List<Site> tourSites = new ArrayList<>();
                            if (t.getSites() != null) {
                                for (Site s : t.getSites()) {
                                    tourSites.add(findOrCreateCleanSite(allCleanSites, s));
                                }
                            }
                            cleanTour.setSites(tourSites);
                            cleanTours.add(cleanTour);
                        }
                        cleanCity.setTours(cleanTours);
                    }

                    if (hasMap) {
                        result.add(cleanCity);
                    }
                }
            }

            return new Message(ActionType.GET_SUBSCRIPTION_MAPS_RESPONSE, result);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.GET_SUBSCRIPTION_MAPS_RESPONSE, new ArrayList<>());
        }
    }

    private Site findOrCreateCleanSite(List<Site> pool, Site source) {
        for (Site existing : pool) {
            if (existing.getId() == source.getId()) return existing;
        }
        Site cleanSite = new Site();
        cleanSite.setId(source.getId());
        cleanSite.setName(source.getName());
        cleanSite.setDescription(source.getDescription());
        cleanSite.setCategory(source.getCategory());
        cleanSite.setAccessible(source.isAccessible());
        cleanSite.setRecommendedVisitDuration(source.getRecommendedVisitDuration());
        cleanSite.setLocation(source.getLocation());
        pool.add(cleanSite);
        return cleanSite;
    }
}
