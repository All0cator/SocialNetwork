package Messages;

import java.io.Serializable;

public class PayloadUpload implements Serializable {
    public int clientID;
    public String imageName;
    public byte[] imageData;
    public String textName;
    public String acompanyingText;
}
