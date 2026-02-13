package common.report;

import common.support.SupportTicketRowDTO;
import java.io.Serializable;
import java.util.List;

public class SupportRequestsReport implements Serializable {
    public int pendingCount;
    public int doneCount;
    public List<SupportTicketRowDTO> rows;

    public SupportRequestsReport(int pendingCount, int doneCount, List<SupportTicketRowDTO> rows) {
        this.pendingCount = pendingCount;
        this.doneCount = doneCount;
        this.rows = rows;
    }
}
