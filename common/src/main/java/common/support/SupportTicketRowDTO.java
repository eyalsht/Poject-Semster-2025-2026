package common.support;

import java.io.Serializable;
import java.time.LocalDateTime;
import common.enums.SupportTicketStatus;

public class SupportTicketRowDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int ticketId;
    private final String clientUsername;
    private final String topic;
    private final SupportTicketStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime repliedAt;
    private final boolean readByClient;
    private final String preview;

    public SupportTicketRowDTO(int ticketId, String clientUsername, String topic,
                               SupportTicketStatus status, LocalDateTime createdAt,
                               LocalDateTime repliedAt, boolean readByClient, String preview) {
        this.ticketId = ticketId;
        this.clientUsername = clientUsername;
        this.topic = topic;
        this.status = status;
        this.createdAt = createdAt;
        this.repliedAt = repliedAt;
        this.readByClient = readByClient;
        this.preview = preview;
    }

    public int getTicketId() { return ticketId; }
    public String getClientUsername() { return clientUsername; }
    public String getTopic() { return topic; }
    public SupportTicketStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getRepliedAt() { return repliedAt; }
    public boolean isReadByClient() { return readByClient; }
    public String getPreview() { return preview; }
}
