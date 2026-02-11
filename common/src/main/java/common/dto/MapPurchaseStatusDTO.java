package common.dto;

import java.io.Serializable;

public class MapPurchaseStatusDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean purchased;
    private boolean olderVersionPurchased;
    private String purchasedVersion;
    private double upgradePrice;
    private double fullPrice;

    public MapPurchaseStatusDTO() {}

    public MapPurchaseStatusDTO(boolean purchased, boolean olderVersionPurchased, String purchasedVersion, double upgradePrice, double fullPrice) {
        this.purchased = purchased;
        this.olderVersionPurchased = olderVersionPurchased;
        this.purchasedVersion = purchasedVersion;
        this.upgradePrice = upgradePrice;
        this.fullPrice = fullPrice;
    }

    public boolean isPurchased() { return purchased; }
    public void setPurchased(boolean purchased) { this.purchased = purchased; }

    public boolean isOlderVersionPurchased() { return olderVersionPurchased; }
    public void setOlderVersionPurchased(boolean olderVersionPurchased) { this.olderVersionPurchased = olderVersionPurchased; }

    public String getPurchasedVersion() { return purchasedVersion; }
    public void setPurchasedVersion(String purchasedVersion) { this.purchasedVersion = purchasedVersion; }

    public double getUpgradePrice() { return upgradePrice; }
    public void setUpgradePrice(double upgradePrice) { this.upgradePrice = upgradePrice; }

    public double getFullPrice() { return fullPrice; }
    public void setFullPrice(double fullPrice) { this.fullPrice = fullPrice; }
}
