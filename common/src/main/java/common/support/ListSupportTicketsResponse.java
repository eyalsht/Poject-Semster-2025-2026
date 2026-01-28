package common.support;

import java.io.Serializable;
import java.util.List;

public class ListSupportTicketsResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<SupportTicketRowDTO> rows;

    public ListSupportTicketsResponse(List<SupportTicketRowDTO> rows) {
        this.rows = rows;
    }

    public List<SupportTicketRowDTO> getRows() { return rows; }
}
