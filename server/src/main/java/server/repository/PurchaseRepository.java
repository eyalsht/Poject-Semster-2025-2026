package server.repository;

import common.content.City;
import common.content.GCMMap;
import common.purchase.OneTimePurchase;
import common.purchase.Purchase;
import common.purchase.PurchasedMapSnapshot;
import common.purchase.Subscription;
import common.user.Client;
import common.user.User;
import server.HibernateUtil;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import server.NotificationService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;


/**
 * Repository for Purchase entity operations.
 */
public class PurchaseRepository extends BaseRepository<Purchase, Integer> {

    private static PurchaseRepository instance;

    private PurchaseRepository() {
        super(Purchase.class);
    }

    public static synchronized PurchaseRepository getInstance() {
        if (instance == null) {
            instance = new PurchaseRepository();
        }
        return instance;
    }

    // ==================== PURCHASE-SPECIFIC OPERATIONS ====================

    /**
     * Create a subscription: queries for renewal status AND inserts in a single transaction
     * to avoid connection pool issues between separate calls.
     *
     * @return a populated Subscription object (not managed by Hibernate)
     */
    public Subscription createSubscription(int userId, int cityId, double monthlyPrice, int months)
    {
        // Bypass Hibernate session management entirely - use raw JDBC
        // to avoid the persistent "connection is closed" issue
        SessionFactoryImplementor sfi = (SessionFactoryImplementor) HibernateUtil.getSessionFactory();
        ConnectionProvider cp = sfi.getServiceRegistry().getService(ConnectionProvider.class);

        final Subscription subscription = new Subscription();
        Connection conn = null;
        try {
            conn = cp.getConnection();
            conn.setAutoCommit(false);

            // Fix schema: map_id must be nullable for SINGLE_TABLE inheritance (subscriptions have no map)
            try (PreparedStatement alter = conn.prepareStatement(
                    "ALTER TABLE purchases MODIFY COLUMN map_id INT NULL")) {
                alter.executeUpdate();
            } catch (Exception ignored) { /* already nullable */ }

            LocalDate today = LocalDate.now();

            // 1. Check for renewal
            LocalDate latestExpiration = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT MAX(expiration_date) FROM purchases WHERE purchase_type = 'SUBSCRIPTION' AND user_id = ? AND city_id = ?")) {
                ps.setInt(1, userId);
                ps.setInt(2, cityId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getDate(1) != null) {
                        latestExpiration = rs.getDate(1).toLocalDate();
                    }
                }
            }

            boolean isRenewal = (latestExpiration != null && !latestExpiration.isBefore(today));
            LocalDate base = isRenewal ? latestExpiration : today;
            LocalDate newExpiration = base.plusMonths(months);

            // 2. Calculate discounts
            double durationDiscount = 0;
            if (months >= 12) durationDiscount = 0.15;
            else if (months >= 6) durationDiscount = 0.10;
            else if (months >= 3) durationDiscount = 0.05;
            double renewalDiscount = isRenewal ? 0.10 : 0;
            double totalPrice = monthlyPrice * months * (1 - (durationDiscount + renewalDiscount));

