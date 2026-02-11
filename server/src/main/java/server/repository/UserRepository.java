package server.repository;

import common.purchase.PaymentDetails;
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
     * Check if email is already registered.
     */
    public boolean isEmailTaken(String email) {
        return executeQuery(session -> {
            Long count = session.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                    .setParameter("email", email)
                    .uniqueResult();
            return count != null && count > 0;
        });
    }

    public boolean isUsernameTaken(String username) {
        return executeQuery(session -> {
            Long count = session.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                    .setParameter("username", username)
                    .uniqueResult();
            return count != null && count > 0;
        });
    }

    /**
     * Register a new client
     * @return true if registration successful
     */
    public boolean registerClient(String firstName, String lastName,
                                  String email, String phone, String username, String password, PaymentDetails payment) {

        if (isUsernameTaken(username) || isEmailTaken(email)) {
            return false;
        }

        Client client = new Client();
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setUsername(username);
        client.setEmail(email);
        client.setPhoneNumber(phone);
        client.setPassword(password);
        client.setPaymentDetails(payment);

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
     * Update client's payment details.
     */
    public void updatePaymentDetails(int userId, PaymentDetails newPayment) {
        executeInTransaction(session -> {
            Client client = session.get(Client.class, userId);
            if (client != null) {
                client.setPaymentDetails(newPayment);
            }
        });
    }

}
