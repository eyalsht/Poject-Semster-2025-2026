package common.support;

import java.io.Serializable;

public class ReplySupportTicketRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int agentId;
    private final int ticketId;
    private final String replyText;

    public ReplySupportTicketRequest(int agentId, int ticketId, String replyText) {
        this.agentId = agentId;
        this.ticketId = ticketId;
        this.replyText = replyText;
    }

    public int getAgentId() { return agentId; }
    public int getTicketId() { return ticketId; }
    public String getReplyText() { return replyText; }
}
