package common.purchase;

import common.content.GCMMap;
import common.user.Client;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Represents a snapshot of a map at the time of purchase.
 * This ensures that even if the original map is updated, 
 * the client retains access to the version they purchased.
 */
@Entity
@Table(name = "purchased_map_snapshots")
public class PurchasedMapSnapshot implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // The client who owns this purchased map
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    // Reference to the original map (may have been updated since purchase)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_map_id")
    private GCMMap originalMap;

    // Snapshot data - stored at time of purchase
    @Column(name = "map_name")
    private String mapName;

    @Column(name = "city_name")
    private String cityName;

    @Column(name = "purchased_version")
    private String purchasedVersion;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Lob
    @Column(name = "map_image_data")
    private byte[] mapImageData;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "price_paid")
    private double pricePaid;

    // ==================== CONSTRUCTORS ====================

    public PurchasedMapSnapshot() {}

    /**
     * Creates a snapshot from a GCMMap at the time of purchase.
     */
    public PurchasedMapSnapshot(Client client, GCMMap map, double pricePaid) {
        this.client = client;
        this.originalMap = map;
        this.mapName = map.getName();
        this.cityName = map.getCityName();
        this.purchasedVersion = map.getVersion();
        this.description = map.getDescription();
        this.mapImageData = map.getMapImage();
        this.purchaseDate = LocalDate.now();
        this.pricePaid = pricePaid;
    }

    // ==================== GETTERS & SETTERS ====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public GCMMap getOriginalMap() { return originalMap; }
    public void setOriginalMap(GCMMap originalMap) { this.originalMap = originalMap; }

    public String getMapName() { return mapName; }
    public void setMapName(String mapName) { this.mapName = mapName; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getPurchasedVersion() { return purchasedVersion; }
    public void setPurchasedVersion(String purchasedVersion) { this.purchasedVersion = purchasedVersion; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public byte[] getMapImageData() { return mapImageData; }
    public void setMapImageData(byte[] mapImageData) { this.mapImageData = mapImageData; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public double getPricePaid() { return pricePaid; }
    public void setPricePaid(double pricePaid) { this.pricePaid = pricePaid; }

    // ==================== CONVENIENCE METHODS ====================

    public int getOriginalMapId() {
        return originalMap != null ? originalMap.getId() : 0;
    }
}
