package common.purchase;

import common.content.GCMMap;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@DiscriminatorValue("ONE_TIME")
public class OneTimePurchase extends Purchase implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Column(name = "purchased_version")
    private String purchasedVersion;  // Changed to String to match GCMMap

    // Specific map for one-time purchase
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_id")
    private GCMMap map;

    // Reference to the snapshot created for this purchase
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    private PurchasedMapSnapshot snapshot;

    // ==================== CONSTRUCTORS ====================
    
    public OneTimePurchase() {
        super();
    }

    public OneTimePurchase(int id, LocalDate purchaseDate, double pricePaid, String purchasedVersion) {
        super(id, purchaseDate, pricePaid);
        this.purchasedVersion = purchasedVersion;
    }

    // ==================== GETTERS & SETTERS ====================
    
    public String getPurchasedVersion() { return purchasedVersion; }
    public void setPurchasedVersion(String purchasedVersion) { this.purchasedVersion = purchasedVersion; }

    public GCMMap getMap() { return map; }
    public void setMap(GCMMap map) { this.map = map; }

    public PurchasedMapSnapshot getSnapshot() { return snapshot; }
    public void setSnapshot(PurchasedMapSnapshot snapshot) { this.snapshot = snapshot; }

    // Convenience method to get city name from map
    public String getCityName() {
        return map != null ? map.getCityName() : null;
    }

    @Override
    public boolean isValid() {
        // One-time purchases are always valid once made
        return true;
    }
}