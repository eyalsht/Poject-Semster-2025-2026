package entities;

import java.io.Serializable;
import java.util.regex.Pattern;
import entities.UserRole;



public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;
    protected int id;
    protected String username;
    protected String password;
    protected String firstName;
    protected String lastName;
    protected String email;
    protected boolean isLoggedIn;
    protected UserRole role;

    // --- קבועים ו-Regex מתוך Lab 3 ---
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 12;

    // Regex לאימייל
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9%+_.-]+@[a-zA-Z0-9][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    // Regex לסיסמה (אות גדולה, מספר, סימן מיוחד)
    private static final String PASSWORD_REGEX = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+])[a-zA-Z\\d!@#$%^&*()_+]+$";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    protected int failedAttempts = 0;
    protected boolean isBlocked = false;

    public User(int id, String username, String password, String firstName, String lastName, String email) {
        this.id = id;
        this.username = username;
        this.password = password; // Use setter to validate
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email; // Use setter to validate
        this.isLoggedIn = false;
    }
    public User() {
        this.id = 0;
        this.username = "";
        setPassword(""); // Use setter to validate
        this.firstName = "";
        this.lastName = "";
        setEmail(""); // Use setter to validate
        this.isLoggedIn = false;
    }
    public static void validateUsername(String name) throws Exception {
        if (name == null || name.isEmpty())
            throw new Exception("Username cannot be empty");
        if (name.length() > MAX_USERNAME_LENGTH)
            throw new Exception("Username is too long (max " + MAX_USERNAME_LENGTH + " chars)");
        if (!EMAIL_PATTERN.matcher(name).matches())
            throw new Exception("Please enter a valid Email as username");
    }

    public static void validatePassword(String password) throws Exception {
        if (password == null || password.isEmpty())
            throw new Exception("Password cannot be empty");
        if (password.length() < MIN_PASSWORD_LENGTH)
            throw new Exception("Password too short (min " + MIN_PASSWORD_LENGTH + " chars)");
        if (password.length() > MAX_PASSWORD_LENGTH)
            throw new Exception("Password too long (max " + MAX_PASSWORD_LENGTH + " chars)");
        // אם רוצים לאכוף
        if (!PASSWORD_PATTERN.matcher(password).matches())
            throw new Exception("Password must contain a letter, digit, and symbol");
    }

    public int getId() {return id;}
    public String getUsername() {return username;}
    public String getPassword() {return password;}
    public String getFirstName() {return firstName;}
    public String getLastName() {return lastName;}
    public String getEmail() {return email;}
    public boolean isLoggedIn() {return isLoggedIn;}
    public UserRole getRole() { return role; }


    public void setId(int id) {this.id = id;}
    public void setUsername(String username) {this.username = username;}
    public void setPassword(String password) {this.password = password;}
    public void setFirstName(String firstName) {this.firstName = firstName;}
    public void setLastName(String lastName) {this.lastName = lastName;}
    public void setEmail(String email) {this.email = email;}
    public void setRole(UserRole role) { this.role = role; }

    public void login() {
        this.isLoggedIn = true;
    }
    public void logout() {
        this.isLoggedIn = false;
    }

    public synchronized int getFailedAttempts() {
        return failedAttempts;
    }

    public synchronized void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public synchronized void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    public synchronized void resetFailedAttempts() {
        this.failedAttempts = 0;
    }

    public synchronized boolean isBlocked() {
        return isBlocked;
    }

    public synchronized void setBlocked(boolean blocked) {
        this.isBlocked = blocked;
    }
}