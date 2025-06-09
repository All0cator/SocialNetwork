package POD;

public class HostData {

    public String hostIP;
    public int port;

    public HostData(String hostIP, int port) {
        this.hostIP = hostIP;
        this.port = port;
    }

    @Override
    public String toString() {
        return String.format("{HostIP: %s, Port: %d}", this.hostIP, this.port);
    }
}
