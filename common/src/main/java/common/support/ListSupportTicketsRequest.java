package common.support;

import java.io.Serializable;

public class ListSupportTicketsRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int agentId;

    public ListSupportTicketsRequest(int agentId) {
        this.agentId = agentId;
    }

    public int getAgentId() { return agentId; }
}
