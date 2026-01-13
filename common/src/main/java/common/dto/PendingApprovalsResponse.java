package common.dto;

import common.workflow.PendingPriceUpdate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for pending approvals request.
 * Includes both the list and the count (eliminates need for separate count request).
 */
public class PendingApprovalsResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private List<PendingPriceUpdate> pendingUpdates = new ArrayList<>();
    private int totalCount;

    public PendingApprovalsResponse() {}
    
    public PendingApprovalsResponse(List<PendingPriceUpdate> pendingUpdates) {
        this.pendingUpdates = pendingUpdates;
        this.totalCount = pendingUpdates != null ? pendingUpdates.size() : 0;
    }

    // ==================== GETTERS & SETTERS ====================
    
    public List<PendingPriceUpdate> getPendingUpdates() { return pendingUpdates; }
    public void setPendingUpdates(List<PendingPriceUpdate> pendingUpdates) {
        this.pendingUpdates = pendingUpdates;
        this.totalCount = pendingUpdates != null ? pendingUpdates.size() : 0;
    }

    public int getTotalCount() { return totalCount; }
}
