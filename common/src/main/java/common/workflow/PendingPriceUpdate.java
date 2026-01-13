package common.workflow;

import common.enums.RequestStatus;
import common.content.GCMMap;
import common.user.User;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_price_updates")
public class PendingPriceUpdate implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // Relationship to the Map being updated
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "map_id", nullable = false)
    private GCMMap map;

    // Relationship to the User who requested the change
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id")
    private User requester;

    @Column(name = "old_price")
    private double oldPrice;

    @Column(name = "new_price")
    private double newPrice;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.OPEN;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ==================== CONSTRUCTORS ====================

    public PendingPriceUpdate() {
        this.createdAt = LocalDateTime.now();
        this.status = RequestStatus.OPEN;
    }

    public PendingPriceUpdate(GCMMap map, User requester, double oldPrice, double newPrice) {
        this();
        this.map = map;
        this.requester = requester;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
    }

    // ==================== GETTERS & SETTERS ====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public GCMMap getMap() { return map; }
    public void setMap(GCMMap map) { this.map = map; }

    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }

    public double getOldPrice() { return oldPrice; }
    public void setOldPrice(double oldPrice) { this.oldPrice = oldPrice; }

    public double getNewPrice() { return newPrice; }
    public void setNewPrice(double newPrice) { this.newPrice = newPrice; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ==================== CONVENIENCE METHODS ====================

    public int getMapId() { 
        return map != null ? map.getId() : 0; 
    }

    public String getCity() { 
        return map != null ? map.getCityName() : null; 
    }

    public String getMapName() { 
        return map != null ? map.getName() : null; 
    }

    public String getVersion() { 
        return map != null ? map.getVersion() : null; 
    }

    // For TableView columns (backward compatibility)
    public String getType() { return "PRICE"; }

    public String getTarget() {
        return getCity() + " - " + getMapName() + " (" + getVersion() + ")";
    }
    
    public String getInfo() { 
        return oldPrice + " -> " + newPrice; 
    }
}
