import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

import Messages.Message;
import Messages.MessageType;
import Messages.PayloadConnectionResult;
import Messages.PayloadCredentials;
import POD.Credentials;
import POD.HostData;


public class Client implements Runnable {

    private static Scanner sc = new Scanner(System.in);
    
    private HostData serverHostData;
    private HostData hostData;
    
    private int ID;
    private String clientDirectoryPath;
    
    private Socket serverConnection;
    private ObjectInputStream iStream;
    private ObjectOutputStream oStream;

    public String GetClientProfileFilePath() {
        if(this.ID == -1) return "";
        return this.clientDirectoryPath + "Profile_31client" + Integer.toString(this.ID) + ".txt";
    }

    private void PrintLoginScreenOptions() {
        System.out.println("0) Print Available Options");
        System.out.println("1) Login");
        System.out.println("2) Signup");
    }

    public static void main(String[] args) {
        if(args.length != 4) return;

        new Thread(new Client(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]))).start();
    }

    public Client(String serverHostIP, int serverPort, String hostIP, int port) {
        this.serverHostData = new HostData(serverHostIP, serverPort);
        this.hostData = new HostData(hostIP, port);
        this.ID = -1;
    }

    @Override 
    public void run() {
        System.out.println("Connecting to Server...");

        try {
            this.serverConnection = new Socket(this.serverHostData.hostIP, this.serverHostData.port);
            this.iStream = new ObjectInputStream(serverConnection.getInputStream());
            this.oStream = new ObjectOutputStream(serverConnection.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException();
        }

        boolean isRunning = true;

        PrintLoginScreenOptions();
        try {
            while(isRunning) {
                int option = Integer.parseInt(sc.nextLine());
                Message serverMessage = new Message();
                Message serverResponse;

                switch (option) {
                    case 0:
                    {
                        PrintLoginScreenOptions();
                    }
                    break;
                    case 1:
                    {
                        serverMessage.type = MessageType.LOGIN;
                        PayloadCredentials pCredentials = new PayloadCredentials();
                        serverMessage.payload = pCredentials;

                        pCredentials.credentials = new Credentials();

                        System.out.print("Username:");
                        pCredentials.credentials.userName = sc.nextLine();
                        System.out.print("Password:");
                        pCredentials.credentials.password = sc.nextLine();

                       
                        this.oStream.writeObject(serverMessage);


                        serverResponse = (Message)this.iStream.readObject();

                        int clientID = ((PayloadConnectionResult)serverResponse.payload).clientID;

                        if(clientID != -1) {
                            // valid connection
                            this.ID = clientID;
                            this.clientDirectoryPath = "src/main/resources/ClientDirectories/Client" + Integer.toString(clientID) + "/";
                            System.out.println("Welcome client " + Integer.toString(this.ID));
                        }
                        else {
                            // wrong credentials
                            System.out.println("Wrong Credentials!");
                        }
                    }
                        break;
                    case 2:
                    {
                        serverMessage.type = MessageType.REGISTER;
                        PayloadCredentials pCredentials = new PayloadCredentials();
                        serverMessage.payload = pCredentials;

                        pCredentials.credentials = new Credentials();

                        System.out.print("Username:");
                        pCredentials.credentials.userName = sc.nextLine();
                        System.out.print("Password:");
                        pCredentials.credentials.password = sc.nextLine();

                        this.oStream.writeObject(serverMessage);

                        serverResponse = (Message)this.iStream.readObject();

                        int clientID = ((PayloadConnectionResult)serverResponse.payload).clientID;

                        if(clientID != -1) {
                            // valid connection
                            this.ID = clientID;
                            this.clientDirectoryPath = "src/main/resources/ClientDirectories/Client" + Integer.toString(clientID) + "/";
                            System.out.println("Welcome client " + Integer.toString(this.ID));
                        }
                        else {
                            System.out.println("User already exists with that Name!");
                        }
                    }
                        break;
                    default:
                        break;
                }
            }

        } catch (IOException e) {
            throw new RuntimeException();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException();
        }
    }
}
