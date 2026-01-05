package common;

import java.io.Serializable;
import java.sql.Timestamp;

public class PendingPriceUpdate implements Serializable {
    private int id;
    private int mapId;
    private String city;
    private String mapName;
    private String version;
    private double oldPrice;
    private double newPrice;
    private Timestamp createdAt;

    public PendingPriceUpdate(int id, int mapId, String city, String mapName, String version,
                              double oldPrice, double newPrice, Timestamp createdAt) {
        this.id = id;
        this.mapId = mapId;
        this.city = city;
        this.mapName = mapName;
        this.version = version;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public int getMapId() { return mapId; }
    public String getCity() { return city; }
    public String getMapName() { return mapName; }
    public String getVersion() { return version; }
    public double getOldPrice() { return oldPrice; }
    public double getNewPrice() { return newPrice; }
    public Timestamp getCreatedAt() { return createdAt; }

    // For TableView columns
    public String getType() { return "PRICE"; }
    public String getTarget() { return city + " - " + mapName + " (" + version + ")"; }
    public String getInfo() { return oldPrice + " -> " + newPrice; }
}
