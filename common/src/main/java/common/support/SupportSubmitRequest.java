package common.support;

import java.io.Serializable;

public class SupportSubmitRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int userId;
    private final String topic;      // e.g. "MEMBERSHIP_EXPIRE"
    private final String messageText; // optional
    private final Integer cityId;     // optional, for clickable choice followup

    public SupportSubmitRequest(int userId, String topic, String messageText, Integer cityId) {
        this.userId = userId;
        this.topic = topic;
        this.messageText = messageText;
        this.cityId = cityId;
    }

    public int getUserId() { return userId; }
    public String getTopic() { return topic; }
    public String getMessageText() { return messageText; }
    public Integer getCityId() { return cityId; }
}
