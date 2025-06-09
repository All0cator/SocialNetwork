import java.io.Serializable;


public class CommandAPDU implements Serializable {
    public short ne;  // Number bytes of response expected
    public short nc;  // Number bytes of command
    public byte[] commandData;
}
