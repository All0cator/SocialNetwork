import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

import POD.Credentials;
import POD.HostData;


public class Server implements Runnable {

    private static final String serverDirectoryPath = String.join("/", "src", "main", "resources", "ServerDirectory", "");
    private ConcurrentHashMap<Integer, Directory> IDtoDirectory;  // Cache of user directories

    public HostData hostData;

    private ServerSocket serverSocket;

    private ConcurrentHashMap<String, Integer> photoPathToDownloadCounter;

    private ConcurrentHashMap<Integer, ObjectOutputStream> clientIDToOStreamLogger;

    private ConcurrentHashMap<String, String> userNameToPassword;
    private ConcurrentHashMap<String, Integer> userNameToID;
    private ConcurrentHashMap<Integer, String> IDToUserName;

    private int userCount;
    private SocialGraph socialGraph;

    public synchronized void Log(int clientID, String message) {
        ObjectOutputStream oS = clientIDToOStreamLogger.get(clientID);

        if(oS != null) {
            try {
                oS.writeObject(message);
                oS.flush();
            } catch (IOException e) {
                throw new RuntimeException();
            }
        } else {
            System.out.println("Logger not found for clientID: " + Integer.toString(clientID));
        }
    }

    public synchronized void UnRegisterOStream(int clientID) {
        this.clientIDToOStreamLogger.remove(clientID);
    }

    public synchronized void RegisterOStream(int clientID, ObjectOutputStream oS) {
        this.clientIDToOStreamLogger.put(clientID, oS);
    }

    // avoid stale reads with synchronized
    public synchronized Directory GetDirectory(int userID) throws IOException {
        Directory result = this.IDtoDirectory.get(userID);

        if (result == null) {
            if (this.socialGraph.GetUserNode(userID) == null) return null;

            String clientDirectoryPath = Server.serverDirectoryPath + "ClientProfiles" + "/" + "Client" + Integer.toString(userID) + "/";
            try {
                result = new Directory(clientDirectoryPath, userID);
                this.IDtoDirectory.put(userID, result);
            } catch (IOException e) {
                System.err.println("Failed to create directory for user " + userID + ": " + e.getMessage());
                return null;
            }
        }

        return result;
    }

    public synchronized int GetUserIDFromCredentials(Credentials credentials) {
        // Convert username to lowercase for case-insensitive comparison
        String userName = credentials.userName.toLowerCase();
        String password = this.userNameToPassword.get(userName);
        if (password != null) {
            if (password.equals(credentials.password)){
                return userNameToID.get(userName);
            }
        }

        return -1;
    }

    public SocialGraph GetSocialGraph() {
        return this.socialGraph;
    }

    public String GetServerDirectoryPath() {
        return Server.serverDirectoryPath;
    }

    public int GetUserCount() {
        return this.userCount;
    }

    public String GetUserName(int userID) {
        return this.IDToUserName.get(userID);
    }

