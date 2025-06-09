package Messages;

import java.io.Serializable;

public class PayloadDownload implements Serializable {

    public int clientID;
    public String name;  // With image extension
    public int timeout;
    public Language preferredLanguage;

    public PayloadDownload() {
        this.preferredLanguage = Language.BOTH;  // Default
    }
}
