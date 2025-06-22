package Messages;

import java.io.Serializable;

public class PayloadApproveComment implements Serializable {
    public String comment;
    public String photoName;
    public boolean isApproved;
}