    public synchronized int RegisterUser(Credentials credentials) throws IOException {
        // Convert username to lowercase for case-insensitive comparison
        String userName = credentials.userName.toLowerCase();
        String password = this.userNameToPassword.get(userName);

        if (password != null) {
            return -1;  // Username already exists
        }

        // Use userCount as the new user ID
        int newUserID = this.userCount;
        
        try {
            // Add user to in-memory maps
            this.userNameToPassword.put(userName, credentials.password);
            this.userNameToID.put(userName, newUserID);
            this.IDToUserName.put(newUserID, userName);

            // Create directory path for new user
            String newUserDirectoryPath = Server.serverDirectoryPath + "ClientProfiles" + "/" + "Client" + newUserID + "/";

            // Create the directory structure
            java.io.File dirFile = new java.io.File(newUserDirectoryPath);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }

            // Create and cache the Directory object
            Directory newUserDirectory = new Directory(newUserDirectoryPath, newUserID);
            this.IDtoDirectory.put(newUserID, newUserDirectory);

            // Create initial profile file
            createInitialProfileFile(newUserDirectory, userName, credentials.password);

            // Create initial notifications file
            createInitialNotificationsFile(newUserDirectory);

            // Add user to social graph
            this.socialGraph.AddUser(newUserID, null);

            // Save credentials to Credentials.txt
            saveCredentialsToFile(newUserID, userName, credentials.password);

            // Update SocialGraph.txt
            updateSocialGraphFile();

            // Increment user count for next user
            this.userCount++;

            System.out.println("New user registered: ID=" + newUserID + ", Username=" + userName);

            return newUserID;
        } catch (Exception e) {
            // Rollback changes if anything fails
            this.userNameToPassword.remove(userName);
            this.userNameToID.remove(userName);
            this.IDToUserName.remove(newUserID);
            this.IDtoDirectory.remove(newUserID);

            System.err.println("Failed to register user " + userName + ": " + e.getMessage());
            throw new IOException("User registration failed", e);
        }
    }

    // Helper method to create initial profile file
    private void createInitialProfileFile(Directory userDirectory, String userName, String password) {
        try {
            String profileFileName = userDirectory.GetLocalProfileName();
            userDirectory.SetFile(profileFileName);
            userDirectory.GetFile(profileFileName).WriteFile("", null);
        } catch (Exception e) {
            System.err.println("Failed to create profile file for user " + userName + ": " + e.getMessage());
        }
    }

    // Helper method to create initial notifications file
    private void createInitialNotificationsFile(Directory userDirectory) {
        try {
            String notificationsFileName = userDirectory.GetLocalNotificationsName();
            userDirectory.SetFile(notificationsFileName);
            userDirectory.GetFile(notificationsFileName).WriteFile("", null);
        } catch (Exception e) {
            System.err.println("Failed to create notifications file: " + e.getMessage());
        }
    }

    // Helper method to save credentials to file
    private void saveCredentialsToFile(int userID, String userName, String password) throws IOException {
        FileWriter fWriter = new FileWriter(Server.serverDirectoryPath + "Credentials.txt", true);
        try {
            fWriter.write(userID + " " + userName + " " + password + "\n");
        } finally {
            fWriter.close();
        }
    }

    // Helper method to update SocialGraph.txt
    public void updateSocialGraphFile() {
        try {
            StringBuilder graphContent = new StringBuilder();

            // Write all users in the social graph
            for (int userID = 0; userID < this.userCount + 1; userID++) {
                SocialGraphNode userNode = this.socialGraph.GetUserNode(userID);
                if (userNode != null) {
                    graphContent.append(userID);

                    // Add follower IDs
                    java.util.Set<Integer> followerIDs = userNode.GetFollowerIDs();
                    for (Integer followerID : followerIDs) {
                        graphContent.append(" ").append(followerID);
                    }

                    if (userID < this.userCount) {
                        graphContent.append("\n");
                    }
                }
            }

            // Write to SocialGraph.txt file
            FileWriter writer = new FileWriter(Server.serverDirectoryPath + "SocialGraph.txt");
            try {
                writer.write(graphContent.toString());
            } finally {
                writer.close();
            }

            System.out.println("SocialGraph.txt updated with new user");

        } catch (Exception e) {
            System.err.println("Error updating SocialGraph.txt: " + e.getMessage());
        }
    }

    public synchronized void AddDownload(String photoPath) {
        if(this.photoPathToDownloadCounter.get(photoPath) != null) {
            this.photoPathToDownloadCounter.put(photoPath, this.photoPathToDownloadCounter.get(photoPath) + 1);
        } else {
            this.photoPathToDownloadCounter.put(photoPath, 1);
        }
    }

    public synchronized void PrintStatistics() {

        if(this.photoPathToDownloadCounter.size() == 0) {
            System.out.println("No Statistics!");
            return;
        }

        Integer[] a = new Integer[this.photoPathToDownloadCounter.size()];
        String[] aa = new String[this.photoPathToDownloadCounter.size()];

        int i = 0;
        for(Map.Entry<String, Integer> c : this.photoPathToDownloadCounter.entrySet()) {
            a[i] = c.getValue();
            aa[i] = c.getKey();
            i++;
        }
        
        // insertion sort
        for(int j = 0; j < a.length; ++j) {
            String b = aa[j];
            int idx = j;

            for(int k = j + 1; k < a.length; ++k) {
                String c = aa[k];
                
                if(c.compareTo(b) > 0) {
                    idx = k;
                    b = c;
                }
            }

            // swap
            String t1 = aa[j];
            aa[j] = aa[idx];
            aa[idx] = t1;

            Integer t2 = a[j];
            a[j] = a[idx];
            a[idx] = t2;
        }

        for(int j = 0; j < a.length; ++j) {
            System.out.printf("%d) PhotoPath: %s, Counter: %d\n", j, aa[j], a[j]);
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) return;

        Server serv = new Server(args[0], Integer.parseInt(args[1]));

        new Thread(serv).start();

        new Thread(new Logger(serv)).start();
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

            this.userNameToPassword = new ConcurrentHashMap<String, String>();
            this.userNameToID = new ConcurrentHashMap<String, Integer>();
            this.IDToUserName = new ConcurrentHashMap<Integer, String>();
            this.IDtoDirectory = new ConcurrentHashMap<Integer, Directory>();

            this.clientIDToOStreamLogger = new ConcurrentHashMap<Integer, ObjectOutputStream>();

            this.photoPathToDownloadCounter = new ConcurrentHashMap<String, Integer>();

            while((line = reader.readLine()) != null) {
                String credentials[] = line.trim().split(" ");

                if (credentials.length != 3) continue;

                this.userNameToID.put(credentials[1], Integer.parseInt(credentials[0]));
                this.IDToUserName.put(Integer.parseInt(credentials[0]), credentials[1]);
                this.userNameToPassword.put(credentials[1], credentials[2]);
                this.IDtoDirectory.put((Integer.parseInt(credentials[0])), new Directory(Server.serverDirectoryPath + "ClientProfiles" + "/" + "Client" + credentials[0] + "/", Integer.parseInt(credentials[0])));

                userCount++;
            }

        } catch (IOException e) {
            throw new RuntimeException();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

        // for (String name : this.userNameToID.keySet()) {
        //     System.out.println(new Credentials(name, this.userNameToPassword.get(name)));
        // }

        // Load Social Graph
        reader = null;
        try {
            reader = new BufferedReader(new FileReader(Server.serverDirectoryPath + "SocialGraph.txt"));

            this.socialGraph = new SocialGraph();

            while ((line = reader.readLine()) != null) {
                String tokens[] = line.trim().split(" ");

                if (tokens.length == 0) continue;
                int userID = Integer.parseInt(tokens[0]);
                int followersIDs[] = null;

                if (tokens.length > 1) {
                    followersIDs = new int[tokens.length - 1];
                    
                    for (int i = 1; i < tokens.length; ++i) {
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
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }

        // for (int i = 0; i < userCount; ++i) {
        //     SocialGraphNode userNode = this.socialGraph.GetUserNode(i);
        //     if(userNode != null) {
        //         System.out.println(userNode);
        //     }
        // }
    }

    public static boolean isOpen;

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

        Server.isOpen = true;

        new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            while(true) {
                String o = sc.nextLine();
                if(o.equals("a")) {
                    System.out.println("\nShutting down server...");
                    PrintStatistics();
                    Server.isOpen = false;
                    System.exit(0);
                }
            }
            
        }).start();

        Socket connectionSocket;
        while (Server.isOpen) {
            try {
                connectionSocket = this.serverSocket.accept();
            } catch (IOException e) {
                throw new RuntimeException();
            }
            
            new Thread(new ServerActions(connectionSocket, this)).start();
        }
    }
}
