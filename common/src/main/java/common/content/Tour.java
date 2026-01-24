package common.content;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tours")
public class Tour extends ContentItem implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Column(name = "recommended_duration")
    private String recommendedDuration;  // e.g., "3 hours", "Half day"

    // MANY Tours belong to ONE City
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    // MANY Tours have MANY Sites (owning side)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "tour_sites",
        joinColumns = @JoinColumn(name = "tour_id"),
        inverseJoinColumns = @JoinColumn(name = "site_id")
    )
    private List<Site> sites = new ArrayList<>();

    // ==================== CONSTRUCTORS ====================
    
    public Tour() {
        super();
        this.sites = new ArrayList<>();
    }

    public Tour(int id, String name, String description, String recommendedDuration) {
        super(id, name, description);
        this.recommendedDuration = recommendedDuration;
        this.sites = new ArrayList<>();
    }

    // ==================== GETTERS & SETTERS ====================
    
    public String getRecommendedDuration() { return recommendedDuration; }
    public void setRecommendedDuration(String recommendedDuration) { this.recommendedDuration = recommendedDuration; }

    // ==================== RELATIONSHIP METHODS ====================
    
    public City getCity() { return city; }

    public void setCity(City city) { this.city = city; }

    public String getCityName() {
        return city != null ? city.getName() : null;
    }

    public List<Site> getSites() { return sites; }
    public void setSites(List<Site> sites) { this.sites = sites; }

    public boolean doesSiteExist(Site site) {return this.sites != null && this.sites.contains(site);}

    public void addSite(Site site) {
        if (this.sites == null) this.sites = new ArrayList<>();
        if (!this.sites.contains(site)) {
            this.sites.add(site);
        }
    }

    public void removeSite(Site site) {
        if (this.sites != null) {
            this.sites.remove(site);
        }
    }

    public int getSiteCount() {
        return sites != null ? sites.size() : 0;
    }

    /**
     * Check if this tour can be fully displayed on a given map
     * (all tour sites must be present on the map)
     */
    public boolean isAvailableOnMap(GCMMap map) {
        if (sites == null || sites.isEmpty()) return false;
        List<Site> mapSites = map.getSites();
        return mapSites != null && mapSites.containsAll(this.sites);
    }

    @Override
    public String getDetails() {
        return "Tour: " + name + " (" + recommendedDuration + ") - " + getSiteCount() + " sites";
    }

    @Override
    public String toString() {
        return getName();
    }
}