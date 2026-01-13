package common.purchase;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@DiscriminatorValue("SUBSCRIPTION")
public class Subscription extends Purchase implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    // ==================== CONSTRUCTORS ====================
    
    public Subscription() {
        super();
    }

    public Subscription(int id, LocalDate purchaseDate, double pricePaid, LocalDate expirationDate) {
        super(id, purchaseDate, pricePaid);
        this.expirationDate = expirationDate;
    }

    // ==================== METHODS ====================
    
    public void renew(int months) {
        if (this.expirationDate == null || this.expirationDate.isBefore(LocalDate.now())) {
            this.expirationDate = LocalDate.now().plusMonths(months);
        } else {
            this.expirationDate = this.expirationDate.plusMonths(months);
        }
    }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    @Override
    public boolean isValid() {
        return expirationDate != null && LocalDate.now().isBefore(expirationDate);
    }
}