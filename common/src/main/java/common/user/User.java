package common.user;

import java.io.Serializable;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)  // Changed from SINGLE_TABLE
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected int id;

    @Column(name = "username", length = 50)
    protected String username;

    @Column(name = "password")
    protected String password;

    @Column(name = "first_name")
    protected String firstName;

    @Column(name = "last_name")
    protected String lastName;

    @Column(name = "email")
    protected String email;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }


    @Transient  // Not persisted - runtime state only
    protected boolean isLoggedIn;

    // Constants
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 12;

    @Transient
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9%+_.-]+@[a-zA-Z0-9][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}$";
    @Transient
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    @Transient
    private static final String PASSWORD_REGEX = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+])[a-zA-Z\\d!@#$%^&*()_+]+$";
    @Transient
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    @Column(name = "failed_attempts")
    protected int failedAttempts = 0;

    @Column(name = "is_blocked")
    protected boolean isBlocked = false;

    // ==================== CONSTRUCTORS ====================
    
    // JPA requires a no-arg constructor
    public User() {
        this.isLoggedIn = false;
    }

    public User(int id, String username, String password, String firstName, String lastName, String email) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.isLoggedIn = false;
    }

    public User(int id, String username, String password, String email) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.firstName = "";
        this.lastName = "";
        this.email = email;
        this.isLoggedIn = false;
    }

    // ==================== GETTERS & SETTERS ====================
    
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public boolean isLoggedIn() { return isLoggedIn; }

    public void setId(int id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }

    public void login() { this.isLoggedIn = true; }
    public void logout() { this.isLoggedIn = false; }

    public synchronized int getFailedAttempts() { return failedAttempts; }
    public synchronized void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
    public synchronized void incrementFailedAttempts() { this.failedAttempts++; }
    public synchronized void resetFailedAttempts() { this.failedAttempts = 0; }

    public synchronized boolean isBlocked() { return isBlocked; }
    public synchronized void setBlocked(boolean blocked) { this.isBlocked = blocked; }
}