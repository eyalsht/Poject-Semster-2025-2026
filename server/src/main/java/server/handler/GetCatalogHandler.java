package server.handler;

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
            
            // Always include all city names for the dropdown
            List<String> allCityNames = cityRepository.findAllCityNames();
            response.setAvailableCities(new ArrayList<>(allCityNames));
            
            // Get filtered maps
            List<GCMMap> maps = mapRepository.findByCriteria(
                filter.getCityName(),
                filter.getMapName(),
                filter.getVersion()
            );
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
