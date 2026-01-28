package common.support;

import java.io.Serializable;

public class MarkSupportReplyReadRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int userId;
    private final int ticketId;

    public MarkSupportReplyReadRequest(int userId, int ticketId) {
        this.userId = userId;
        this.ticketId = ticketId;
    }

    public int getUserId() { return userId; }
    public int getTicketId() { return ticketId; }
}
