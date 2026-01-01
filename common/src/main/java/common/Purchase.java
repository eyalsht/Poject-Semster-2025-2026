package common;

import java.time.LocalDate;

public abstract class Purchase {

    protected int id;
    protected LocalDate purchaseDate;
    protected double pricePaid;

    public Purchase(int id, LocalDate purchaseDate, double pricePaid) {
        this.id = id;
        this.purchaseDate = purchaseDate;
        this.pricePaid = pricePaid;
    }

    // UML: +isValid() : boolean
    public abstract boolean isValid();

    // getters (optional but standard)
    public int getId() { return id; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public double getPricePaid() { return pricePaid; }
}
