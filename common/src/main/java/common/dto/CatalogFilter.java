package common.dto;

import java.io.Serializable;

/**
 * Filter criteria for catalog requests.
 * Replaces the old ArrayList<String> approach with a proper typed object.
 */
public class CatalogFilter implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private String cityName;
    private String mapName;
    private String version;
    
    public CatalogFilter() {}
    
    public CatalogFilter(String cityName, String mapName, String version) {
        this.cityName = normalizeFilter(cityName);
        this.mapName = normalizeFilter(mapName);
        this.version = normalizeFilter(version);
    }
    
    /**
     * Convert empty/blank strings to null for cleaner query logic.
     */
    private String normalizeFilter(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    // ==================== GETTERS & SETTERS ====================
    
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = normalizeFilter(cityName); }

    public String getMapName() { return mapName; }
    public void setMapName(String mapName) { this.mapName = normalizeFilter(mapName); }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = normalizeFilter(version); }
    
    public boolean hasFilters() {
        return cityName != null || mapName != null || version != null;
    }
}
