package Messages;

import java.io.Serializable;

public class PayloadClientRequest implements Serializable {
    public int clientIDSource;
    public int clientIDDestination;

    @Override
    public String toString() {
        return "PayloadClientRequest{" +
                "clientIDSource=" + clientIDSource +
                ", clientIDDestination=" + clientIDDestination +
                '}';
    }
}
