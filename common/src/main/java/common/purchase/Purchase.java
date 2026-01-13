package common.purchase;

import common.content.City;
import common.user.User;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "purchases")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "purchase_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Purchase implements Serializable {
    
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected int id;

    @Column(name = "purchase_date")
    protected LocalDate purchaseDate;

    @Column(name = "price")
    protected double pricePaid;

    // MANY Purchases belong to ONE User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    protected User user;

    // MANY Purchases can be for ONE City
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    protected City city;

    // ==================== CONSTRUCTORS ====================
    
    public Purchase() {}

    public Purchase(int id, LocalDate purchaseDate, double pricePaid) {
        this.id = id;
        this.purchaseDate = purchaseDate;
        this.pricePaid = pricePaid;
    }

    // ==================== ABSTRACT METHOD ====================
    
    public abstract boolean isValid();

    // ==================== GETTERS & SETTERS ====================
    
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public double getPricePaid() { return pricePaid; }
    public void setPricePaid(double pricePaid) { this.pricePaid = pricePaid; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public City getCity() { return city; }
    public void setCity(City city) { this.city = city; }
}
