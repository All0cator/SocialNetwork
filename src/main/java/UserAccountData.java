import POD.Credentials;

public class UserAccountData {
    private int ID;
    private Credentials credentials; 

    public UserAccountData() {
        ID = -1;
        this.credentials = new Credentials("", "");
    }

    public UserAccountData(int ID, Credentials credentials) {
        this.ID = ID;
        this.credentials = credentials;
    }

    public int GetID() {
        return this.ID;
    }

    public Credentials GetCredentials() {
        return this.credentials;
    }

    @Override
    public String toString() {
        return this.credentials.toString();
    }
}
