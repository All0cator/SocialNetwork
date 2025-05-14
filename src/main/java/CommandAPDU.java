import java.io.Serializable;

public class CommandAPDU implements Serializable {
    public short ne;  // number bytes of response expected
    public short nc;  // number bytes of command
    public byte[] commandData;
}
