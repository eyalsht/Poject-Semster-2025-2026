package server.repository;

import common.content.City;
import common.content.GCMMap;
import common.workflow.PendingPriceUpdate;
import common.enums.RequestStatus;
import common.user.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PendingPriceUpdate entity operations.
 */
public class PendingPriceUpdateRepository extends BaseRepository<PendingPriceUpdate, Integer> {

    private static PendingPriceUpdateRepository instance;

    private PendingPriceUpdateRepository() {
        super(PendingPriceUpdate.class);
    }

    public static synchronized PendingPriceUpdateRepository getInstance() {
        if (instance == null) {
            instance = new PendingPriceUpdateRepository();
        }
        return instance;
    }

    // ==================== PENDING PRICE UPDATE OPERATIONS ====================

    /**
     * Create a new pending price update request.
     */
    public boolean createPendingUpdate(GCMMap map, User requester, double newPrice) {
        try {
            PendingPriceUpdate pending = new PendingPriceUpdate(
                map,
                requester,
                map.getPrice(),  // Current price becomes old price
                newPrice
            );
            save(pending);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all pending (not yet approved/denied) price updates.
     */
    public List<PendingPriceUpdate> findAllPending() {
        return executeQuery(session ->
            session.createQuery(
                "FROM PendingPriceUpdate p " +
                "LEFT JOIN FETCH p.map m " +
                "LEFT JOIN FETCH m.city " +
                "WHERE p.status = :status " +
                "ORDER BY p.createdAt DESC",
                PendingPriceUpdate.class)
                .setParameter("status", RequestStatus.OPEN)
                .getResultList()
        );
    }

    /**
     * Count pending price updates.
     */
    public int countPending() {
        return executeQuery(session -> {
            Long count = session.createQuery(
                "SELECT COUNT(p) FROM PendingPriceUpdate p WHERE p.status = :status",
                Long.class)
                .setParameter("status", RequestStatus.OPEN)
                .uniqueResult();
            return count != null ? count.intValue() : 0;
        });
    }

    /**
     * Approve a pending price update.
     * Updates the map's price and marks the request as closed.
     */
    public boolean approve(int pendingId) {
        try {
            executeInTransaction(session -> {
                PendingPriceUpdate pending = session.get(PendingPriceUpdate.class, pendingId);
                
                if (pending == null || pending.getStatus() != RequestStatus.OPEN) {
                    throw new RuntimeException("Pending update not found or already processed");
                }

                if (pending.isSubscriptionChange()) {
                    // Update the city's subscription price
                    City city = pending.getMap().getCity();
                    city.setPriceSub(pending.getNewPrice());
                    session.merge(city);
                } else {
                    // Update the map's price
                    GCMMap map = pending.getMap();
                    map.setPrice(pending.getNewPrice());
                    session.merge(map);
                }

                // Mark as closed
                pending.setStatus(RequestStatus.CLOSED);
                session.merge(pending);
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deny a pending price update.
     * Just marks the request as closed without changing the price.
     */
    public boolean deny(int pendingId) {
        try {
            executeInTransaction(session -> {
                PendingPriceUpdate pending = session.get(PendingPriceUpdate.class, pendingId);
                
                if (pending == null || pending.getStatus() != RequestStatus.OPEN) {
                    throw new RuntimeException("Pending update not found or already processed");
                }

                // Just mark as closed (could use a DENIED status if you want)
                pending.setStatus(RequestStatus.CLOSED);
                session.merge(pending);
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Find pending update by ID with map loaded.
     */
    public Optional<PendingPriceUpdate> findByIdWithMap(int id) {
        return executeQuery(session ->
            session.createQuery(
                "FROM PendingPriceUpdate p " +
                "LEFT JOIN FETCH p.map m " +
                "LEFT JOIN FETCH m.city " +
                "WHERE p.id = :id",
                PendingPriceUpdate.class)
                .setParameter("id", id)
                .uniqueResultOptional()
        );
    }
}
