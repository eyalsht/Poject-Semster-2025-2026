package common;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private actionType action;
    private Object message;

    public Message(actionType action, Object message){
        this.action = action;
        this.message = message;
    }
    public Message(actionType action) {
        this.action = action;
        this.message = action;
    }
    public actionType getAction() {
        return action;
    }

    public void setAction(actionType action) {
        this.action = action;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Message [action=" + action + ", message=" + message + "]";
    }
}

