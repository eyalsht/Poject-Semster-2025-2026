package entities;

import java.time.LocalDate;

public class OneTimePurchase extends Purchase {

    private double purchasedVersion;

    public OneTimePurchase(int id, LocalDate purchaseDate, double pricePaid, double purchasedVersion) {
        super(id, purchaseDate, pricePaid);
        this.purchasedVersion = purchasedVersion;
    }

    public double getPurchasedVersion() {
        return purchasedVersion;
    }

    public void setPurchasedVersion(double purchasedVersion) {
        this.purchasedVersion = purchasedVersion;
    }

    @Override
    public boolean isValid() {
        // one-time purchases never expire
        return true;
    }
}