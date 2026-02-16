package common.purchase;

import common.content.GCMMap;
import common.content.Site;
import common.content.SiteMarker;
import common.enums.SiteCategory;
import common.enums.SiteDuration;
import common.user.Client;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a snapshot of a map at the time of purchase.
 * This ensures that even if the original map is updated,
 * the client retains access to the version they purchased.
 */
@Entity
@Table(name = "purchased_map_snapshots")
public class PurchasedMapSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    // The client who owns this purchased map
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    // Reference to the original map (may have been updated since purchase)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_map_id")
    private GCMMap originalMap;

    // Snapshot data - stored at time of purchase
    @Column(name = "map_name")
    private String mapName;

    @Column(name = "city_name")
    private String cityName;

    @Column(name = "purchased_version")
    private String purchasedVersion;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Lob
    @Column(name = "map_image_data", columnDefinition = "LONGBLOB")
    private byte[] mapImageData;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "price_paid")
    private double pricePaid;

    @Lob
    @Column(name = "sites_json", columnDefinition = "LONGTEXT")
    private String sitesJson;

    @Lob
    @Column(name = "site_markers_json", columnDefinition = "LONGTEXT")
    private String siteMarkersJson;

    @Column(name = "original_city_id")
    private int originalCityId;

    // ==================== CONSTRUCTORS ====================

    public PurchasedMapSnapshot() {}

    /**
     * Creates a snapshot from a GCMMap at the time of purchase.
     */
    public PurchasedMapSnapshot(Client client, GCMMap map, double pricePaid) {
        this.client = client;
        this.originalMap = map;
        this.mapName = map.getName();
        this.cityName = map.getCityName();
        this.purchasedVersion = map.getVersion();
        this.description = map.getDescription();
        this.mapImageData = map.getMapImage();
        this.purchaseDate = LocalDate.now();
        this.pricePaid = pricePaid;
        this.siteMarkersJson = map.getSiteMarkersJson();
        this.sitesJson = serializeSites(map.getSites());
        this.originalCityId = map.getCity() != null ? map.getCity().getId() : 0;
    }

    // ==================== SITE SERIALIZATION ====================

    /**
     * Serializes a list of Site objects to JSON string.
     * Uses manual JSON building (same style as the project's marker JSON).
     */
    public static String serializeSites(List<Site> sites) {
        if (sites == null || sites.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sites.size(); i++) {
            Site s = sites.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"id\":").append(s.getId()).append(",");
            sb.append("\"name\":\"").append(escapeJson(s.getName())).append("\",");
            sb.append("\"description\":\"").append(escapeJson(s.getDescription())).append("\",");
            sb.append("\"category\":\"").append(s.getCategory() != null ? s.getCategory().name() : "").append("\",");
            sb.append("\"location\":\"").append(escapeJson(s.getLocation())).append("\",");
            sb.append("\"accessible\":").append(s.isAccessible()).append(",");
            sb.append("\"duration\":\"").append(s.getRecommendedVisitDuration() != null ? s.getRecommendedVisitDuration().name() : "").append("\"");
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
    }

    /**
     * Parses sitesJson back into detached Site objects.
     * Uses regex parsing (same pattern as GCMMap.getSiteMarkers()).
     */
    public List<Site> getSnapshotSitesAsSiteObjects() {
        if (sitesJson == null || sitesJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Site> sites = new ArrayList<>();
        // Match each JSON object in the array
        Pattern p = Pattern.compile("\\{[^}]*\"id\"\\s*:\\s*(\\d+)[^}]*\"name\"\\s*:\\s*\"([^\"]*?)\"[^}]*\"description\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"[^}]*\"category\"\\s*:\\s*\"([^\"]*?)\"[^}]*\"location\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"[^}]*\"accessible\"\\s*:\\s*(true|false)[^}]*\"duration\"\\s*:\\s*\"([^\"]*?)\"[^}]*\\}");
        Matcher m = p.matcher(sitesJson);
        while (m.find()) {
            Site site = new Site();
            site.setId(Integer.parseInt(m.group(1)));
            site.setName(unescapeJson(m.group(2)));
            site.setDescription(unescapeJson(m.group(3)));
            String cat = m.group(4);
            if (cat != null && !cat.isEmpty()) {
                try { site.setCategory(SiteCategory.valueOf(cat)); } catch (Exception ignored) {}
            }
            site.setLocation(unescapeJson(m.group(5)));
            site.setAccessible(Boolean.parseBoolean(m.group(6)));
            String dur = m.group(7);
            if (dur != null && !dur.isEmpty()) {
                try { site.setRecommendedVisitDuration(SiteDuration.valueOf(dur)); } catch (Exception ignored) {}
            }
            sites.add(site);
        }
        return sites;
    }

    private static String unescapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\n", "\n")
                     .replace("\\r", "\r")
                     .replace("\\t", "\t")
                     .replace("\\\"", "\"")
                     .replace("\\\\", "\\");
    }

    /**
     * Parses siteMarkersJson into SiteMarker objects.
     * Uses the exact same regex pattern as GCMMap.getSiteMarkers().
     */
    public List<SiteMarker> getSnapshotMarkers() {
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

    // ==================== GETTERS & SETTERS ====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public GCMMap getOriginalMap() { return originalMap; }
    public void setOriginalMap(GCMMap originalMap) { this.originalMap = originalMap; }

    public String getMapName() { return mapName; }
    public void setMapName(String mapName) { this.mapName = mapName; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getPurchasedVersion() { return purchasedVersion; }
    public void setPurchasedVersion(String purchasedVersion) { this.purchasedVersion = purchasedVersion; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public byte[] getMapImageData() { return mapImageData; }
    public void setMapImageData(byte[] mapImageData) { this.mapImageData = mapImageData; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public double getPricePaid() { return pricePaid; }
    public void setPricePaid(double pricePaid) { this.pricePaid = pricePaid; }

    public String getSitesJson() { return sitesJson; }
    public void setSitesJson(String sitesJson) { this.sitesJson = sitesJson; }

    public String getSiteMarkersJson() { return siteMarkersJson; }
    public void setSiteMarkersJson(String siteMarkersJson) { this.siteMarkersJson = siteMarkersJson; }

    public int getOriginalCityId() { return originalCityId; }
    public void setOriginalCityId(int originalCityId) { this.originalCityId = originalCityId; }

    // ==================== CONVENIENCE METHODS ====================

    public int getOriginalMapId() {
        return originalMap != null ? originalMap.getId() : 0;
    }
}
