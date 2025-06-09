import java.io.Serializable;


public class ResponseAPDU implements Serializable {
    public byte[] responseData;  // Data
    public int sw1sw2;  // Status words
}
