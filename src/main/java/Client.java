import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import javax.imageio.ImageIO;

import java.io.File;

import Messages.Message;
import Messages.MessageType;
import Messages.PayloadClientGraph;
import Messages.PayloadClientRequest;
import Messages.PayloadCredentials;
import Messages.PayloadDownload;
import Messages.PayloadText;
import Messages.PayloadUpload;
import Messages.PayloadUserID;
import POD.Credentials;
import POD.HostData;
import POD.UserAccountData;


public class Client implements Runnable {

    private static Scanner sc = new Scanner(System.in);
    private volatile boolean isShuttingDown = false;

    private Directory testDirectory;
    private Directory clientDirectory;

    // Cache of Client from server refreshes when connecting to server and when refreshing option selected
    private ArrayList<UserAccountData> followerDatas;
    private ArrayList<UserAccountData> followingDatas;
    private ArrayList<String> notifications;

    private HostData serverHostData;
    // private HostData hostData;

    private int ID;
    private String clientDirectoryPath;

    private Socket serverConnection;
    private ObjectInputStream iStream;
    private ObjectOutputStream oStream;

    public String GetClientProfileFilePath() {
        if (this.ID == -1) return "";
        return this.clientDirectoryPath + "Profile_31client" + Integer.toString(this.ID) + ".txt";
    }

    public String GetClientNotificationsFilePath() {
        if (this.ID == -1) return "";
        return this.clientDirectoryPath + "Others_31client" + Integer.toString(this.ID) + ".txt";
    }

    private void PrintLoginScreenOptions() {
        System.out.println("\n+-------------------------------+");
        System.out.println("|      SOCIAL NETWORK LOGIN     |");
        System.out.println("|-------------------------------|");
        System.out.println("| 0) Print Available Options    |");
        System.out.println("| 1) Login                      |");
        System.out.println("| 2) Signup                     |");
        System.out.println("+-------------------------------+");
        System.out.println("\n+-------------------------------+");
        System.out.println("|      SOCIAL NETWORK LOGIN     |");
        System.out.println("|-------------------------------|");
        System.out.println("| 0) Print Available Options    |");
        System.out.println("| 1) Login                      |");
        System.out.println("| 2) Signup                     |");
        System.out.println("+-------------------------------+");
    }

