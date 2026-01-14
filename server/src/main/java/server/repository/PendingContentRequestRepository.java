package server.repository;

import common.enums.ContentActionType;
import common.enums.ContentType;
import common.enums.RequestStatus;
import common.user.User;
import common.workflow.PendingContentRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PendingContentRequest entity operations.
 */
public class PendingContentRequestRepository extends BaseRepository<PendingContentRequest, Integer> {

    private static PendingContentRequestRepository instance;

    private PendingContentRequestRepository() {
        super(PendingContentRequest.class);
    }

    public static synchronized PendingContentRequestRepository getInstance() {
        if (instance == null) {
            instance = new PendingContentRequestRepository();
        }
        return instance;
    }

    /**
     * Create a new pending content request.
     */
    public boolean createPendingRequest(User requester, ContentActionType actionType,
                                         ContentType contentType, Integer targetId,
                                         String targetName, String contentDetails) {
        try {
            PendingContentRequest request = new PendingContentRequest(
                requester, actionType, contentType, targetId, targetName, contentDetails
            );
            save(request);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get all pending content requests.
     */
    public List<PendingContentRequest> findAllPending() {
        return executeQuery(session ->
            session.createQuery(
                "FROM PendingContentRequest p " +
                "LEFT JOIN FETCH p.requester " +
                "WHERE p.status = :status " +
                "ORDER BY p.createdAt DESC",
                PendingContentRequest.class)
                .setParameter("status", RequestStatus.OPEN)
                .getResultList()
        );
    }

    /**
     * Count pending content requests.
     */
    public int countPending() {
        return executeQuery(session -> {
            Long count = session.createQuery(
                "SELECT COUNT(p) FROM PendingContentRequest p WHERE p.status = :status",
                Long.class)
                .setParameter("status", RequestStatus.OPEN)
                .uniqueResult();
            return count != null ? count.intValue() : 0;
        });
    }

    /**
     * Approve a pending content request.
     */
    public boolean approve(int pendingId, User approver) {
        try {
            executeInTransaction(session -> {
                PendingContentRequest pending = session.get(PendingContentRequest.class, pendingId);
                
                if (pending == null || pending.getStatus() != RequestStatus.OPEN) {
                    throw new RuntimeException("Pending request not found or already processed");
                }

                // Apply the actual change based on action type
                applyContentChange(session, pending);

                // Mark as approved
                pending.setStatus(RequestStatus.APPROVED);
                pending.setProcessedAt(LocalDateTime.now());
                pending.setProcessedBy(approver);
                session.merge(pending);
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deny a pending content request.
     */
    public boolean deny(int pendingId, User denier) {
        try {
            executeInTransaction(session -> {
                PendingContentRequest pending = session.get(PendingContentRequest.class, pendingId);
                
                if (pending == null || pending.getStatus() != RequestStatus.OPEN) {
                    throw new RuntimeException("Pending request not found or already processed");
                }

                pending.setStatus(RequestStatus.DENIED);
                pending.setProcessedAt(LocalDateTime.now());
                pending.setProcessedBy(denier);
                session.merge(pending);
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Apply the actual content change when approved.
     * This method needs to parse contentDetails JSON and apply changes.
     */
    private void applyContentChange(org.hibernate.Session session, PendingContentRequest pending) {
        ContentActionType actionType = pending.getActionType();
        ContentType contentType = pending.getContentType();
        
        // Implementation depends on content type
        switch (contentType) {
            case MAP:
                applyMapChange(session, pending);
                break;
            case SITE:
                applySiteChange(session, pending);
                break;
            case TOUR:
                applyTourChange(session, pending);
                break;
        }
    }

    private void applyMapChange(org.hibernate.Session session, PendingContentRequest pending) {
        // Parse JSON and apply changes
        // This is a simplified implementation - use a proper JSON library
        switch (pending.getActionType()) {
            case ADD:
                // Create new map from contentDetails
                break;
            case EDIT:
                // Update existing map
                break;
            case DELETE:
                // Delete the map
                if (pending.getTargetId() != null) {
                    common.content.GCMMap map = session.get(common.content.GCMMap.class, pending.getTargetId());
                    if (map != null) {
                        session.remove(map);
                    }
                }
                break;
        }
    }

    private void applySiteChange(org.hibernate.Session session, PendingContentRequest pending) {
        // TODO: Similar implementation for sites
    }

    private void applyTourChange(org.hibernate.Session session, PendingContentRequest pending) {
        // TODO: Similar implementation for tours
    }
}
