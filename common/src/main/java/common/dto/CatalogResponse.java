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

    // Detailed search results organized by content type
    private DetailedSearchResult detailedSearchResult;

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

    /**
     * Detailed search results organized by content type.
     * Used when CatalogFilter.detailedSearch = true.
     */
    public static class DetailedSearchResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<MapSearchItem> maps = new ArrayList<>();
        private List<TourSearchItem> tours = new ArrayList<>();
        private List<SiteSearchItem> sites = new ArrayList<>();
        private List<CitySearchItem> cities = new ArrayList<>();

        private int totalMaps;
        private int totalTours;
        private int totalSites;
        private int totalCities;

        public DetailedSearchResult() {}

        public List<MapSearchItem> getMaps() { return maps; }
        public void setMaps(List<MapSearchItem> maps) { this.maps = maps; }

        public List<TourSearchItem> getTours() { return tours; }
        public void setTours(List<TourSearchItem> tours) { this.tours = tours; }

        public List<SiteSearchItem> getSites() { return sites; }
        public void setSites(List<SiteSearchItem> sites) { this.sites = sites; }

        public List<CitySearchItem> getCities() { return cities; }
        public void setCities(List<CitySearchItem> cities) { this.cities = cities; }

        public int getTotalMaps() { return totalMaps; }
        public void setTotalMaps(int totalMaps) { this.totalMaps = totalMaps; }

        public int getTotalTours() { return totalTours; }
        public void setTotalTours(int totalTours) { this.totalTours = totalTours; }

        public int getTotalSites() { return totalSites; }
        public void setTotalSites(int totalSites) { this.totalSites = totalSites; }

        public int getTotalCities() { return totalCities; }
        public void setTotalCities(int totalCities) { this.totalCities = totalCities; }
    }

    /**
     * Lightweight map item for search results.
     */
    public static class MapSearchItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private int id;
        private String name;
        private String description;
        private String version;
        private double price;
        private String cityName;
        private String imagePath;

        public MapSearchItem() {}

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }

        public String getCityName() { return cityName; }
        public void setCityName(String cityName) { this.cityName = cityName; }

        public String getImagePath() { return imagePath; }
        public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    }

    /**
     * Lightweight tour item for search results.
     */
    public static class TourSearchItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private int id;
        private String name;
        private String description;
        private String duration;
        private String cityName;
        private int siteCount;

        public TourSearchItem() {}

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }

        public String getCityName() { return cityName; }
        public void setCityName(String cityName) { this.cityName = cityName; }

        public int getSiteCount() { return siteCount; }
        public void setSiteCount(int siteCount) { this.siteCount = siteCount; }
    }

    /**
     * Lightweight site item for search results.
     */
    public static class SiteSearchItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private int id;
        private String name;
        private String description;
        private String category;
        private String cityName;
        private String location;

        public SiteSearchItem() {}

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getCityName() { return cityName; }
        public void setCityName(String cityName) { this.cityName = cityName; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }

    /**
     * Lightweight city item for search results with counts.
     */
    public static class CitySearchItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private int id;
        private String name;
        private String description;
        private double priceSub;
        private String imagePath;
        private int mapCount;
        private int tourCount;
        private int siteCount;

        public CitySearchItem() {}

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public double getPriceSub() { return priceSub; }
        public void setPriceSub(double priceSub) { this.priceSub = priceSub; }

        public String getImagePath() { return imagePath; }
        public void setImagePath(String imagePath) { this.imagePath = imagePath; }

        public int getMapCount() { return mapCount; }
        public void setMapCount(int mapCount) { this.mapCount = mapCount; }

        public int getTourCount() { return tourCount; }
        public void setTourCount(int tourCount) { this.tourCount = tourCount; }

        public int getSiteCount() { return siteCount; }
        public void setSiteCount(int siteCount) { this.siteCount = siteCount; }
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

    public DetailedSearchResult getDetailedSearchResult() { return detailedSearchResult; }
    public void setDetailedSearchResult(DetailedSearchResult detailedSearchResult) { this.detailedSearchResult = detailedSearchResult; }

    // Convenience method
    public int getMapCount() {
        return maps != null ? maps.size() : 0;
    }

    /**
     * Check if this response contains city-count search results.
     */
    public boolean isSearchMode() {
        return searchResults != null && !searchResults.isEmpty();
    }

    /**
     * Check if this response contains detailed search results.
     */
    public boolean isDetailedSearchMode() {
        return detailedSearchResult != null;
    }
}