            // 3. Insert (include map_id, purchased_version, snapshot_id as NULL for SINGLE_TABLE inheritance)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO purchases (purchase_type, user_id, city_id, map_id, price, purchase_date, expiration_date, is_renewal, purchased_version, snapshot_id) " +
                    "VALUES ('SUBSCRIPTION', ?, ?, NULL, ?, ?, ?, ?, NULL, NULL)")) {
                ps.setInt(1, userId);
                ps.setInt(2, cityId);
                ps.setDouble(3, totalPrice);
                ps.setDate(4, java.sql.Date.valueOf(today));
                ps.setDate(5, java.sql.Date.valueOf(newExpiration));
                ps.setBoolean(6, isRenewal);
                ps.executeUpdate();
            }

            conn.commit();

            // Populate returned object for logging
            subscription.setPricePaid(totalPrice);
            subscription.setPurchaseDate(today);
            subscription.setExpirationDate(newExpiration);
            subscription.setRenewal(isRenewal);

        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (Exception ignored) {}
            throw new RuntimeException("Subscription creation failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) try { cp.closeConnection(conn); } catch (Exception ignored) {}
        }
        return subscription;
    }

    /**
     * Log a new one-time purchase for a map.
     * Creates a snapshot of the map at purchase time.
     */
    public OneTimePurchase createOneTimePurchase(User user, GCMMap map, double price) {
        final OneTimePurchase purchase = new OneTimePurchase();
        // Use native SQL to avoid Hibernate detached entity / closed connection issues
        executeInTransaction(session -> {
            LocalDate today = LocalDate.now();

            // Insert purchase record via native SQL
            session.createNativeQuery(
                "INSERT INTO purchases (purchase_type, user_id, city_id, map_id, price, purchase_date, purchased_version, is_renewal) " +
                "VALUES ('ONE_TIME', :userId, :cityId, :mapId, :price, :purchaseDate, :version, false)")
                .setParameter("userId", user.getId())
                .setParameter("cityId", map.getCity() != null ? map.getCity().getId() : null)
                .setParameter("mapId", map.getId())
                .setParameter("price", price)
                .setParameter("purchaseDate", today)
                .setParameter("version", map.getVersion())
                .executeUpdate();

            // Create snapshot record if user is a Client
            if (user instanceof Client) {
                session.createNativeQuery(
                    "INSERT INTO purchased_map_snapshots (client_id, original_map_id, map_name, city_name, purchased_version, description, purchase_date, price_paid) " +
                    "VALUES (:clientId, :mapId, :mapName, :cityName, :version, :desc, :purchaseDate, :price)")
                    .setParameter("clientId", user.getId())
                    .setParameter("mapId", map.getId())
                    .setParameter("mapName", map.getName())
                    .setParameter("cityName", map.getCityName())
                    .setParameter("version", map.getVersion())
                    .setParameter("desc", map.getDescription())
                    .setParameter("purchaseDate", today)
                    .setParameter("price", price)
                    .executeUpdate();
            }

            // Populate returned object for logging
            purchase.setPricePaid(price);
            purchase.setPurchaseDate(today);
            purchase.setPurchasedVersion(map.getVersion());
        });
        return purchase;
    }

    /**
     * Get all purchases for a user.
     */
    public List<Purchase> findByUserId(int userId) {
        return executeQuery(session ->
            session.createQuery(
                "FROM Purchase p WHERE p.user.id = :userId ORDER BY p.purchaseDate DESC",
                Purchase.class)
                   .setParameter("userId", userId)
                   .getResultList()
        );
    }

    /**
     * Find all active (non-expired) subscriptions for a user.
     */
    public List<Subscription> findActiveSubscriptionsByUserId(int userId) {
        return executeQuery(session ->
            session.createQuery(
                "FROM Subscription s WHERE s.user.id = :userId AND s.expirationDate >= :today ORDER BY s.expirationDate DESC",
                Subscription.class)
                   .setParameter("userId", userId)
                   .setParameter("today", LocalDate.now())
                   .getResultList()
        );
    }

    /**
     * Find the latest subscription for a user+city pair.
     */
    public Subscription findLatestSubscription(int userId, int cityId) {
        return executeQuery(session -> {
            List<Subscription> results = session.createQuery(
                "FROM Subscription s WHERE s.user.id = :userId AND s.city.id = :cityId ORDER BY s.expirationDate DESC",
                Subscription.class)
                   .setParameter("userId", userId)
                   .setParameter("cityId", cityId)
                   .setMaxResults(1)
                   .getResultList();
            return results.isEmpty() ? null : results.get(0);
        });
    }

    /**
     * Finds the latest expiration date for a user+city pair (lightweight native SQL, no entity loading).
     */
    public LocalDate findLatestExpirationDate(int userId, int cityId) {
        return executeQuery(session -> {
            Object result = session.createNativeQuery(
                "SELECT MAX(expiration_date) FROM purchases WHERE purchase_type = 'SUBSCRIPTION' AND user_id = :userId AND city_id = :cityId")
                .setParameter("userId", userId)
                .setParameter("cityId", cityId)
                .getSingleResult();
            if (result == null) return null;
            if (result instanceof java.sql.Date) return ((java.sql.Date) result).toLocalDate();
            if (result instanceof LocalDate) return (LocalDate) result;
            return LocalDate.parse(result.toString());
        });
    }

    /**
     * Find all purchased map snapshots for a user.
     */
    public List<PurchasedMapSnapshot> findPurchasedMapsByUserId(int userId) {
        return executeQuery(session ->
            session.createQuery(
                "FROM PurchasedMapSnapshot s WHERE s.client.id = :userId ORDER BY s.purchaseDate DESC",
                PurchasedMapSnapshot.class)
                   .setParameter("userId", userId)
                   .getResultList()
        );
    }

    /**
     * Lightweight native SQL: check subscription status for a user+city pair.
     * Returns Object[] { expiration_date (java.sql.Date), city_name (String), price_sub (Double) }
     * or null if no subscription exists.
     */
    public Object[] checkSubscriptionStatusNative(int userId, int cityId) {
        return executeQuery(session -> {
            Object[] result = (Object[]) session.createNativeQuery(
                "SELECT MAX(p.expiration_date), c.name, c.price_sub " +
                "FROM purchases p JOIN cities c ON p.city_id = c.id " +
                "WHERE p.purchase_type = 'SUBSCRIPTION' AND p.user_id = :userId AND p.city_id = :cityId " +
                "GROUP BY c.name, c.price_sub")
                .setParameter("userId", userId)
                .setParameter("cityId", cityId)
                .getSingleResultOrNull();
            return result;
        });
    }

    /**
     * Lightweight native SQL: get all active subscriptions for a user (grouped by city).
     * Each Object[] contains { city_id (Integer), expiration_date (java.sql.Date), city_name (String), price_sub (Double) }
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findActiveSubscriptionDTOsNative(int userId) {
        return executeQuery(session ->
            session.createNativeQuery(
                "SELECT p.city_id, MAX(p.expiration_date) as expiry, c.name, c.price_sub " +
                "FROM purchases p JOIN cities c ON p.city_id = c.id " +
                "WHERE p.purchase_type = 'SUBSCRIPTION' AND p.user_id = :userId " +
                "GROUP BY p.city_id, c.name, c.price_sub " +
                "HAVING MAX(p.expiration_date) >= CURRENT_DATE " +
                "ORDER BY expiry DESC")
                .setParameter("userId", userId)
                .getResultList()
        );
    }

    /**
     * Find a purchased snapshot for a specific user and map.
     */
    public PurchasedMapSnapshot findPurchasedSnapshot(int userId, int mapId) {
        return executeQuery(session -> {
            List<PurchasedMapSnapshot> results = session.createQuery(
                "FROM PurchasedMapSnapshot s WHERE s.client.id = :userId AND s.originalMap.id = :mapId ORDER BY s.purchaseDate DESC",
                PurchasedMapSnapshot.class)
                   .setParameter("userId", userId)
                   .setParameter("mapId", mapId)
                   .setMaxResults(1)
                   .getResultList();
            return results.isEmpty() ? null : results.get(0);
        });
    }
    /**
     * סורק את מסד הנתונים ושולח התראות למשתמשים שהמנוי שלהם מסתיים בעוד 3 ימים.
     */
    public void checkAndNotifyExpiringSubscriptions()
    {
        java.time.LocalDate targetDate = java.time.LocalDate.now().plusDays(3);

        List<Object[]> results = executeQuery(session ->
                session.createNativeQuery(
                                "SELECT u.email, cl.phone_number, u.first_name, c.name as city_name " +
                                        "FROM purchases p " +
                                        "JOIN users u ON p.user_id = u.id " +
                                        "JOIN cities c ON p.city_id = c.id " +
                                        "JOIN clients cl ON u.id = cl.id " +
                                        "WHERE p.purchase_type = 'SUBSCRIPTION' " +
                                        "AND p.expiration_date = :targetDate")
                        .setParameter("targetDate", java.sql.Date.valueOf(targetDate))
                        .getResultList()
        );

        for (Object[] row : results)
        {
            String email = (String) row[0];
            String phone = (String) row[1];
            String firstName = (String) row[2];
            String cityName = (String) row[3];

            NotificationService.sendSubscriptionAlert(
                    email,
                    phone,
                    firstName,
                    3,
                    cityName
            );

            System.out.println("[Notification] Sent 3-day reminder to: " + email + " (Phone: " + phone + ") for city: " + cityName);
        }
    }
}
