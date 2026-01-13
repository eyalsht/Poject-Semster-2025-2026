package common.user;

import common.purchase.PurchasedMapSnapshot;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients") // Creates a separate table for clients (besides the user's table)
@DiscriminatorValue("Client")  // Value stored in the user_type column
@PrimaryKeyJoinColumn(name = "id") // The link between the client and the user table
public class Client extends User {

    private static final long serialVersionUID = 1L;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "credit_card")
    private String creditCardInfo;

    @Column(name = "credit_card_token")
    private String creditCardToken;

    @Column(name = "subscription_expiry")
    private LocalDate subscriptionExpiry;

    // List of permanently purchased maps (snapshots at time of purchase)
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PurchasedMapSnapshot> purchasedMaps = new ArrayList<>();

    // JPA no-arg constructor
    public Client() {
        super();
    }

    public Client(int id, String firstName, String lastName, String username, String email, String password,
                  String phoneNumber, String creditCardInfo, String creditCardToken, LocalDate subscriptionExpiry) {
        super(id, username, password, firstName, lastName, email);
        this.phoneNumber = phoneNumber;
        this.creditCardInfo = creditCardInfo;
        this.creditCardToken = creditCardToken;
        this.subscriptionExpiry = subscriptionExpiry;
    }

    public Client(int id, String username, String password, String email) {
        super(id, username, password, email);
    }

    // ==================== GETTERS & SETTERS ====================

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getCreditCardInfo() { return creditCardInfo; }
    public void setCreditCardInfo(String creditCardInfo) { this.creditCardInfo = creditCardInfo; }

    public String getCreditCardToken() { return creditCardToken; }
    public void setCreditCardToken(String creditCardToken) { this.creditCardToken = creditCardToken; }

    public LocalDate getSubscriptionExpiry() { return subscriptionExpiry; }
    public void setSubscriptionExpiry(LocalDate subscriptionExpiry) { this.subscriptionExpiry = subscriptionExpiry; }

    public void register() {
        // registration logic
    }

    public void updateProfile() {
        // profile update logic
    }

    public void purchaseSubscription() {
        // purchase logic
    }

    // ==================== PURCHASED MAPS RELATIONSHIP ====================

    public List<PurchasedMapSnapshot> getPurchasedMaps() { return purchasedMaps; }
    public void setPurchasedMaps(List<PurchasedMapSnapshot> purchasedMaps) { this.purchasedMaps = purchasedMaps; }

    public void addPurchasedMap(PurchasedMapSnapshot snapshot) {
        if (this.purchasedMaps == null) this.purchasedMaps = new ArrayList<>();
        this.purchasedMaps.add(snapshot);
        snapshot.setClient(this);
    }

    public boolean hasPurchasedMap(int originalMapId) {
        if (purchasedMaps == null) return false;
        return purchasedMaps.stream()
            .anyMatch(snapshot -> snapshot.getOriginalMapId() == originalMapId);
    }
    
    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}