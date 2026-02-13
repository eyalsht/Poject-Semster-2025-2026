package common.content;

import java.io.Serializable;

/**
 * Represents a numbered marker position on a map image.
 * Coordinates are relative (0.0–1.0) so they scale with any image size.
 */
public class SiteMarker implements Serializable {

    private static final long serialVersionUID = 1L;

    private int siteId;
    private double x; // 0.0–1.0 relative to image width
    private double y; // 0.0–1.0 relative to image height

    public SiteMarker() {}

    public SiteMarker(int siteId, double x, double y) {
        this.siteId = siteId;
        this.x = x;
        this.y = y;
    }

    public int getSiteId() { return siteId; }
    public void setSiteId(int siteId) { this.siteId = siteId; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
}
