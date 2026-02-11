package common.content;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Entity
@Table(name = "cities")
public class City extends ContentItem implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Column(name = "price_sub")
    private double priceSub;

    @Transient  // Not persisted - only for pending workflow
    private double pendingPriceSub;

    @Column(name = "image_path")
    private String imagePath;

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    // ONE City has MANY Maps
    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<GCMMap> maps = new ArrayList<>();

    // ONE City has MANY Sites
    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Site> sites = new ArrayList<>();

    // ONE City has MANY Tours
    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Tour> tours = new ArrayList<>();

    // ==================== CONSTRUCTORS ====================
    
    public City() {
        super();
        this.maps = new ArrayList<>();
        this.sites = new ArrayList<>();
        this.tours = new ArrayList<>();
    }

    public City(int id, String name, double priceSub) {
        super(id, name, null);
        this.priceSub = priceSub;
        this.maps = new ArrayList<>();
        this.sites = new ArrayList<>();
        this.tours = new ArrayList<>();
    }

    public City(int id, String name, String description, double priceSub) {
        super(id, name, description);
        this.priceSub = priceSub;
        this.maps = new ArrayList<>();
        this.sites = new ArrayList<>();
        this.tours = new ArrayList<>();
    }

    public City(String name)
    {
        super(name);
    }

    // ==================== GETTERS & SETTERS ====================
    
    public double getPriceSub() { return priceSub; }
    public void setPriceSub(double priceSub) { this.priceSub = priceSub; }

    public double getPendingPriceSub() { return pendingPriceSub; }
    public void setPendingPriceSub(double pendingPriceSub) { this.pendingPriceSub = pendingPriceSub; }

    // ==================== MAP RELATIONSHIP ====================
    
    public List<GCMMap> getMaps() { return maps; }
    public void setMaps(List<GCMMap> maps) { if (maps == null) {
        this.maps = new ArrayList<>();
        return;
    }

        this.maps = maps.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(GCMMap::getId))),
                        ArrayList::new
                ));
    }

    public void addMap(GCMMap map) {
        if (this.maps == null) this.maps = new ArrayList<>();
        this.maps.add(map);
        map.setCity(this);
    }

    public void removeMap(GCMMap map) {
        if (this.maps != null) {
            this.maps.remove(map);
            map.setCity(null);
        }
    }

    public int getMapCount() {
        return maps != null ? maps.size() : 0;
    }

    // ==================== SITE RELATIONSHIP ====================
    
    public List<Site> getSites() { return sites; }
    public void setSites(List<Site> sites) { this.sites = sites; }

    public void addSite(Site site) {
        if (this.sites == null) this.sites = new ArrayList<>();
        this.sites.add(site);
        site.setCity(this);
    }

    public void removeSite(Site site) {
        if (this.sites != null) {
            this.sites.remove(site);
            site.setCity(null);
        }
    }

    public int getSiteCount() {
        return sites != null ? sites.size() : 0;
    }

    // ==================== TOUR RELATIONSHIP ====================
    
    public List<Tour> getTours() { return tours; }
    public void setTours(List<Tour> tours) { this.tours = tours; }

    public void addTour(Tour tour) {
        if (this.tours == null) this.tours = new ArrayList<>();
        this.tours.add(tour);
        tour.setCity(this);
    }

    public void removeTour(Tour tour) {
        if (this.tours != null) {
            this.tours.remove(tour);
            tour.setCity(null);
        }
    }

    public int getTourCount() {
        return tours != null ? tours.size() : 0;
    }

    @Override
    public String getDetails() {
        return "City: " + name;
    }

    @Override
    public String toString() {
        return name;
    }
}
