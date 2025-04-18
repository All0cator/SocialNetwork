import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PKCS12Attribute;
import java.util.ArrayList;

import Messages.Message;
import Messages.MessageType;
import Messages.PayloadConnectionResult;
import Messages.PayloadCredentials;

public class ServerActions implements Runnable {

    private Socket connectionSocket;
    private Server server;

    private ObjectOutputStream oStream;
    private ObjectInputStream iStream;

    public ServerActions(Socket connectionSocket, Server server) {
        this.connectionSocket = connectionSocket;
        this.server = server;

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
                    case MessageType.LOGIN:
                    {
                        PayloadCredentials pCredentials = (PayloadCredentials)connectionMessage.payload;

                        int ID = this.server.GetUserIDFromCredentials(pCredentials.credentials);
                        
                        Message loginResponse = new Message();
                        loginResponse.type = MessageType.LOGIN;
                        PayloadConnectionResult pResult = new PayloadConnectionResult();
                        loginResponse.payload = pResult;
                        pResult.clientID = ID;

                        oStream.writeObject(loginResponse);

                    }    
                    break;
                    case MessageType.REGISTER:
                    {
                        PayloadCredentials pCredentials = (PayloadCredentials)connectionMessage.payload;

                        int ID = this.server.RegisterUser(pCredentials.credentials);
                        
                        Message registerResponse = new Message();
                        registerResponse.type = MessageType.REGISTER;
                        PayloadConnectionResult pResult = new PayloadConnectionResult();
                        registerResponse.payload = pResult;
                        pResult.clientID = ID;

                        oStream.writeObject(registerResponse);
                    }
                    break;
                    default:
                    break;
                }
            }
        } catch(IOException e) {
            throw new RuntimeException();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException();
        }
    }
}
