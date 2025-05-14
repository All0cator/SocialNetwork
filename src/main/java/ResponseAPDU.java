import java.io.Serializable;

public class ResponseAPDU implements Serializable {
    public byte[] responseData;  // data
    public int sw1sw2;  // status words
}
