package Messages;

import java.io.Serializable;

public class PayloadDownload implements Serializable {
    public int clientID;
    public String name; // with image extension
    public int timeout;
}
