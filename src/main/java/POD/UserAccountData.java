package POD;

import java.io.Serializable;

/**
 * UserAccountData class represents a user account with an ID and a username.
 * It implements Serializable to allow instances to be serialized for storage or transmission.
 */
public class UserAccountData implements Serializable {
    public int ID;
    public String userName; 

    public UserAccountData() {
        ID = -1;
        this.userName = "";
    }

    public UserAccountData(int ID, String userName) {
        this.ID = ID;
        this.userName = userName;
    }

    @Override
    public String toString() {
        return String.format("{ID: %d, UserName: %s}", this.ID, this.userName);
    }
}
