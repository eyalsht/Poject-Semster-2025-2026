package entities;

public class Client extends User {

    private String phoneNumber;
    private String creditCardInfo;

    public Client(int id,
                  String firstName,
                  String lastName,
                  String username,
                  String email,
                  String password,
                  String phoneNumber,
                  String creditCardInfo) {

        // Use super to call the User constructor
        super(id, firstName, lastName, username, email, password);
        this.phoneNumber = phoneNumber;
        this.creditCardInfo = creditCardInfo;
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
    public void register() {
        // registration logic
    }

    public void updateProfile() {
        // profile update logic
    }

    public void purchaseSubscription() {
        // subscription purchase logic
    }
}