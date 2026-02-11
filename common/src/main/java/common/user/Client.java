package common.user;

import common.purchase.PurchasedMapSnapshot;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import common.purchase.PaymentDetails;

@Entity
@Table(name = "clients") // Creates a separate table for clients (besides the user's table)
@DiscriminatorValue("Client")  // Value stored in the user_type column
@PrimaryKeyJoinColumn(name = "id") // The link between the client and the user table
public class Client extends User {

    private static final long serialVersionUID = 1L;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Embedded
    private PaymentDetails paymentDetails;

    // List of permanently purchased maps (snapshots at time of purchase)
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PurchasedMapSnapshot> purchasedMaps = new ArrayList<>();

    public Client() {
        super();
    }

    public Client(int id, String firstName, String lastName, String username, String email, String password,
                  String phoneNumber, PaymentDetails paymentDetails) {
        super(id, username, password, firstName, lastName, email);
        this.phoneNumber = phoneNumber;
        this.paymentDetails = paymentDetails;
    }

    public Client(int id, String username, String password, String email, PaymentDetails paymentDetails, String phoneNumber) {
        super(id, username, password, email);
        this.paymentDetails = paymentDetails;
        this.phoneNumber = phoneNumber;
    }

    // ==================== GETTERS & SETTERS ====================

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public PaymentDetails getPaymentDetails() { return paymentDetails; }
    public void setPaymentDetails(PaymentDetails paymentDetails) { this.paymentDetails = paymentDetails; }

    public void register() {
        // registration logic
    }

    public void updateProfile() {
        // profile update logic
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

    public boolean hasPurchasedMapVersion(int originalMapId, String version) {
        if (purchasedMaps == null || version == null) return false;
        return purchasedMaps.stream()
            .anyMatch(snapshot -> snapshot.getOriginalMapId() == originalMapId
                    && version.equals(snapshot.getPurchasedVersion()));
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