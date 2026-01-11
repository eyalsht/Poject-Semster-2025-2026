package common;

import java.util.Date;

public class SubscriptionNotification extends Notification {

    private int subscriptionId;
    private Date expiryDate;
    
    // The notification is sent 3 days before the subscription expires
    public static final int DAYS_BEFORE_EXPIRATION = 3;

    public SubscriptionNotification(int id, int userId, String message, Date date, int subscriptionId, Date expiryDate) {
        super(id, userId, message, date);
        this.subscriptionId = subscriptionId;
        this.expiryDate = expiryDate;
    }

    public int getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(int subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }
}
