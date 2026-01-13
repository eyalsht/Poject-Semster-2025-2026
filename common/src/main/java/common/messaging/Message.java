package common.messaging;

import common.enums.ActionType;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private ActionType action;
    private Object message;

    public Message(ActionType action, Object message){
        this.action = action;
        this.message = message;
    }
    public Message(ActionType action) {
        this.action = action;
        this.message = action;
    }
    public ActionType getAction() {
        return action;
    }

    public void setAction(ActionType action) {
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

