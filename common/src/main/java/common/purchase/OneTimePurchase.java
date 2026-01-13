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

    @Override
    public boolean isValid() {
        // One-time purchases are always valid once made
        return true;
    }
}