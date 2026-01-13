package common.dto;

import common.user.User;

import java.io.Serializable;

/**
 * Response DTO for authentication requests (login/register).
 */
public class AuthResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private User user;  // Only populated on successful login

    public AuthResponse() {}
    
    public AuthResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public AuthResponse(boolean success, String message, User user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }
    
    // Static factory methods for cleaner code
    public static AuthResponse success(User user) {
        return new AuthResponse(true, "Success", user);
    }
    
    public static AuthResponse success(String message) {
        return new AuthResponse(true, message);
    }
    
    public static AuthResponse failure(String message) {
        return new AuthResponse(false, message);
    }

    // ==================== GETTERS & SETTERS ====================
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
