package Messages;

import java.io.Serializable;

import POD.Credentials;

public class PayloadCredentials implements Serializable {

    public Credentials credentials;

    @Override
    public String toString() {
        return "PayloadCredentials{" +
                "credentials=" + credentials.toString() +
                '}';
    }
}
