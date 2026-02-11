package server.repository;

import common.content.City;
import common.content.GCMMap;
import common.purchase.OneTimePurchase;
import common.purchase.Purchase;
import common.purchase.PurchasedMapSnapshot;
import common.purchase.Subscription;
import common.user.Client;
import common.user.User;


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
     * Log a new subscription purchase.
     */
    public Subscription createSubscription(int userId, int cityId, double price, int months,
                                              boolean isRenewal, LocalDate latestExpiration)
    {
        final Subscription subscription = new Subscription();
        // Use native SQL to avoid Hibernate detached entity / closed connection issues
        executeInTransaction(session -> {
            LocalDate today = LocalDate.now();
            LocalDate base = (isRenewal && latestExpiration != null) ? latestExpiration : today;
            LocalDate newExpiration = base.plusMonths(months);

            // Insert via native SQL to bypass entity proxy issues
            session.createNativeQuery(
                "INSERT INTO purchases (purchase_type, user_id, city_id, price, purchase_date, expiration_date, is_renewal) " +
                "VALUES ('SUBSCRIPTION', :userId, :cityId, :price, :purchaseDate, :expirationDate, :isRenewal)")
                .setParameter("userId", userId)
                .setParameter("cityId", cityId)
                .setParameter("price", price)
                .setParameter("purchaseDate", today)
                .setParameter("expirationDate", newExpiration)
                .setParameter("isRenewal", isRenewal)
                .executeUpdate();

            // Populate the returned object for logging
            subscription.setPricePaid(price);
            subscription.setPurchaseDate(today);
            subscription.setExpirationDate(newExpiration);
            subscription.setRenewal(isRenewal);
        });
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
}
