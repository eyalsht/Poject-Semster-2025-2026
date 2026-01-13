package common.content;

import common.enums.SiteCategory;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sites")
public class Site extends ContentItem implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private SiteCategory category;

    @Column(name = "is_accessible")
    private boolean isAccessible;

    @Column(name = "recommended_visit_duration")
    private double recommendedVisitDuration;

    @Column(name = "location")
    private String location;

    // MANY Sites belong to ONE City
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    // Bidirectional Many-to-Many with GCMMap (a site can appear on multiple maps)
    @ManyToMany(mappedBy = "sites", fetch = FetchType.LAZY)
    private List<GCMMap> maps = new ArrayList<>();

    // Bidirectional Many-to-Many with Tour (a site can be part of multiple tours)
    @ManyToMany(mappedBy = "sites", fetch = FetchType.LAZY)
    private List<Tour> tours = new ArrayList<>();

    // ==================== CONSTRUCTORS ====================
    
    public Site() {
        super();
        this.maps = new ArrayList<>();
        this.tours = new ArrayList<>();
    }

    public Site(int id,
                String name,
                String description,
                SiteCategory category,
                boolean isAccessible,
                double recommendedVisitDuration,
                String location) {
        super(id, name, description);
        this.category = category;
        this.isAccessible = isAccessible;
        this.recommendedVisitDuration = recommendedVisitDuration;
        this.location = location;
        this.maps = new ArrayList<>();
        this.tours = new ArrayList<>();
    }

    // ==================== GETTERS & SETTERS ====================
    
    public SiteCategory getCategory() { return category; }
    public void setCategory(SiteCategory category) { this.category = category; }

    public boolean isAccessible() { return isAccessible; }
    public void setAccessible(boolean accessible) { isAccessible = accessible; }

    public double getRecommendedVisitDuration() { return recommendedVisitDuration; }
    public void setRecommendedVisitDuration(double duration) { this.recommendedVisitDuration = duration; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    // ==================== RELATIONSHIP METHODS ====================
    
    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }

    public String getCityName() {
        return city != null ? city.getName() : null;
    }

    public List<GCMMap> getMaps() { return maps; }
    public void setMaps(List<GCMMap> maps) { this.maps = maps; }

    public List<Tour> getTours() { return tours; }
    public void setTours(List<Tour> tours) { this.tours = tours; }

    @Override
    public String getDetails() {
        return "Site: " + name + " (" + category + ") - " + location;
    }

    @Override
    public String toString() {
        return name;
    }
}
