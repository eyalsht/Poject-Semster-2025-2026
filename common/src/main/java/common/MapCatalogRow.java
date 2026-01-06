package common;

import java.io.Serializable;

public class MapCatalogRow implements Serializable {

    private final String city;
    private final String mapName;
    private final String version;
    private final double price;
    private final String description;

    public MapCatalogRow(String city, String mapName, String version, double price, String description) {
        this.city = city;
        this.mapName = mapName;
        this.version = version;
        this.price = price;
        this.description = description;
    }

    public String getCity() { return city; }
    public String getMapName() { return mapName; }
    public String getVersion() { return version; }
    public double getPrice() { return price; }
    public String getDescription() { return description; }
}