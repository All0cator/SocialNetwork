import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import Messages.Message;
import Messages.PayloadClientRequest;

public class LoggerActions implements Runnable {

    private Logger logger;

    private Socket connectionSocket;

    private ObjectOutputStream oStream;
    private ObjectInputStream iStream;

    public LoggerActions(Socket connectionSocket, Logger logger) {
        this.logger = logger;
        this.connectionSocket = connectionSocket;

        try {
            this.oStream = new ObjectOutputStream(connectionSocket.getOutputStream());
            this.iStream = new ObjectInputStream(connectionSocket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    @Override
    public void run() {
        try {
            while(!this.connectionSocket.isClosed()) {
                Message connectionMessage = (Message)iStream.readObject();

                 switch (connectionMessage.type) {
                    case REGISTER_LOGGER:
                    {
                        PayloadClientRequest pRequest = (PayloadClientRequest)connectionMessage.payload;

                        if(pRequest.clientIDSource >= 0) {
                            this.logger.server.UnRegisterOStream(pRequest.clientIDSource);
                        }
                        
                        this.logger.server.RegisterOStream(pRequest.clientIDDestination, this.oStream);
                    }
                    default:
                    break;
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