    private void PrintUserOptions() {
        System.out.println("\n+-------------------------------+");
        System.out.println("|         USER MENU             |");
        System.out.println("|-------------------------------|");
        System.out.println("| 0) Print Available Options    |");
        System.out.println("| 1) Logout                     |");
        System.out.println("| 2) Accept Follow Request      |");
        System.out.println("| 3) Follow Request             |");
        System.out.println("| 4) Unfollow                   |");
        System.out.println("| 5) View Notifications (" + this.notifications.size() + ")     |");
        System.out.println("| 6) Access Profile             |");
        System.out.println("| 7) Upload                     |");
        System.out.println("| 8) Download Photo             |");
        System.out.println("| 9) Refresh                    |");
        System.out.println("+-------------------------------+");
        System.out.println("\n+-------------------------------+");
        System.out.println("|         USER MENU             |");
        System.out.println("|-------------------------------|");
        System.out.println("| 0) Print Available Options    |");
        System.out.println("| 1) Logout                     |");
        System.out.println("| 2) Accept Follow Request      |");
        System.out.println("| 3) Follow Request             |");
        System.out.println("| 4) Unfollow                   |");
        System.out.println("| 5) View Notifications (" + this.notifications.size() + ")     |");
        System.out.println("| 6) Access Profile             |");
        System.out.println("| 7) Upload                     |");
        System.out.println("| 8) Download Photo             |");
        System.out.println("| 9) Refresh                    |");
        System.out.println("+-------------------------------+");
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
        if(args.length != 4) return;

        new Thread(new Client(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]))).start();
    }

    public Client(String serverHostIP, int serverPort, String hostIP, int port) throws IOException {
        this.serverHostData = new HostData(serverHostIP, serverPort);
        // this.hostData = new HostData(hostIP, port);
        this.ID = -1;
        this.followerDatas = new ArrayList<UserAccountData>();
        this.followingDatas = new ArrayList<UserAccountData>();
        this.notifications = new ArrayList<String>();
        this.testDirectory = new Directory("src/main/resources/ClientDirectories/", -1);
    }

    @Override 
    public void run() {
        System.out.println("Connecting to Server...");

        try {
            this.serverConnection = new Socket(this.serverHostData.hostIP, this.serverHostData.port);
            this.iStream = new ObjectInputStream(serverConnection.getInputStream());
            this.oStream = new ObjectOutputStream(serverConnection.getOutputStream());

            // Shutdown hook for graceful cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down client...");
                cleanup();
            }));
        } catch (IOException e) {
            cleanup();
            throw new RuntimeException();
        }

        boolean isRunning = true;
        PrintLoginScreenOptions();
        try {
            while (isRunning && !isShuttingDown) {
                int option;
                String input = sc.nextLine().trim();
                try {
                    option = input.isEmpty() ? 0 : Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a number (0-2).");
                    PrintLoginScreenOptions();
                    continue;
                }

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
                        System.out.println("\n----------- LOGIN -----------");
                        serverMessage.type = MessageType.LOGIN;
                        PayloadCredentials pCredentials = new PayloadCredentials();
                        serverMessage.payload = pCredentials;

                        pCredentials.credentials = new Credentials();

                        System.out.print("Username: ");
                        pCredentials.credentials.userName = sc.nextLine();
                        System.out.print("Password: ");
                        pCredentials.credentials.password = sc.nextLine();
                        System.out.println("-------------------------------");

                        System.out.println("Authenticating...");
                        this.oStream.writeObject(serverMessage);
                        this.oStream.flush();

                        serverResponse = (Message)this.iStream.readObject();

                        int clientID = ((PayloadUserID)serverResponse.payload).clientID;
                        if (clientID != -1) {
                            // Valid connection
                            this.ID = clientID;
                            this.clientDirectoryPath = "src/main/resources/ClientDirectories/Client" + Integer.toString(clientID) + "/";
                            this.clientDirectory = new Directory(this.clientDirectoryPath, clientID);
                            System.out.println("✓ Login successful! Welcome client " + Integer.toString(this.ID));

                            EnterApplication();
                        } else {
                            // Wrong credentials
                            System.out.println("✗ Login failed: Incorrect username or password");
                        }
                    }
                    break;
                    case 2:
                    {
                        System.out.println("\n----------- SIGNUP -----------");
                        serverMessage.type = MessageType.SIGNUP;
                        PayloadCredentials pCredentials = new PayloadCredentials();
                        serverMessage.payload = pCredentials;

                        pCredentials.credentials = new Credentials();

                        System.out.print("Username: ");
                        pCredentials.credentials.userName = sc.nextLine();
                        System.out.print("Password: ");
                        pCredentials.credentials.password = sc.nextLine();
                        System.out.println("-------------------------------");

                        System.out.println("Creating account...");
                        this.oStream.writeObject(serverMessage);
                        this.oStream.flush();

                        serverResponse = (Message)this.iStream.readObject();

                        int clientID = ((PayloadUserID)serverResponse.payload).clientID;

                        if (clientID != -1) {
                            // Valid connection
                            this.ID = clientID;
                            this.clientDirectoryPath = "src/main/resources/ClientDirectories/Client" + Integer.toString(clientID) + "/";
                            this.clientDirectory = new Directory(this.clientDirectoryPath, clientID);
                            System.out.println("✓ Account created successfully! Welcome client " + Integer.toString(this.ID));

                            EnterApplication();
                        } else {
                            System.out.println("✗ Signup failed: Username already exists");
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

    public void EnterApplication() {
        PrintUserOptions();

        boolean isRunning = true;
        try {
            while (isRunning) {
                int option;
                String input = sc.nextLine().trim();
                try {
                    option = input.isEmpty() ? 0 : Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a number (0-9).");
                    PrintUserOptions();
                    continue;
                }

                Message serverMessage = new Message();
                Message serverResponse;

                switch (option) {
                    case 0:
                    {
                        PrintUserOptions();
                    }
                    break;
                    case 1:
                    {
                        PrintLoginScreenOptions();
                        return;
                    }
                    case 2:
                    {
                        // Accept Follow Request
                        System.out.println("\n----------- FOLLOW REQUESTS -----------");
                        ArrayList<String> followRequests = new ArrayList<String>();

                        for (int i = 0; i < notifications.size(); ++i) {
                            if (notifications.get(i).contains("follow request")) {
                                followRequests.add(notifications.get(i));
                            }
                        }

                        if (followRequests.size() == 0) {
                            System.out.println("You have no pending follow requests.");
                            System.out.println("---------------------------------------");
                            break;
                        }

                        System.out.println("Pending follow requests:");
                        for (int i = 0; i < followRequests.size(); ++i) {
                            System.out.println(Integer.toString(i) + ") " + followRequests.get(i));
                        }

                        System.out.print("Choose a request to accept(-1 to go back): ");
                        int option2;
                        try {
                            option2 = Integer.parseInt(sc.nextLine());
                            if (option2 < -1 || option2 >= followRequests.size()) {
                                System.out.println("Invalid selection.");
                                System.out.println("---------------------------------------");
                                break;
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a number.");
                            System.out.println("---------------------------------------");
                            break;
                        }

                        if (option2 == -1) {
                            System.out.println("---------------------------------------");
                            break;
                        }

                        String[] followRequestTokens = followRequests.get(option2).split("\\s+");
                        int followerID = Integer.parseInt(followRequestTokens[0]);

                        System.out.println("\nOptions:");
                        System.out.println("0) Accept");
                        System.out.println("1) Reject");
                        System.out.println("2) Accept and follow back");
                        System.out.print("Choose an option(-1 to cancel): ");

                        int option3;
                        try {
                            option3 = Integer.parseInt(sc.nextLine());
                            if (option3 < -1 || option3 > 2) {
                                System.out.println("Invalid selection.");
                                System.out.println("---------------------------------------");
                                break;
                            }
                        } catch(NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a number.");
                            System.out.println("---------------------------------------");
                            break;
                        }

                        if (option3 == -1) {
                            System.out.println("---------------------------------------");
                            break;
                        }

                        Message requestMessage = new Message();

                        if (option3 == 0) {
                            requestMessage.type = MessageType.FOLLOW_REQUEST_ACCEPT;
                            System.out.println("Accepting follow request...");
                        } else if (option3 == 1) {
                            requestMessage.type = MessageType.FOLLOW_REQUEST_REJECT;
                            System.out.println("Rejecting follow request...");
                        } else {
                            // We are accepting and sending another request
                            requestMessage.type = MessageType.FOLLOW_REQUEST_ACCEPT;
                            System.out.println("Accepting follow request and following back...");
                        }

                        PayloadClientRequest pRequest = new PayloadClientRequest();
                        requestMessage.payload = pRequest;

                        pRequest.clientIDSource = this.ID;
                        pRequest.clientIDDestination  = followerID;

                        Set<String> removeLines = new HashSet<String>();
                        removeLines.add(String.format("%d follow request", followerID));

                        this.clientDirectory.GetFile(this.clientDirectory.GetLocalNotificationsName()).RemoveFile(removeLines);
                        this.oStream.writeObject(requestMessage);
                        this.oStream.flush();

                        if (option3 == 2) {
                            requestMessage.type = MessageType.FOLLOW_REQUEST_ACCEPT;

                            Message followbackMessage = new Message();
                            followbackMessage.type = MessageType.FOLLOW_REQUEST;
                            PayloadClientRequest pRequest2 = new PayloadClientRequest();
                            followbackMessage.payload = pRequest2;

                            pRequest2.clientIDSource = this.ID;
                            pRequest2.clientIDDestination = followerID;

                            this.oStream.writeObject(followbackMessage);
                            this.oStream.flush();
                        }

                        System.out.println("✓ Request processed successfully!");
                        System.out.println("---------------------------------------");
                    }
                    break;
                    case 3:
                    {
                        // Follow Request
                        System.out.println("\n------------ FOLLOW REQUEST ------------");

                        Message followMessage = new Message();
                        followMessage.type = MessageType.FOLLOW_REQUEST;
                        PayloadClientRequest  pRequest = new PayloadClientRequest();
                        followMessage.payload = pRequest;
                        pRequest.clientIDSource = this.ID;

                        System.out.print("Input User ID to follow(-1 to go back): ");
                        int followUserID;

                        try {
                            String inputFollow = sc.nextLine().trim();
                            followUserID = inputFollow.isEmpty() ? -1 : Integer.parseInt(inputFollow);

                            if (followUserID < -1) {
                                System.out.println("✗ Invalid user ID. Please enter a non-negative number.");
                                System.out.println("---------------------------------------");
                                break;
                            }
                        } catch(NumberFormatException e) {
                            System.out.println("✗ Invalid input. Please enter a number.");
                            System.out.println("---------------------------------------");
                            break;
                        }

                        if (followUserID == -1) {
                            System.out.println("Follow request cancelled.");
                            System.out.println("---------------------------------------");
                            break;
                        }

                        if (followUserID == this.ID) {
                            System.out.println("✗ You cannot follow yourself.");
                            System.out.println("---------------------------------------");
                            break;
                        }

                        pRequest.clientIDDestination = followUserID;

                        System.out.println("Sending follow request to user ID " + followUserID + "...");
                        this.oStream.writeObject(followMessage);
                        this.oStream.flush();

                        System.out.println("✓ Follow request sent successfully!");
                        System.out.println("---------------------------------------");
                    }
                    break;
                    case 4:
                    {
                        // Unfollow
                        System.out.println("\n--------------- UNFOLLOW ---------------");

                        if (this.followingDatas.size() == 0) {
                            System.out.println("You are not following anyone.");
                            System.out.println("---------------------------------------");
                            break;
                        }

                        System.out.println("Users you follow:");
                        for(int i = 0; i < this.followingDatas.size(); ++i) {
                            System.out.println(Integer.toString(i) + ") " + this.followingDatas.get(i));
                        }

                        System.out.print("Choose a user to unfollow(-1 to go back): ");
                        int option2;

                        try {
                            String inputUnfollow = sc.nextLine().trim();
                            option2 = inputUnfollow.isEmpty() ? -1 : Integer.parseInt(inputUnfollow);

                            if (option2 < -1 || option2 >= this.followingDatas.size()) {
                                System.out.println("✗ Invalid selection.");
                                System.out.println("---------------------------------------");
                                break;
                            }
                        } catch(NumberFormatException e) {
                            System.out.println("✗ Invalid input. Please enter a number.");
                            System.out.println("---------------------------------------");
                            break;
                        }

                        if (option2 == -1) {
                            System.out.println("Unfollow cancelled.");
                            System.out.println("---------------------------------------");
                            break;
                        }

                        int unfollowUserID = this.followingDatas.get(option2).ID;
                        String unfollowUsername = this.followingDatas.get(option2).toString();

                        System.out.println("Unfollowing " + unfollowUsername + "...");

                        // Create a new message to avoid serialization caching issues
                        serverMessage.type = MessageType.UNFOLLOW;
                        PayloadClientRequest pRequest = new PayloadClientRequest();
                        serverMessage.payload = pRequest;
                        pRequest.clientIDSource = this.ID;
                        pRequest.clientIDDestination = unfollowUserID;

                        this.oStream.writeObject(serverMessage);
                        this.oStream.flush();

                        // Process server response
                        serverResponse = (Message)this.iStream.readObject();
                        PayloadClientGraph pGraph = (PayloadClientGraph)serverResponse.payload;

                        // Update local following data
                        this.followingDatas.clear();
                        this.followingDatas.addAll(pGraph.followings);

                        for (int i = 0; i < pGraph.followings.size(); ++i) {
                            System.out.println(Integer.toString(i) + ") " + pGraph.followings.get(i));
                        }

                        System.out.println("✓ You have unfollowed " + unfollowUsername);
                        System.out.println("---------------------------------------");
                    }
                    break;
                    case 5:
                    {
                        // View notifications
                        System.out.println("\n------------ NOTIFICATIONS ------------");

                        if (this.notifications.size() == 0) {
                            System.out.println("No notifications");
                        } else {
                            for (int i = 0; i < this.notifications.size(); ++i) {
                                System.out.println(" • " + this.notifications.get(i));
                            }
                        }

                        System.out.println("---------------------------------------");
                    }
                    break;
                    case 6:
                    {
                        // Access Profile
                        System.out.println("\n----------- PROFILE ACCESS -----------");

                        if (this.followingDatas.size() == 0) {
                            System.out.println("You are not following anyone.");
                            System.out.println("---------------------------------------");
                            break;
                        }

                        System.out.println("Users you follow:");
                        for (int i = 0; i < this.followingDatas.size(); ++i) {
                            System.out.printf(" %d) %s\n", i, this.followingDatas.get(i));
                        }

                        System.out.print("\nSelect user to view profile (-1 to cancel): ");
                        int choice2;
                        try {
                            choice2 = Integer.parseInt(sc.nextLine());
                            if (choice2 < -1 || choice2 >= this.followingDatas.size()) {
                                System.out.println("Invalid selection.");
                                System.out.println("---------------------------------------");
                                break;
                            }
                        } catch(NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a number.");
                            System.out.println("---------------------------------------");
                            break;
                        }

                        if (choice2 == -1) {
                            System.out.println("---------------------------------------");
                            break;
                        }

                        // Send access profile request with PayloadcClientRequest
                        Message requestMessage = new Message();
                        requestMessage.type = MessageType.ACCESS_PROFILE;
                        PayloadClientRequest pRequest = new PayloadClientRequest();
                        requestMessage.payload = pRequest;
                        pRequest.clientIDSource = this.ID;

                        for (int i = 0; i < this.followingDatas.size(); ++i) {
                            System.out.printf("%d) %s\n", i, this.followingDatas.get(i));
                        }

                        System.out.println("Input Client ID to view Profile: ");
                        int profileChoice;  // Changed from 'choice2' to 'profileChoice'
                        try {
                            profileChoice = Integer.parseInt(sc.nextLine());
                            if(profileChoice < -1 || profileChoice >= this.followingDatas.size()) {
                                System.out.println("Invalid selection.");
                                System.out.println("---------------------------------------");
                                break;
                            }
                        } catch(NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a number.");
                            System.out.println("---------------------------------------");
                            break;
                        }

                        if (profileChoice == -1 || profileChoice == this.ID) {
                            System.out.println("---------------------------------------");
                            break;
                        }

                        pRequest.clientIDDestination = this.followingDatas.get(profileChoice).ID;

                        this.oStream.writeObject(requestMessage);
                        this.oStream.flush();

                        // Receive answer either "Access Profile Denied Reason: not following specified client" or
                        // Receive text with Profile_31ClientID of user with clientID = ID then print it
                        PayloadText pText = (PayloadText)this.iStream.readObject();

                        System.out.println(pText.text);
                        System.out.println("---------------------------------------");
                    }
                    break;
                    case 7:
                    {
                        // Upload TestImage on local repository
                        this.clientDirectory.SetFile("TestImage.png");
                        this.clientDirectory.SetFile("TestImage.txt");

                        _File imageFileTest = this.testDirectory.GetFile("TestImage.png");
                        _File textFileTest = this.testDirectory.GetFile("TestImage.txt");

                        _File imageFile = this.clientDirectory.GetFile("TestImage.png");
                        _File textFile = this.clientDirectory.GetFile("TestImage.txt");

                        imageFile.WriteFile(imageFileTest.ReadFile());
                        textFile.WriteFile(textFileTest.ReadFile());

                        ArrayList<String> appendList = new ArrayList<String>();
                        appendList.add(String.format("%d posted %s", this.ID, "TestImage.png"));

                        this.clientDirectory.GetFile(this.clientDirectory.GetLocalProfileName()).AppendFile(appendList);

                        // Upload TestImage on remote repository
                        Message synchronizationMessage = new Message();
                        synchronizationMessage.type = MessageType.UPLOAD;
                        PayloadUpload pUpload = new PayloadUpload();
                        synchronizationMessage.payload = pUpload;

                        pUpload.clientID = this.ID;
                        pUpload.imageName = "TestImage.png";
                        pUpload.imageData = (byte[])imageFileTest.ReadFile();
                        pUpload.textName = "TestImage.txt";
                        pUpload.acompanyingText = (String)textFileTest.ReadFile();

                        this.oStream.writeObject(synchronizationMessage);
                        this.oStream.flush();
                    }
                    break;
                    case 8:
                    {
                        // Download Photo
                        Message downloadMessage = new Message();
                        downloadMessage.type = MessageType.DOWNLOAD_PHOTO;
                        PayloadDownload pDownload = new PayloadDownload();
                        downloadMessage.payload = pDownload;
                        pDownload.clientID = this.ID;

                        System.out.println("\n----------- PHOTO DOWNLOAD -----------");
                        System.out.print("Photo name to download: ");
                        pDownload.name = sc.nextLine();

                        System.out.println("\nSending download request...");
                        this.oStream.writeObject(downloadMessage);
                        this.oStream.flush();

                        // Image to download and client that has it
                        System.out.println("Receiving response...");
                        Message imageSearchResult = (Message)this.iStream.readObject();
                        if (imageSearchResult.type == MessageType.ERROR) {
                            System.out.println("\n✗ Photo not found!");
                            System.out.println("---------------------------------------");
                            break;
                        }

                        // Else image search is successfull
                        PayloadDownload pReply = (PayloadDownload)imageSearchResult.payload;
                        int clientID = pReply.clientID;
                        String photoName = pReply.name;

                        // 3 way handshake
                        Message message1 = new Message();
                        message1.type = MessageType.SYN;
                        message1.payload = null;

                        System.out.println("Syn");
                        long t1 = System.currentTimeMillis();
                        this.oStream.writeObject(message1);
                        this.oStream.flush();

                        System.out.println("SynAck");
                        Message response1 = (Message)this.iStream.readObject();

                        // Validate the SYN_ACK message
                        if (response1.type != MessageType.SYN_ACK) {
                            System.err.println("Expected SYN_ACK message but received: " + response1.type);
                            System.out.println("✗ Download failed due to protocol error");
                            System.out.println("---------------------------------------");
                            break;
                        }

                        long t2 = System.currentTimeMillis();
                        int timeout = (int)(t2 - t1);

                        Message message2 = new Message();
                        message2.type = MessageType.ACK;
                        PayloadDownload pDownload2 = new PayloadDownload();
                        message2.payload = pDownload2;
                        pDownload2.clientID = clientID;
                        pDownload2.name = photoName;
                        pDownload2.timeout = 5000;  //timeout; // Round trip time is not consistent

                        System.out.println("Ack");
                        this.oStream.writeObject(message2);
                        this.oStream.flush();

                        System.out.println("Receiving Image Parameters...");
                        // Send 3rd message wait for timeout
                        Message response2 = (Message)this.iStream.readObject();

                        // Validate the response message
                        if (response2.type != MessageType.SYN_ACK) {
                            System.err.println("Expected SYN_ACK message but received: " + response2.type);
                            System.out.println("✗ Download failed due to protocol error");
                            System.out.println("---------------------------------------");
                            break;
                        }

                        // Retransmission
                        System.out.println("Retransmitting Image Parameters...");
                        Message response3 = (Message)this.iStream.readObject();
                        int imageBytes = (int)response3.payload;

                        Message message3 = new Message();
                        message3.type = MessageType.ACK;
                        message3.payload = null;

                        System.out.println("ACK Image Parameters");
                        this.oStream.writeObject(message3);
                        this.oStream.flush();

                        int i = 0;
                        // Break image into 10 pieces
                        int ne = (int)((float)imageBytes / 10.0f);
                        int neFinalPacket = (int)(((float)imageBytes / 10.0f) + ((float)(_ceil((float)imageBytes / 10.0f)) - (float)imageBytes / 10.0f));

                        ArrayList<Byte> reconstructedImage = new ArrayList<Byte>();

                        System.out.println(ne);
                        System.out.println(neFinalPacket);
                        while (i < 10) {                            
                            // Send parameters
                            CommandAPDU commandAPDU = new CommandAPDU();
                            commandAPDU.nc = 0;
                            commandAPDU.ne = (short)ne;

                            if (i == 9) {
                                // 10th message final packet
                                commandAPDU.ne = (short)neFinalPacket;    
                            }

                            if (i == 2) {
                                // 5th message of client to the server
                                // Send also timeout

                                ByteArrayOutputStream bOS = new ByteArrayOutputStream();
                                ObjectOutputStream oS = new ObjectOutputStream(bOS);

                                oS.writeInt(timeout);
                                oS.flush();

                                commandAPDU.commandData = bOS.toByteArray();
                                commandAPDU.nc = (short)commandAPDU.commandData.length;
                            } else if (i == 3) {
                                // 6th message of client to the server
                                try {
                                    Thread.sleep(10000);
                                    CommandAPDU commandAPDU2 = new CommandAPDU();
                                    commandAPDU2.ne = (short)ne;
                                    commandAPDU2.nc = 0;
                                    commandAPDU2.commandData = null;
                                    
                                    System.out.println("Sending CommandAPDU...");
                                    this.oStream.writeObject(commandAPDU2);
                                    this.oStream.flush();
                                    // Send 2 packets handle double ACK in server side
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            System.out.println("Sending CommandAPDU...");
                            t1 = System.currentTimeMillis();
                            this.oStream.writeObject(commandAPDU);
                            this.oStream.flush();

                            System.out.println("Reading ResponseAPDU...");
                            ResponseAPDU responseAPDU = (ResponseAPDU)this.iStream.readObject();
                            t2 = System.currentTimeMillis();
                            timeout = (int)(t2 - t1);

                            // Success
                            if (responseAPDU.sw1sw2 == 0x9000) {
                                for (int j = 0; j < commandAPDU.ne; ++j) {
                                    reconstructedImage.add(responseAPDU.responseData[j]);
                                }
                            }
                            i++;
                        }

                        System.out.println("Finished!");
                        PayloadText textFile = (PayloadText)this.iStream.readObject();

                        if (textFile.text == null) {
                            System.out.println("There is no accompanying text with the photo requested");
                        } else {
                            BufferedWriter fw = null;

                            try {
                                fw = new BufferedWriter(new FileWriter(new File(this.clientDirectoryPath + photoName)));
                                fw.write(textFile.text);
                            } catch(IOException e) {
                                throw new RuntimeException();
                            } finally {
                                if(fw != null) {
                                    fw.close();
                                }
                            }
                        }

                        Byte[] imageData = reconstructedImage.toArray(new Byte[0]);
                        byte[] imageData2 = new byte[imageData.length];
                        // memcpy
                        for (int j = 0; j < imageData.length; ++j) {
                            imageData2[j] = imageData[j];
                        }

                        ByteArrayInputStream bao = new ByteArrayInputStream(imageData2);
                        BufferedImage bImage = ImageIO.read(bao);

                        // . in regex means any character we have to escape it
                        String[] tokens = photoName.split("\\.");

                        ImageIO.write(bImage, tokens[1], new File(this.clientDirectoryPath + photoName));

                        System.out.println("\n✓ Download complete!");
                        System.out.println("Photo saved to: " + this.clientDirectoryPath + photoName);
                        System.out.println("---------------------------------------");
                    }
                    break;
                    case 9:
                    {
                        // Refresh
                        // Get the social graph
                        Message clientGraphMessage = new Message();
                        clientGraphMessage.type = MessageType.GET_CLIENT_GRAPH;
                        PayloadUserID pUserID = new PayloadUserID();
                        clientGraphMessage.payload = pUserID;

                        pUserID.clientID = this.ID;

                        this.oStream.writeObject(clientGraphMessage);
                        this.oStream.flush();

                        PayloadClientGraph pClientGraph = (PayloadClientGraph)((Message)this.iStream.readObject()).payload;

                        // Update followers, followings
                        this.followerDatas.clear();
                        this.followerDatas.addAll(pClientGraph.followers);
                        this.followingDatas.clear();
                        this.followingDatas.addAll(pClientGraph.followings);

                        // For all followings GET_NOTIFICATIONS
                        Message notificationsMessage = new Message();
                        notificationsMessage.type = MessageType.GET_NOTIFICATIONS;
                        PayloadUserID pUserID2 = new PayloadUserID();
                        notificationsMessage.payload = pUserID2;

                        pUserID2.clientID = this.ID;

                        this.oStream.writeObject(notificationsMessage);
                        this.oStream.flush();

                        // Populate notifications

                        PayloadText pText = (PayloadText)((Message)this.iStream.readObject()).payload;

                        String[] newNotifications = pText.text.split("\n");
                        this.notifications.clear();

                        if(newNotifications != null) {
                            for(String notification : newNotifications) {
                                if (!notification.trim().isEmpty()) {
                                    this.notifications.add(notification);
                                }
                            }
                        }

                        // Synchronize Directory
                        Message synchronizeMessage = new Message();
                        synchronizeMessage.type = MessageType.SYNCHRONIZE_DIRECTORY;
                        PayloadDirectory pDirectory = new PayloadDirectory();
                        synchronizeMessage.payload = pDirectory;

                        pDirectory.fileDatas = this.clientDirectory.ComputeFileDatas();
                        pDirectory.clientID = this.ID;

                        this.oStream.writeObject(synchronizeMessage);
                        this.oStream.flush();

                        Object receivedObject = this.iStream.readObject();
                        ArrayList<String> unsynchronizedFilePaths;

                        if (receivedObject instanceof ArrayList<?>) {
                            ArrayList<?> rawList = (ArrayList<?>) receivedObject;
                            unsynchronizedFilePaths = new ArrayList<String>();

                            // Safely check each element
                            for (Object item : rawList) {
                                if (item instanceof String) {
                                    unsynchronizedFilePaths.add((String) item);
                                } else {
                                    System.err.println("Warning: Non-string item in file paths list: " + item);
                                }
                            }
                        } else {
                            System.err.println("Error: Expected ArrayList but received: " + 
                                              (receivedObject != null ? receivedObject.getClass().getName() : "null"));
                            unsynchronizedFilePaths = new ArrayList<String>();
                        }

                        // System.out.println(unsynchronizedFilePaths.size());
                        for (String filePath : unsynchronizedFilePaths) {
                            Message getFileContentsMessage = new Message();
                            getFileContentsMessage.type = MessageType.GET_FILE_CONTENTS;
                            PayloadDownload pDownload = new PayloadDownload();
                            getFileContentsMessage.payload = pDownload;

                            pDownload.name = filePath;
                            pDownload.clientID = this.ID;

                            this.oStream.writeObject(getFileContentsMessage);
                            this.oStream.flush();

                            Object contents = this.iStream.readObject();
                            // if (filePath.contains(".txt")) {
                            //     System.out.println((String)contents);
                            // }
                            this.clientDirectory.SetFile(filePath);
                            this.clientDirectory.GetFile(filePath).WriteFile(contents);;
                        }

                        PrintUserOptions();
                    }
                    break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            cleanup();
            throw new RuntimeException();
        } catch (ClassNotFoundException e) {
            cleanup();
            throw new RuntimeException();
        } finally {
            cleanup();  // Ensure cleanup is called on exit
        }
    }

    int _ceil(float x) {
        return (int) Math.ceil(x);
    }

    public void cleanup() {
        if (isShuttingDown) return;  // Prevent multiple cleanup calls
        isShuttingDown = true;

        System.out.println("Cleaning up resources...");

        // Close network connections
        try {
            if (oStream != null) {
                oStream.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing output stream: " + e.getMessage());
        }

        try {
            if (iStream != null) {
                iStream.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing input stream: " + e.getMessage());
        }

        try {
            if (serverConnection != null && !serverConnection.isClosed()) {
                serverConnection.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server connection: " + e.getMessage());
        }

        // Close Scanner (be careful with System.in)
        if (sc != null) {
            sc.close();
        }

        System.out.println("Cleanup completed.");
    }
}
