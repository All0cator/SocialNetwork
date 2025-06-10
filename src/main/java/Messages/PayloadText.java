package Messages;

import java.io.Serializable;

public class PayloadText implements Serializable {

    public String text;

    @Override
    public String toString() {
        return "PayloadText{" +
                "text='" + text + '\'' +
                '}';
    }
};
