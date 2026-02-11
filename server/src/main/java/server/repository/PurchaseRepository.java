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
    public Subscription createSubscription(User user, City city, double price, int months)
    {
        LocalDate today = LocalDate.now();

        // Find the latest expiration date of any subscription for this user+city
        LocalDate latestExpiration = executeQuery(session ->
                session.createQuery(
                                "select max(s.expirationDate) " +
                                        "from Subscription s " +
                                        "where s.user.id = :userId and s.city.id = :cityId",
                                LocalDate.class)
                        .setParameter("userId", user.getId())
                        .setParameter("cityId", city.getId())
                        .getSingleResult()
        );

        boolean renewal = (latestExpiration != null && !latestExpiration.isBefore(today));

        LocalDate base = (renewal ? latestExpiration : today);
        LocalDate newExpiration = base.plusMonths(months);
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setCity(city);
        subscription.setPricePaid(price);
        subscription.setPurchaseDate(LocalDate.now());
        subscription.setExpirationDate(newExpiration);
        subscription.setRenewal(renewal);

        save(subscription);
        return subscription;
    }

    /**
     * Log a new one-time purchase for a map.
     * Creates a snapshot of the map at purchase time.
     */
    public OneTimePurchase createOneTimePurchase(User user, GCMMap map, double price) {
        OneTimePurchase purchase = new OneTimePurchase();
        purchase.setUser(user);
        purchase.setMap(map);
        purchase.setPricePaid(price);
        purchase.setPurchaseDate(LocalDate.now());
        purchase.setPurchasedVersion(map.getVersion());

        // Create snapshot if user is a Client
        if (user instanceof Client client) {
            PurchasedMapSnapshot snapshot = new PurchasedMapSnapshot(client, map, price);
            client.addPurchasedMap(snapshot);
            purchase.setSnapshot(snapshot);
        }

        save(purchase);
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
