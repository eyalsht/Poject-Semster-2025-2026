package common.support;

import java.io.Serializable;

public class CreateSupportTicketRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int userId;
    private final String topic;
    private final String text;

    public CreateSupportTicketRequest(int userId, String topic, String text) {
        this.userId = userId;
        this.topic = topic;
        this.text = text;
    }

    public int getUserId() { return userId; }
    public String getTopic() { return topic; }
    public String getText() { return text; }
}
