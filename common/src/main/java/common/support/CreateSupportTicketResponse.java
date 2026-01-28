package common.support;

import java.io.Serializable;

public class CreateSupportTicketResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int ticketId;

    public CreateSupportTicketResponse(int ticketId) {
        this.ticketId = ticketId;
    }

    public int getTicketId() { return ticketId; }
}
