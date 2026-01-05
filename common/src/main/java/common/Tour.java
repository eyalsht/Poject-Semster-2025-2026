package common;

import java.util.ArrayList;
import java.io.Serializable;

/**
 * Entity class representing a Tour in the GCM system.
 * A Tour is a collection of interesting sites (PlaceOfInterest) with a recommended duration.
 * It extends the abstract ContentItem class.
 */
public class Tour extends ContentItem implements Serializable {

    // Unique attributes for Tour
    private String recommendedDuration; // e.g., "3 hours", "Half day"
    private ArrayList<Site> sites; // List of sites included in the tour

    /**
     * Default constructor. Initializes the sites list to avoid NullPointerException.
     */
    public Tour() {
        super(); // Calls the default constructor of ContentItem
        this.sites = new ArrayList<>();
    }

    /**
     * Full constructor.
     * @param id The unique ID of the tour.
     * @param name The name of the tour.
     * @param description A brief description of the tour.
     * @param recommendedDuration The suggested time to complete the tour.
     */
    public Tour(int id, String name, String description, String recommendedDuration) {
        super(id, name, description); // Initialize fields from the abstract parent class
        this.recommendedDuration = recommendedDuration;
        this.sites = new ArrayList<>();
    }

    // --- Getters and Setters ---

    public String getRecommendedDuration() {
        return recommendedDuration;
    }

    public void setRecommendedDuration(String recommendedDuration) {
        this.recommendedDuration = recommendedDuration;
    }

    public ArrayList<Site> getSites() {
        return sites;
    }

    public void setSites(ArrayList<Site> sites) {
        this.sites = sites;
    }

    // --- List Management Methods ---

    /**
     * Adds a site to the tour if it's not already present.
     * @param site The PlaceOfInterest to add.
     */
    public void addSite(Site site) {
        if (this.sites == null) {
            this.sites = new ArrayList<>();
        }
        if (!this.sites.contains(site)) {
            this.sites.add(site);
        }
    }

    /**
     * Removes a site from the tour.
     * @param site The PlaceOfInterest to remove.
     */
    public void removeSite(Site site) {
        if (this.sites != null) {
            this.sites.remove(site);
        }
    }

    /**
     * Returns the number of sites in this tour.
     * @return int count of sites.
     */
    public int getSiteCount() {
        return (sites == null) ? 0 : sites.size();
    }

    // --- Abstract Method Implementation ---

    /**
     * Returns a string summary of the tour details.
     * Overrides the abstract method from ContentItem.
     */
    @Override
    public String getDetails() {
        return "Tour: " + getName() + " | Duration: " + recommendedDuration + " | Sites: " + getSiteCount();
    }

    @Override
    public String toString() {
        return getName();
    }
}