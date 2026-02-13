package common.content;

import common.enums.MapStatus;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Entity
@Table(name = "maps")
public class GCMMap extends ContentItem implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Column(name = "version")
    private String version;  // Changed from double to String - versions like "1.0", "2.1" are better as strings

    @Column(name = "price")
    private double price;

    @Column(name = "image_path")
    private String imagePath;

    @Lob
    @Column(name = "map_image", columnDefinition = "LONGBLOB")
    private byte[] mapImage;

    @Lob
    @Column(name = "site_markers_json", columnDefinition = "LONGTEXT")
    private String siteMarkersJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MapStatus status;

    // MANY Maps belong to ONE City
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "map_sites",
        joinColumns = @JoinColumn(name = "map_id"),
        inverseJoinColumns = @JoinColumn(name = "site_id")
    )
    @OrderColumn(name = "display_order")
    private List<Site> sites = new ArrayList<>();

    // ==================== CONSTRUCTORS ====================
    
    public GCMMap() {
        super();
        this.sites = new ArrayList<>();
    }

    public GCMMap(int id, String name, String description, String version,double price, String imagePath, MapStatus status) {
        super(id, name, description);
        this.version = version;
        this.price = price;
        this.imagePath = imagePath;
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

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public byte[] getMapImage() { return mapImage; }
    public void setMapImage(byte[] mapImage) { this.mapImage = mapImage; }

    public String getSiteMarkersJson() { return siteMarkersJson; }
    public void setSiteMarkersJson(String siteMarkersJson) { this.siteMarkersJson = siteMarkersJson; }

    /**
     * Parse siteMarkersJson into a list of SiteMarker objects.
     * Expected JSON format: [{"siteId":1,"x":0.35,"y":0.62}, ...]
     */
    public List<SiteMarker> getSiteMarkers() {
        if (siteMarkersJson == null || siteMarkersJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<SiteMarker> markers = new ArrayList<>();
        Pattern p = Pattern.compile("\\{[^}]*\"siteId\"\\s*:\\s*(\\d+)[^}]*\"x\"\\s*:\\s*([\\d.]+)[^}]*\"y\"\\s*:\\s*([\\d.]+)[^}]*\\}");
        Matcher m = p.matcher(siteMarkersJson);
        while (m.find()) {
            markers.add(new SiteMarker(
                Integer.parseInt(m.group(1)),
                Double.parseDouble(m.group(2)),
                Double.parseDouble(m.group(3))
            ));
        }
        return markers;
    }

    /**
     * Serialize a list of SiteMarker objects to JSON and store in siteMarkersJson.
     */
    public void setSiteMarkers(List<SiteMarker> markers) {
        if (markers == null || markers.isEmpty()) {
            this.siteMarkersJson = null;
            return;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < markers.size(); i++) {
            SiteMarker mk = markers.get(i);
            if (i > 0) sb.append(",");
            sb.append(String.format("{\"siteId\":%d,\"x\":%.4f,\"y\":%.4f}",
                    mk.getSiteId(), mk.getX(), mk.getY()));
        }
        sb.append("]");
        this.siteMarkersJson = sb.toString();
    }

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GCMMap gcmMap)) return false;
        // COMPARE BASE ON ID
        return getId() == gcmMap.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
    @Override
    public String toString() {
        return name + " v" + version;
    }
}
