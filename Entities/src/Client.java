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

        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.creditCardInfo = creditCardInfo;
        this.isLoggedIn = false;
    }

    public Client(int id, String username, String password, String firstName, String lastName, String email) {
        super(id, username, password, firstName, lastName, email);
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