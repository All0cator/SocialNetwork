package Messages;

import java.io.Serializable;

public class PayloadConnectionResult implements Serializable {
    public int clientID; // is -1 when connection failed else is a valid client ID
}
