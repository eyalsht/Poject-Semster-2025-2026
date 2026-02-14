package server.handler;

import common.content.City;
import common.content.GCMMap;
import common.dto.CatalogFilter;
import common.dto.CatalogResponse;
import common.enums.ActionType;
import common.messaging.Message;
import server.repository.CityRepository;
import server.repository.MapRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Consolidated handler for all catalog-related queries.
 * Returns cities, maps, and filter options in a single response.
 */
public class GetCatalogHandler implements RequestHandler {

    private final CityRepository cityRepository = CityRepository.getInstance();
    private final MapRepository mapRepository = MapRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            // Parse filter from request
            CatalogFilter filter = parseFilter(request.getMessage());

            // Build response
            CatalogResponse response = new CatalogResponse();

            // Check if this is a search request
            if (filter.isSearchMode()) {
                return handleSearchRequest(filter, response);
            }

            // Regular catalog mode
            // Always include all city names for the dropdown
            List<String> allCityNames = cityRepository.findAllCityNames();
            response.setAvailableCities(new ArrayList<>(allCityNames));

            // Get filtered maps
            List<GCMMap> maps = mapRepository.findByCriteria(
                filter.getCityName(),
                filter.getMapName(),
                filter.getVersion()
            );

            // Strip heavy blob data before sending — catalog only needs name/price/imagePath
            for (GCMMap map : maps) {
                map.setMapImage(null);
                map.setSiteMarkersJson(null);
            }

            response.setMaps(new ArrayList<>(maps));

            // Populate available map names (for selected city, or all if no city selected)
            if (filter.getCityName() != null) {
                List<String> mapNames = mapRepository.findMapNamesByCity(filter.getCityName());
                response.setAvailableMapNames(new ArrayList<>(mapNames));

                // Populate available versions (for selected city+map)
                if (filter.getMapName() != null) {
                    List<String> versions = mapRepository.findVersionsByCityAndMapName(
                        filter.getCityName(), filter.getMapName()
                    );
                    response.setAvailableVersions(new ArrayList<>(versions));
                }
            }

            return new Message(ActionType.GET_CATALOG_RESPONSE, response);

        } catch (Exception e) {
            e.printStackTrace();
            return new Message(ActionType.ERROR, "Error fetching catalog: " + e.getMessage());
        }
    }

    /**
     * Handle search mode request - returns cities matching query with counts.
     */
    private Message handleSearchRequest(CatalogFilter filter, CatalogResponse response) {
        String searchQuery = filter.getSearchQuery();

        // Get search results with counts
        List<Object[]> results = cityRepository.searchWithCounts(searchQuery);

        List<CatalogResponse.CitySearchResult> searchResults = new ArrayList<>();
        for (Object[] row : results) {
            City city = (City) row[0];
            Long mapCount = (Long) row[1];
            Long siteCount = (Long) row[2];
            Long tourCount = (Long) row[3];

            // Strip heavy blob data from eagerly-loaded maps
            if (city.getMaps() != null) {
                for (GCMMap m : city.getMaps()) {
                    m.setMapImage(null);
                    m.setSiteMarkersJson(null);
                }
            }

            CatalogResponse.CitySearchResult result = new CatalogResponse.CitySearchResult();
            result.setCity(city);
            result.setMapCount(mapCount.intValue());
            result.setSiteCount(siteCount.intValue());
            result.setTourCount(tourCount.intValue());

            // Get map descriptions for this city
            List<String> mapDescriptions = cityRepository.getMapDescriptionsForCity(city.getId());
            result.setMapDescriptions(new ArrayList<>(mapDescriptions));

            searchResults.add(result);
        }

        response.setSearchResults(searchResults);
        return new Message(ActionType.GET_CATALOG_RESPONSE, response);
    }

    /**
     * Parse filter from various input formats for backward compatibility.
     */
    private CatalogFilter parseFilter(Object message) {
        if (message == null) {
            return new CatalogFilter();
        }
        
        if (message instanceof CatalogFilter) {
            return (CatalogFilter) message;
        }
        
        // Backward compatibility: support old List<String> format
        if (message instanceof List<?> list) {
            String city = list.size() > 0 ? (String) list.get(0) : null;
            String map = list.size() > 1 ? (String) list.get(1) : null;
            String version = list.size() > 2 ? (String) list.get(2) : null;
            return new CatalogFilter(city, map, version);
        }
        
        return new CatalogFilter();
    }
}
