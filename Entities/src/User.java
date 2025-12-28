package entities;

import java.io.Serializable;
import java.util.regex.Pattern;

public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Email validation pattern
    private static final String EMAIL_PATTERN = 
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);
    
    // Password requirements: at least 6 characters
    private static final int MIN_PASSWORD_LENGTH = 6;
    
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
    
    /**
     * Validates email format
     * @param email the email to validate
     * @return true if email is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return emailPattern.matcher(email).matches();
    }
    
    /**
     * Validates password strength
     * @param password the password to validate
     * @return true if password is valid, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return password.length() >= MIN_PASSWORD_LENGTH;
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
    public void setPassword(String password) {
        if (!isValidPassword(password)) {
            throw new IllegalArgumentException(
                "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }
        this.password = password;
    }
    public void setFirstName(String firstName) {this.firstName = firstName;}
    public void setLastName(String lastName) {this.lastName = lastName;}
    public void setEmail(String email) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.email = email;
    }
    public void setPhone(String phone) {this.phone = phone;}
    public void setIsLoggedIn(boolean isLoggedIn) {this.isLoggedIn = isLoggedIn;}


}