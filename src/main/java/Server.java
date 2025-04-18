import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeSet;

import POD.Credentials;
import POD.HostData;

public class Server implements Runnable {

    private static final String serverDirectoryPath = "src/main/resources/ServerDirectory/";

    private HostData hostData;

    private ServerSocket serverSocket;

    private HashMap<String, String> userNameToPassword;
    private HashMap<String, Integer> userNameToID;
    private int userCount;
    private SocialGraph socialGraph;

    public synchronized int GetUserIDFromCredentials(Credentials credentials) {
        String password = this.userNameToPassword.get(credentials.userName);
        if(password != null) {
            if(password.equals(credentials.password)){
                return userNameToID.get(credentials.userName);
            }
        }

        return -1;
    }

    public synchronized int RegisterUser(Credentials credentials) {
        String password = this.userNameToPassword.get(credentials.userName);
        int ID = -1;
        
        if(password == null) {
            userNameToPassword.put(credentials.userName, credentials.password);
            ID = this.userCount++;
            userNameToID.put(credentials.userName, ID);
        }

        return ID;
    }

    public static void main(String[] args) {
        if(args.length != 2) return;

        new Thread(new Server(args[0], Integer.parseInt(args[1]))).start();
    }

    public Server(String hostIP, int port) {
        this.hostData = new HostData(hostIP, port);
    }

    private void LoadServerData() {
        // Load User Credentials

        userCount = 0;

        BufferedReader reader = null;
        
        String line;
        
        try {
            reader = new BufferedReader(new FileReader(Server.serverDirectoryPath + "Credentials.txt"));

            this.userNameToPassword = new HashMap<String, String>();
            this.userNameToID = new HashMap<String, Integer>();

            while((line = reader.readLine()) != null) {
                String credentials[] = line.trim().split(" ");
                
                if(credentials.length != 3) continue;
                
                this.userNameToID.put(credentials[1], Integer.parseInt(credentials[0]));
                this.userNameToPassword.put(credentials[1], credentials[2]);
                userCount++;
            }

        } catch (IOException e) {
            throw new RuntimeException();
        } finally {
            try {
                if(reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

        //for(String name : this.userNameToID.keySet()) {
        //    System.out.println(new Credentials(name, this.userNameToPassword.get(name)));
        //}

        // Load Social Graph

        reader = null;
        
        try {
            reader = new BufferedReader(new FileReader(Server.serverDirectoryPath + "SocialGraph.txt"));

            this.socialGraph = new SocialGraph();

            while((line = reader.readLine()) != null) {
                String tokens[] = line.trim().split(" ");
                
                if(tokens.length == 0) continue;
                int userID = Integer.parseInt(tokens[0]);
                int followersIDs[] = null;

                if(tokens.length > 1) {
                    followersIDs = new int[tokens.length - 1];
                    
                    for(int i = 1; i < tokens.length; ++i) {
                        followersIDs[i - 1] = Integer.parseInt(tokens[i]);
                    }
                }

                this.socialGraph.AddUser(userID, followersIDs);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        } finally {
            try {
                if(reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

        //for(int i = 0; i < userCount; ++i) {
        //    SocialGraphNode userNode = this.socialGraph.GetUserNode(i);
        //    if(userNode != null) {
        //        System.out.println(userNode);
        //    }
        //}
    }

    @Override
    public void run() {

        // Loads data into cache (memory)
        LoadServerData();

        try {
            this.serverSocket = new ServerSocket(this.hostData.port, 50, InetAddress.getByName(this.hostData.hostIP));
        } catch (UnknownHostException e) {
            throw new RuntimeException();
        } catch (IOException e) {
            throw new RuntimeException();
        }

        boolean isOpen = true;

        Socket connectionSocket;

        while(isOpen) {
            try {
                connectionSocket = this.serverSocket.accept();
            } catch (IOException e) {
                throw new RuntimeException();
            }

            new Thread(new ServerActions(connectionSocket, this)).start();
        }
    }
}