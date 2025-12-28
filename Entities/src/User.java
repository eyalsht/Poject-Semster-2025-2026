package entities;

import java.io.Serializable;
import java.util.regex.Pattern;

public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected String username;
    protected String password;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected String phone;
    protected boolean isLoggedIn;

    public User(int id, String username, String password, String firstName, String lastName, String email, String phone) {
        this.id = id;
        this.username = username;
        setPassword(password); // Use setter to validate
        this.firstName = firstName;
        this.lastName = lastName;
        setEmail(email); // Use setter to validate
        this.phone = phone;
        this.isLoggedIn = false;
    }

    public int getId() {return id;}
    public String getUsername() {return username;}
    public String getPassword() {return password;}
    public String getFirstName() {return firstName;}
    public String getLastName() {return lastName;}
    public String getEmail() {return email;}
    public String getPhone() {return phone;}
    public boolean isLoggedIn() {return isLoggedIn;}

    public void setId(int id) {this.id = id;}
    public void setUsername(String username) {this.username = username;}
    public void setPassword(String password) {this.password = password;}
    public void setFirstName(String firstName) {this.firstName = firstName;}
    public void setLastName(String lastName) {this.lastName = lastName;}
    public void setEmail(String email) {this.email = email;}
    public void setPhone(String phone) {this.phone = phone;}
    public void setIsLoggedIn(boolean isLoggedIn) {this.isLoggedIn = isLoggedIn;}


}