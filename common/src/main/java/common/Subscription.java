package common;

import java.time.LocalDate;

public class Subscription extends Purchase {

    private LocalDate expirationDate;

    public Subscription(int id, LocalDate purchaseDate, double pricePaid, LocalDate expirationDate) {
        super(id, purchaseDate, pricePaid);
        this.expirationDate = expirationDate;
    }

    // UML: +renew(months : int) : void
    public void renew(int months) {
        if (expirationDate.isBefore(LocalDate.now())) {
            expirationDate = LocalDate.now().plusMonths(months);
        } else {
            expirationDate = expirationDate.plusMonths(months);
        }
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    @Override
    public boolean isValid() {
        return expirationDate.isAfter(LocalDate.now());
    }
}