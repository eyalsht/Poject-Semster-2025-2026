package common.workflow;

import common.enums.ContentActionType;
import common.enums.ContentType;
import common.enums.RequestStatus;
import common.user.User;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity for pending content changes (add/edit/delete maps, sites, tours).
 * These are approved by Content Managers.
 */
@Entity
@Table(name = "pending_content_requests")
public class PendingContentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id")
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ContentActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentType contentType;

    @Column(name = "target_id")
    private Integer targetId;  // ID of existing item (null for ADD)

    @Column(name = "target_name")
    private String targetName;  // Human-readable name (e.g., "Haifa - Downtown")

    @Lob
    @Column(name = "content_details", columnDefinition = "TEXT")
    private String contentDetails;  // JSON representation of the change

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RequestStatus status = RequestStatus.OPEN;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_user_id")
    private User processedBy;

    // ==================== CONSTRUCTORS ====================

    public PendingContentRequest() {
        this.createdAt = LocalDateTime.now();
    }

    public PendingContentRequest(User requester, ContentActionType actionType, 
                                  ContentType contentType, Integer targetId, 
                                  String targetName, String contentDetails) {
        this();
        this.requester = requester;
        this.actionType = actionType;
        this.contentType = contentType;
        this.targetId = targetId;
        this.targetName = targetName;
        this.contentDetails = contentDetails;
    }

    // ==================== GETTERS & SETTERS ====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }

    public ContentActionType getActionType() { return actionType; }
    public void setActionType(ContentActionType actionType) { this.actionType = actionType; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public Integer getTargetId() { return targetId; }
    public void setTargetId(Integer targetId) { this.targetId = targetId; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getContentDetails() { return contentDetails; }
    public void setContentDetails(String contentDetails) { this.contentDetails = contentDetails; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public User getProcessedBy() { return processedBy; }
    public void setProcessedBy(User processedBy) { this.processedBy = processedBy; }

    // ==================== DISPLAY METHODS (for TableView) ====================

    public String getType() {
        return actionType + " " + contentType;
    }

    public String getTarget() {
        return targetName != null ? targetName : "New " + contentType;
    }

    public String getInfo() {
        String requesterName = requester != null ? requester.getUsername() : "Unknown";
        return "By: " + requesterName + " | " + createdAt.toLocalDate();
    }

    public String getRequesterName() {
        return requester != null ? requester.getUsername() : "Unknown";
    }
}
