package common.dto;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO for submitting a batch price change request for a city.
 * Includes the new subscription price and new prices for each map.
 * These are NOT applied directly — they create PendingPriceUpdate records.
 */
public class PriceChangeRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int cityId;
    private String cityName;
    private double newSubscriptionPrice;
    private double oldSubscriptionPrice;

    /** Map of mapId -> newPrice */
    private Map<Integer, Double> mapPriceChanges = new HashMap<>();

    /** Map of mapId -> oldPrice (for audit trail) */
    private Map<Integer, Double> mapOldPrices = new HashMap<>();

    private Integer requesterId;

    public PriceChangeRequestDTO() {}

    public PriceChangeRequestDTO(int cityId, String cityName, Integer requesterId) {
        this.cityId = cityId;
        this.cityName = cityName;
        this.requesterId = requesterId;
    }

    // ==================== GETTERS & SETTERS ====================

    public int getCityId() { return cityId; }
    public void setCityId(int cityId) { this.cityId = cityId; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public double getNewSubscriptionPrice() { return newSubscriptionPrice; }
    public void setNewSubscriptionPrice(double newSubscriptionPrice) { this.newSubscriptionPrice = newSubscriptionPrice; }

    public double getOldSubscriptionPrice() { return oldSubscriptionPrice; }
    public void setOldSubscriptionPrice(double oldSubscriptionPrice) { this.oldSubscriptionPrice = oldSubscriptionPrice; }

    public Map<Integer, Double> getMapPriceChanges() { return mapPriceChanges; }
    public void setMapPriceChanges(Map<Integer, Double> mapPriceChanges) { this.mapPriceChanges = mapPriceChanges; }

    public Map<Integer, Double> getMapOldPrices() { return mapOldPrices; }
    public void setMapOldPrices(Map<Integer, Double> mapOldPrices) { this.mapOldPrices = mapOldPrices; }

    public Integer getRequesterId() { return requesterId; }
    public void setRequesterId(Integer requesterId) { this.requesterId = requesterId; }

    /**
     * Add a map price change entry.
     */
    public void addMapPriceChange(int mapId, double oldPrice, double newPrice) {
        this.mapPriceChanges.put(mapId, newPrice);
        this.mapOldPrices.put(mapId, oldPrice);
    }
}
