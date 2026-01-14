package common.dto;

import common.enums.ContentActionType;
import common.enums.ContentType;
import java.io.Serializable;

/**
 * DTO for submitting content change requests.
 */
public class ContentChangeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer requesterId;
    private ContentActionType actionType;
    private ContentType contentType;
    private Integer targetId;          // For EDIT/DELETE
    private String targetName;
    private String contentDetailsJson; // JSON with new values

    public ContentChangeRequest() {}

    public ContentChangeRequest(Integer requesterId, ContentActionType actionType,
                                 ContentType contentType, Integer targetId,
                                 String targetName, String contentDetailsJson) {
        this.requesterId = requesterId;
        this.actionType = actionType;
        this.contentType = contentType;
        this.targetId = targetId;
        this.targetName = targetName;
        this.contentDetailsJson = contentDetailsJson;
    }

    // Getters and Setters
    public Integer getRequesterId() { return requesterId; }
    public void setRequesterId(Integer requesterId) { this.requesterId = requesterId; }

    public ContentActionType getActionType() { return actionType; }
    public void setActionType(ContentActionType actionType) { this.actionType = actionType; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public Integer getTargetId() { return targetId; }
    public void setTargetId(Integer targetId) { this.targetId = targetId; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getContentDetailsJson() { return contentDetailsJson; }
    public void setContentDetailsJson(String contentDetailsJson) { this.contentDetailsJson = contentDetailsJson; }
}
