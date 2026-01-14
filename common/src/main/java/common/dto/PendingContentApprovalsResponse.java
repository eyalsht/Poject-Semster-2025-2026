package common.dto;

import common.workflow.PendingContentRequest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for pending content approvals.
 */
public class PendingContentApprovalsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<PendingContentRequest> pendingRequests = new ArrayList<>();
    private int totalCount;

    public PendingContentApprovalsResponse() {}

    public PendingContentApprovalsResponse(List<PendingContentRequest> pendingRequests) {
        this.pendingRequests = pendingRequests;
        this.totalCount = pendingRequests != null ? pendingRequests.size() : 0;
    }

    public List<PendingContentRequest> getPendingRequests() { return pendingRequests; }
    public void setPendingRequests(List<PendingContentRequest> pendingRequests) {
        this.pendingRequests = pendingRequests;
        this.totalCount = pendingRequests != null ? pendingRequests.size() : 0;
    }

    public int getTotalCount() { return totalCount; }
}
