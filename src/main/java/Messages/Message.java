package Messages;

import java.io.Serializable;

public class Message implements Serializable {
    public MessageType type;
    public Object payload;

    public Message() {
        
    }

    public Message(Message other) {
        this.type = other.type;
        this.payload = other.payload;
    }
}
