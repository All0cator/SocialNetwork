package Messages;

import java.io.Serializable;

public class Message implements Serializable {
    public MessageType type;
    public Object payload;
}
