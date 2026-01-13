package common.content;

import common.enums.MapStatus;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "maps")
public class GCMMap extends ContentItem implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Lob
    @Column(name = "image", columnDefinition = "LONGBLOB")
    private byte[] image;

    @Column(name = "version")
    private String version;  // Changed from double to String - versions like "1.0", "2.1" are better as strings

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MapStatus status;

    @Column(name = "price")
    private double price;

    // MANY Maps belong to ONE City
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    // MANY Maps can DISPLAY MANY Sites (Many-to-Many)
    // This is NOT ownership - it's "which sites appear on this map"
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "map_sites",
        joinColumns = @JoinColumn(name = "map_id"),
        inverseJoinColumns = @JoinColumn(name = "site_id")
    )
    private List<Site> sites = new ArrayList<>();

    // ==================== CONSTRUCTORS ====================
    
    public GCMMap() {
        super();
        this.sites = new ArrayList<>();
    }

    public GCMMap(int id, String name, String description, String version, byte[] image, MapStatus status) {
        super(id, name, description);
        this.version = version;
        this.image = image;
        this.status = status;
        this.sites = new ArrayList<>();
    }

    public GCMMap(int id, String name, String description, String version, double price, MapStatus status) {
        super(id, name, description);
        this.version = version;
        this.price = price;
        this.status = status;
        this.sites = new ArrayList<>();
    }

    // ==================== GETTERS & SETTERS ====================
    
    public byte[] getImage() { return image; }
    public void setImage(byte[] image) { this.image = image; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public MapStatus getStatus() { return status; }
    public void setStatus(MapStatus status) { this.status = status; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    // ==================== CITY RELATIONSHIP ====================
    
    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }

    public String getCityName() {
        return city != null ? city.getName() : null;
    }

    // ==================== SITES RELATIONSHIP ====================
    
    public List<Site> getSites() { return sites; }
    public void setSites(List<Site> sites) { this.sites = sites; }

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

    // ==================== DERIVED TOUR ACCESS ====================
    
    /**
     * Get all tours that can be fully completed using sites on this map.
     * A tour is "available" on this map if ALL its sites are displayed here.
     * 
     * This is a COMPUTED relationship - no tours field is stored on GCMMap!
     */
    public List<Tour> getAvailableTours() {
        if (city == null || sites == null || sites.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Tour> cityTours = city.getTours();
        if (cityTours == null) {
            return new ArrayList<>();
        }

        return cityTours.stream()
                .filter(tour -> tour.getSites() != null && 
                               !tour.getSites().isEmpty() &&
                               this.sites.containsAll(tour.getSites()))
                .collect(Collectors.toList());
    }

    @Override
    public String getDetails() {
        return "Map Name: " + name + ", Version: " + version + ", Status: " + status;
    }

    @Override
    public String toString() {
        return name + " v" + version;
    }
}
