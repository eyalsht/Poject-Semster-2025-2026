package server.handler;

import common.content.City;
import common.content.GCMMap;
import common.content.Tour;
import common.content.Site;
import common.dto.CatalogFilter;
import common.dto.CatalogResponse;
import common.enums.ActionType;
import common.messaging.Message;
import server.repository.CityRepository;
import server.repository.MapRepository;
import server.repository.TourRepository;
import server.repository.SiteRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Consolidated handler for all catalog-related queries.
 * Returns cities, maps, and filter options in a single response.
 *
 * Builds lightweight detached copies to avoid serializing heavy blob data
 * and the full EAGER-loaded city object graph.
 */
public class GetCatalogHandler implements RequestHandler {

    private final CityRepository cityRepository = CityRepository.getInstance();
    private final MapRepository mapRepository = MapRepository.getInstance();
    private final TourRepository tourRepository = TourRepository.getInstance();
    private final SiteRepository siteRepository = SiteRepository.getInstance();

    @Override
    public Message handle(Message request) {
        try {
            // Parse filter from request
            CatalogFilter filter = parseFilter(request.getMessage());

            // Build response
            CatalogResponse response = new CatalogResponse();

            // Route to appropriate search mode
            if (filter.isDetailedSearchMode()) {
                return handleDetailedSearch(filter, response);
            } else if (filter.isSearchMode()) {
                return handleSearchRequest(filter, response);
            }

            // Regular catalog mode
            // Always include all city names for the dropdown
            List<String> allCityNames = cityRepository.findCatalogCityNames();
            response.setAvailableCities(new ArrayList<>(allCityNames));

            // Get filtered maps
            List<GCMMap> maps = mapRepository.findByCriteria(
                filter.getCityName(),
                filter.getMapName(),
                filter.getVersion()
            );

            // Build lightweight copies — no blobs, no deep city graph
            List<GCMMap> lightMaps = new ArrayList<>();
            for (GCMMap map : maps) {
                lightMaps.add(toLightweight(map));
            }
            response.setMaps(lightMaps);

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

            // Build lightweight city copy (no deep collections)
            City lightCity = toLightweightCity(city);

            CatalogResponse.CitySearchResult result = new CatalogResponse.CitySearchResult();
            result.setCity(lightCity);
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
     * Handle detailed search - returns actual content items organized by type.
     */
    private Message handleDetailedSearch(CatalogFilter filter, CatalogResponse response) {
        String query = filter.getSearchQuery();

        CatalogResponse.DetailedSearchResult result = new CatalogResponse.DetailedSearchResult();

        // Search maps
        List<GCMMap> maps = mapRepository.searchMaps(query);
        for (GCMMap map : maps) {
            result.getMaps().add(toMapSearchItem(map));
        }
        result.setTotalMaps(maps.size());

        // Search tours
        List<Tour> tours = tourRepository.searchTours(query);
        for (Tour tour : tours) {
            result.getTours().add(toTourSearchItem(tour));
        }
        result.setTotalTours(tours.size());

        // Search sites
        List<Site> sites = siteRepository.searchSites(query);
        for (Site site : sites) {
            result.getSites().add(toSiteSearchItem(site));
        }
        result.setTotalSites(sites.size());

        // Search cities (reuse existing query)
        List<Object[]> cityResults = cityRepository.searchWithCounts(query);
        for (Object[] row : cityResults) {
            result.getCities().add(toCitySearchItem(row));
        }
        result.setTotalCities(cityResults.size());

        response.setDetailedSearchResult(result);
        return new Message(ActionType.GET_CATALOG_RESPONSE, response);
    }

    /**
     * Convert GCMMap to lightweight MapSearchItem (no blobs).
     */
    private CatalogResponse.MapSearchItem toMapSearchItem(GCMMap map) {
        CatalogResponse.MapSearchItem item = new CatalogResponse.MapSearchItem();
        item.setId(map.getId());
        item.setName(map.getName());
        item.setDescription(map.getDescription());
        item.setVersion(map.getVersion());
        item.setPrice(map.getPrice());
        item.setCityName(map.getCity() != null ? map.getCity().getName() : null);
        item.setImagePath(map.getImagePath());
        return item;
    }

    /**
     * Convert Tour to lightweight TourSearchItem.
     */
    private CatalogResponse.TourSearchItem toTourSearchItem(Tour tour) {
        CatalogResponse.TourSearchItem item = new CatalogResponse.TourSearchItem();
        item.setId(tour.getId());
        item.setName(tour.getName());
        item.setDescription(tour.getDescription());
        item.setDuration(tour.getRecommendedDuration());
        item.setCityName(tour.getCity() != null ? tour.getCity().getName() : null);
        item.setSiteCount(tour.getSiteCount());
        return item;
    }

    /**
     * Convert Site to lightweight SiteSearchItem.
     */
    private CatalogResponse.SiteSearchItem toSiteSearchItem(Site site) {
        CatalogResponse.SiteSearchItem item = new CatalogResponse.SiteSearchItem();
        item.setId(site.getId());
        item.setName(site.getName());
        item.setDescription(site.getDescription());
        item.setCategory(site.getCategory() != null ? site.getCategory().toString() : null);
        item.setCityName(site.getCity() != null ? site.getCity().getName() : null);
        item.setLocation(site.getLocation());
        return item;
    }

    /**
     * Convert search result row to lightweight CitySearchItem.
     */
    private CatalogResponse.CitySearchItem toCitySearchItem(Object[] row) {
        City city = (City) row[0];
        CatalogResponse.CitySearchItem item = new CatalogResponse.CitySearchItem();
        item.setId(city.getId());
        item.setName(city.getName());
        item.setDescription(city.getDescription());
        item.setPriceSub(city.getPriceSub());
        item.setImagePath(city.getImagePath());
        item.setMapCount(((Long) row[1]).intValue());
        item.setSiteCount(((Long) row[2]).intValue());
        item.setTourCount(((Long) row[3]).intValue());
        return item;
    }

    /**
     * Create a lightweight GCMMap copy for catalog display.
     * Excludes mapImage blob, siteMarkersJson, and deep Hibernate proxy collections on sites.
     */
    private GCMMap toLightweight(GCMMap map) {
        GCMMap light = new GCMMap();
        light.setId(map.getId());
        light.setName(map.getName());
        light.setDescription(map.getDescription());
        light.setVersion(map.getVersion());
        light.setPrice(map.getPrice());
        light.setStatus(map.getStatus());
        light.setImagePath(map.getImagePath());
        // Intentionally skip: mapImage, siteMarkersJson

        if (map.getCity() != null) {
            light.setCity(toLightweightCity(map.getCity()));
        }

        // Include lightweight site copies for client-side search
        if (map.getSites() != null) {
            List<Site> lightSites = new ArrayList<>();
            for (Site site : map.getSites()) {
                lightSites.add(toLightweightSite(site));
            }
            light.setSites(lightSites);
        }

        return light;
    }

    /**
     * Create a lightweight Site copy (no Hibernate proxy collections).
     */
    private Site toLightweightSite(Site site) {
        Site light = new Site();
        light.setId(site.getId());
        light.setName(site.getName());
        light.setDescription(site.getDescription());
        light.setLocation(site.getLocation());
        light.setCategory(site.getCategory());
        light.setAccessible(site.isAccessible());
        light.setRecommendedVisitDuration(site.getRecommendedVisitDuration());
        // Intentionally skip: city, maps, tours (Hibernate proxy collections)
        return light;
    }

    /**
     * Create a lightweight City copy (name, price, imagePath only — no collections).
     */
    private City toLightweightCity(City city) {
        City light = new City();
        light.setId(city.getId());
        light.setName(city.getName());
        light.setDescription(city.getDescription());
        light.setPriceSub(city.getPriceSub());
        light.setImagePath(city.getImagePath());
        // Intentionally skip: maps, sites, tours
        return light;
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