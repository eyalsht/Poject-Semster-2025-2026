package common.dto;

import common.content.City;
import common.content.GCMMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for catalog requests.
 * Contains all data needed to populate the catalog page in a single response.
 */
public class CatalogResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;

    // The filtered maps based on search criteria
    private List<GCMMap> maps = new ArrayList<>();
    
    // Filter options for dropdowns
    private List<String> availableCities = new ArrayList<>();
    private List<String> availableMapNames = new ArrayList<>();
    private List<String> availableVersions = new ArrayList<>();
    
    // Optional: full city data if needed
    private List<City> cities = new ArrayList<>();

    // Search results for search mode
    private List<CitySearchResult> searchResults = new ArrayList<>();

    public CatalogResponse() {}

    /**
     * Inner class to hold search result data with counts.
     */
    public static class CitySearchResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private City city;
        private int mapCount;
        private int siteCount;
        private int tourCount;
        private List<String> mapDescriptions;

        public CitySearchResult() {}

        public City getCity() { return city; }
        public void setCity(City city) { this.city = city; }

        public int getMapCount() { return mapCount; }
        public void setMapCount(int mapCount) { this.mapCount = mapCount; }

        public int getSiteCount() { return siteCount; }
        public void setSiteCount(int siteCount) { this.siteCount = siteCount; }

        public int getTourCount() { return tourCount; }
        public void setTourCount(int tourCount) { this.tourCount = tourCount; }

        public List<String> getMapDescriptions() { return mapDescriptions; }
        public void setMapDescriptions(List<String> mapDescriptions) { this.mapDescriptions = mapDescriptions; }
    }

    // ==================== GETTERS & SETTERS ====================
    
    public List<GCMMap> getMaps() { return maps; }
    public void setMaps(List<GCMMap> maps) { this.maps = maps; }

    public List<String> getAvailableCities() { return availableCities; }
    public void setAvailableCities(List<String> availableCities) { this.availableCities = availableCities; }

    public List<String> getAvailableMapNames() { return availableMapNames; }
    public void setAvailableMapNames(List<String> availableMapNames) { this.availableMapNames = availableMapNames; }

    public List<String> getAvailableVersions() { return availableVersions; }
    public void setAvailableVersions(List<String> availableVersions) { this.availableVersions = availableVersions; }

    public List<City> getCities() { return cities; }
    public void setCities(List<City> cities) { this.cities = cities; }

    public List<CitySearchResult> getSearchResults() { return searchResults; }
    public void setSearchResults(List<CitySearchResult> searchResults) { this.searchResults = searchResults; }

    // Convenience method
    public int getMapCount() {
        return maps != null ? maps.size() : 0;
    }

    /**
     * Check if this response contains search results.
     */
    public boolean isSearchMode() {
        return searchResults != null && !searchResults.isEmpty();
    }
}
