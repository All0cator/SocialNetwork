import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientLogger implements Runnable {
    
    private Client client;
    private int port;

    private Socket loggerConnection;
    private ObjectInputStream iStreamLog;
    private ObjectOutputStream oStreamLog;
    
    public ClientLogger(Client client) {
        this.client = client;
    }

    public synchronized void LogToServer(Object message) {
        try {
            this.oStreamLog.writeObject(message);
            this.oStreamLog.flush();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void run() {
        try {
            this.loggerConnection = new Socket("localhost", 8079);
            this.oStreamLog = new ObjectOutputStream(this.loggerConnection.getOutputStream());
            this.iStreamLog = new ObjectInputStream(this.loggerConnection.getInputStream());
        } catch (IOException e) {
            System.err.println("ERROR: Failed to connect to server: " + e.getMessage());
            throw new RuntimeException();
        }

        try {

            while(!this.loggerConnection.isClosed()) {
                String connectionMessage = (String)iStreamLog.readObject();
                
                System.out.println("LOG: " + connectionMessage);

                if(connectionMessage.contains("disapproved access permission")) {
                    this.client.ApproveAccess(false, true);
                } else if(connectionMessage.contains("approved access permission")) {
                    this.client.ApproveAccess(true, true);
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException();
        }
    }
}