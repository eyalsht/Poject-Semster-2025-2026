package common.support;

import java.io.Serializable;

public class ListClientSupportRepliesRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int userId;

    public ListClientSupportRepliesRequest(int userId) {
        this.userId = userId;
    }

    public int getUserId() { return userId; }
}
