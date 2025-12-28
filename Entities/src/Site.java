package entities;

public class Site extends ContentItem {

    private SiteCategory category;
    private boolean isAccessible;
    private double recommendedVisitDuration;
    private String location;

    public Site(int id,
                String name,
                String description,
                SiteCategory category,
                boolean isAccessible,
                double recommendedVisitDuration,
                String location) {

        super(id, name, description); // <-- ContentItem fields
        this.category = category;
        this.isAccessible = isAccessible;
        this.recommendedVisitDuration = recommendedVisitDuration;
        this.location = location;
    }

    @Override
    public String getDetails() {
        return "Site{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", category=" + category +
                ", isAccessible=" + isAccessible +
                ", recommendedVisitDuration=" + recommendedVisitDuration +
                ", location='" + location + '\'' +
                '}';
    }


    public SiteCategory getCategory()
    {
        return category;
    }
    public void setCategory(SiteCategory category) { this.category = category; }

    public boolean isAccessible() { return isAccessible; }
    public void setAccessible(boolean accessible) { isAccessible = accessible; }

    public double getRecommendedVisitDuration() { return recommendedVisitDuration; }
    public void setRecommendedVisitDuration(double recommendedVisitDuration) {
        this.recommendedVisitDuration = recommendedVisitDuration;
    }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
