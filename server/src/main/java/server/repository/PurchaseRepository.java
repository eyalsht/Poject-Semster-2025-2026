package server.repository;

import common.content.City;
import common.content.GCMMap;
import common.purchase.OneTimePurchase;
import common.purchase.Purchase;
import common.purchase.Subscription;
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
    public Subscription createSubscription(User user, City city, double price, int months) {
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setCity(city);
        subscription.setPricePaid(price);
        subscription.setPurchaseDate(LocalDate.now());
        subscription.setExpirationDate(LocalDate.now().plusMonths(months));

        save(subscription);
        return subscription;
    }

    /**
     * Log a new one-time purchase.
     */
    public OneTimePurchase createOneTimePurchase(User user, City city, GCMMap map, double price) {
        OneTimePurchase purchase = new OneTimePurchase();
        purchase.setUser(user);
        purchase.setCity(city);
        purchase.setMap(map);
        purchase.setPricePaid(price);
        purchase.setPurchaseDate(LocalDate.now());
        purchase.setPurchasedVersion(map.getVersion());

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
}
