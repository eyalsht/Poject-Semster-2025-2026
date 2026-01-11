package common;

import java.io.Serializable;

public class GCMMap extends ContentItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private byte[] image;
    private double version;
    private String status;

    public GCMMap() {
        super();
    }

    public GCMMap(int id, String name, String description, double version, byte[] image, String status) {
        super(id, name, description);
        this.version = version;
        this.image = image;
        this.status = status;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public double getVersion() {
        return version;
    }

    public void setVersion(double version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String getDetails() {
        return "Map Name: " + name + ", Version: " + version + ", Status: " + status;
    }
}
