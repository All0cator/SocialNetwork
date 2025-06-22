package Messages;

import java.io.Serializable;

public class PayloadUpload implements Serializable {

    public int clientID;
    public String imageName;
    public byte[] imageData;
    public String textName;
    public String acompanyingText;
    public MultiLingualText multiLingualText;

    public PayloadUpload() {
        this.multiLingualText = new MultiLingualText();
    }

    /*@Override
    public String toString() {
        return "PayloadUpload{" +
                "clientID=" + clientID +
                ", imageName='" + imageName + '\'' +
                ", textName='" + textName + '\'' +
                ", multilingualText=" + multilingualText +
                '}';
    }*/
}
