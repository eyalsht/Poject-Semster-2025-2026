package common;

import java.time.LocalDate;

public class Client extends User {
    
    private static final long serialVersionUID = 1L;
    
    private String phoneNumber;
    private String creditCardInfo;
    private String creditCardToken;
    private LocalDate subscriptionExpiry;

    public Client(int id,
                  String firstName,
                  String lastName,
                  String username,
                  String email,
                  String password,
                  String phoneNumber,
                  String creditCardInfo,
                  String creditCardToken,
                  LocalDate subscriptionExpiry) {

        // Use super to call the User constructor
        super(id, firstName, lastName, username, email, password);
        this.phoneNumber = phoneNumber;
        this.creditCardInfo = creditCardInfo;
        this.creditCardToken = creditCardToken;
        this.subscriptionExpiry = subscriptionExpiry;
    }

    public Client(int id, String username, String password, String email) {
        super(id, username, password, email);
        this.phoneNumber = "";
        this.creditCardInfo = "";
        this.creditCardToken = "";
        this.subscriptionExpiry = null;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCreditCardInfo() {
        return creditCardInfo;
    }
    
    public void setCreditCardInfo(String creditCardInfo) {
        this.creditCardInfo = creditCardInfo;
    }
    
    public String getCreditCardToken() {
        return creditCardToken;
    }
    
    public void setCreditCardToken(String creditCardToken) {
        this.creditCardToken = creditCardToken;
    }
    
    public LocalDate getSubscriptionExpiry() {
        return subscriptionExpiry;
    }
    
    public void setSubscriptionExpiry(LocalDate subscriptionExpiry) {
        this.subscriptionExpiry = subscriptionExpiry;
    }
    
    public void register() {
        // registration logic
    }

    public void updateProfile() {
        // profile update logic
    }

    public void purchaseSubscription() {
        // subscription purchase logic
    }
    
    @Override
    public String toString() {
        return "Client{" +
                "id=" + getId() +
                ", firstName='" + getFirstName() + '\'' +
                ", lastName='" + getLastName() + '\'' +
                ", username='" + getUsername() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", creditCardInfo='" + creditCardInfo + '\'' +
                ", creditCardToken='" + creditCardToken + '\'' +
                ", subscriptionExpiry=" + subscriptionExpiry +
                '}';
    }
}