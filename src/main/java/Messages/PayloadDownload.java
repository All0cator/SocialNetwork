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

    @Override
    public String toString() {
        return "PayloadDownload{" +
                "clientID=" + clientID +
                ", name='" + name + '\'' +
                ", timeout=" + timeout +
                ", preferredLanguage=" + preferredLanguage +
                '}';
    }
}
