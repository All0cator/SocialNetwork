import java.io.Serializable;
import POD.FileData;

public class PayloadDirectory implements Serializable {
    public int clientID;
    public FileData[] fileDatas;
}
