package server.handler;

import common.content.City;
import common.content.GCMMap;
import common.content.Site;
import common.content.Tour;
import common.enums.ActionType;
import common.messaging.Message;
import server.repository.CityRepository;
import server.repository.MapRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for fetching a single map with full data (image, sites, markers).
 * Used by the map content popup to load heavy data on demand.
 *
 * Builds a clean detached copy to avoid serializing the entire city graph
 * (which would include ALL other maps' LONGBLOB images).
 */
public class GetMapDetailsHandler implements RequestHandler {

    private final MapRepository mapRepository = MapRepository.getInstance();
    private final CityRepository cityRepository = CityRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            int mapId = (Integer) request.getMessage();

            return mapRepository.findByIdWithSites(mapId)
                .map(this::toCleanCopy)
                .map(map -> new Message(ActionType.GET_MAP_DETAILS_RESPONSE, map))
                .orElse(new Message(ActionType.ERROR, "Map not found with ID: " + mapId));

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Error fetching map details: " + e.getMessage());
        }
    }

    /**
     * Build a clean detached copy of the map with its image, sites, and markers,
     * plus a lightweight city (with tours for getAvailableTours, but no map images).
     */
    private GCMMap toCleanCopy(GCMMap source) {
        GCMMap clean = new GCMMap();
        clean.setId(source.getId());
        clean.setName(source.getName());
        clean.setDescription(source.getDescription());
        clean.setVersion(source.getVersion());
        clean.setPrice(source.getPrice());
        clean.setStatus(source.getStatus());
        clean.setImagePath(source.getImagePath());
        clean.setMapImage(source.getMapImage());
        clean.setSiteMarkersJson(source.getSiteMarkersJson());

        // Copy sites
        List<Site> cleanSites = new ArrayList<>();
        if (source.getSites() != null) {
            for (Site s : source.getSites()) {
                Site cs = new Site();
                cs.setId(s.getId());
                cs.setName(s.getName());
                cs.setDescription(s.getDescription());
                cs.setCategory(s.getCategory());
                cs.setAccessible(s.isAccessible());
                cs.setRecommendedVisitDuration(s.getRecommendedVisitDuration());
                cs.setLocation(s.getLocation());
                cleanSites.add(cs);
            }
        }
        clean.setSites(cleanSites);

        // Build lightweight city with tours (needed for getAvailableTours)
        // but without other maps' images
        if (source.getCity() != null) {
            City sourceCity = source.getCity();
            City cleanCity = new City();
            cleanCity.setId(sourceCity.getId());
            cleanCity.setName(sourceCity.getName());
            cleanCity.setDescription(sourceCity.getDescription());
            cleanCity.setPriceSub(sourceCity.getPriceSub());
            cleanCity.setImagePath(sourceCity.getImagePath());

            // Copy tours with their site references
            List<Tour> cleanTours = new ArrayList<>();
            if (sourceCity.getTours() != null) {
                for (Tour t : sourceCity.getTours()) {
                    Tour ct = new Tour();
                    ct.setId(t.getId());
                    ct.setName(t.getName());
                    ct.setDescription(t.getDescription());
                    ct.setRecommendedDuration(t.getRecommendedDuration());

                    // Map tour sites to clean site references
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
                    ct.setSites(tourSites);
                    cleanTours.add(ct);
                }
            }
            cleanCity.setTours(cleanTours);

            clean.setCity(cleanCity);
        }

        return clean;
    }
}