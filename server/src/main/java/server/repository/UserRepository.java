package server.repository;

import common.user.Client;
import common.user.User;

import java.util.Optional;

/**
 * Repository for User entity operations.
 * Handles User, Client, and Employee entities (Single Table Inheritance).
 */
public class UserRepository extends BaseRepository<User, Integer> {

    // Singleton instance
    private static UserRepository instance;

    private UserRepository() {
        super(User.class);
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    // ==================== USER-SPECIFIC QUERIES ====================

    /**
     * Find user by username (for login).
     * This replaces DBController.getUserForLogin()
     */
    public Optional<User> findByUsername(String username) {
        return executeQuery(session ->
            session.createQuery("FROM User u WHERE u.username = :username", User.class)
                   .setParameter("username", username)
                   .uniqueResultOptional()
        );
    }

    /**
     * Find user by email.
     */
    public Optional<User> findByEmail(String email) {
        return executeQuery(session ->
            session.createQuery("FROM User u WHERE u.email = :email", User.class)
                   .setParameter("email", email)
                   .uniqueResultOptional()
        );
    }

    /**
     * Check if a user exists with given email or ID number.
     * Used for registration validation.
     */
    public boolean existsByEmailOrIdNumber(String email, String idNumber) {
        return executeQuery(session -> {
            Long count = session.createQuery(
                "SELECT COUNT(c) FROM Client c WHERE c.email = :email OR c.creditCardInfo = :idNumber", 
                Long.class)
                .setParameter("email", email)
                .setParameter("idNumber", idNumber)
                .uniqueResult();
            return count != null && count > 0;
        });
    }

    /**
     * Register a new client.
     * This replaces DBController.registerUser()
     *
     * @return true if registration successful, false if user already exists
     */
    public boolean registerClient(String firstName, String lastName, String idNumber,
                                   String email, String password, String cardNumber) {
        // Check if user already exists
        if (existsByEmailOrIdNumber(email, idNumber)) {
            return false;
        }

        // Create new Client entity
        Client client = new Client();
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setUsername(email);  // Username = email
        client.setEmail(email);
        client.setPassword(password);
        client.setCreditCardInfo(cardNumber);

        try {
            save(client);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update user's security state (failed attempts, blocked status).
     * This replaces DBController.updateUserSecurityState()
     */
    public void updateSecurityState(User user) {
        executeInTransaction(session -> {
            // Only update the security fields
            User managed = session.get(User.class, user.getId());
            if (managed != null) {
                managed.setFailedAttempts(user.getFailedAttempts());
                managed.setBlocked(user.isBlocked());
            }
        });
    }

    /**
     * Find client by ID (specific type).
     */
    public Optional<Client> findClientById(int id) {
        return executeQuery(session ->
            Optional.ofNullable(session.get(Client.class, id))
        );
    }

    /**
     * Update client's subscription expiry date.
     */
    public void updateSubscriptionExpiry(int userId, java.time.LocalDate newExpiryDate) {
        executeInTransaction(session -> {
            Client client = session.get(Client.class, userId);
            if (client != null) {
                client.setSubscriptionExpiry(newExpiryDate);
            }
        });
    }
}
