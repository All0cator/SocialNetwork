package Messages;

import java.io.Serializable;

public class PayloadUserID implements Serializable {

    public int clientID;  // -1 if invalid client ID

    @Override
    public String toString() {
        return "PayloadUserID{" +
                "clientID=" + clientID +
                '}';
    }
}
