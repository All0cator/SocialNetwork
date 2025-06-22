package Messages;

import java.io.Serializable;

public class PayloadComment implements Serializable {
    public MultiLingualText comment;
    public String photoName;
    public boolean isApproved;
}