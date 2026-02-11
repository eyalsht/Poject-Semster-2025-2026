package common.dto;

import java.io.Serializable;
import java.time.LocalDate;

public class SubscriptionStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean active;
    private LocalDate expirationDate;
    private String cityName;
    private int cityId;
    private double pricePerMonth;

    public SubscriptionStatusDTO() {}

    public SubscriptionStatusDTO(boolean active, LocalDate expirationDate, String cityName, int cityId, double pricePerMonth) {
        this.active = active;
        this.expirationDate = expirationDate;
        this.cityName = cityName;
        this.cityId = cityId;
        this.pricePerMonth = pricePerMonth;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public int getCityId() { return cityId; }
    public void setCityId(int cityId) { this.cityId = cityId; }

    public double getPricePerMonth() { return pricePerMonth; }
    public void setPricePerMonth(double pricePerMonth) { this.pricePerMonth = pricePerMonth; }
}
