package common.support;

import java.io.Serializable;
import java.util.List;

public class ListClientSupportRepliesResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<SupportTicketRowDTO> rows;

    public ListClientSupportRepliesResponse(List<SupportTicketRowDTO> rows) {
        this.rows = rows;
    }

    public List<SupportTicketRowDTO> getRows() { return rows; }
}
