package common;

import java.io.Serializable;
import java.util.Date;

public abstract class Notification implements Serializable {
    private int id;
    private int userId;
    private String message;
    private boolean isRead;
    private Date date;

    public Notification(int id, int userId, String message, Date date) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
