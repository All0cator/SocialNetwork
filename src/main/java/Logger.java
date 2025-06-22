import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Logger implements Runnable {

    public Server server;

    private ServerSocket serverLoggerSocket;

    public Logger(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        try {
            this.serverLoggerSocket = new ServerSocket(8079, 50, InetAddress.getByName(this.server.hostData.hostIP));
        } catch (UnknownHostException e) {
            throw new RuntimeException();
        } catch (IOException e) {
            throw new RuntimeException();
        }

        boolean isOpen = true;
        Socket connectionSocket;
        while (isOpen) {
            try {
                connectionSocket = this.serverLoggerSocket.accept();
            } catch (IOException e) {
                throw new RuntimeException();
            }

            new Thread(new LoggerActions(connectionSocket, this)).start();
        }
    }
}