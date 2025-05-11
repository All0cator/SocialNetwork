package Messages;

import java.io.Serializable;
import java.util.ArrayList;

import POD.UserAccountData;

public class PayloadClientGraph implements Serializable {
    public ArrayList<UserAccountData> followers;
    public ArrayList<UserAccountData> followings;
}
