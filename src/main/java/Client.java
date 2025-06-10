import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import Messages.Language;
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

        // Establish connection immediately and keep it open
        try {
            this.serverConnection = new Socket(serverHostIP, serverPort);
            this.oStream = new ObjectOutputStream(this.serverConnection.getOutputStream());
            this.iStream = new ObjectInputStream(this.serverConnection.getInputStream());
            System.out.println("DEBUG: Connection established to server");
        } catch (IOException e) {
            System.err.println("ERROR: Failed to connect to server: " + e.getMessage());
            throw e;
        }
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

                        if (serverResponse == null) {
                            System.err.println("✗ Server response error: No response received");
                            PrintLoginScreenOptions();
                            break;
                        }

                        if (serverResponse.payload == null) {
                            System.err.println("✗ Server response error: Invalid response format");
                            System.err.println("DEBUG: Response type: " + serverResponse.type);
                            PrintLoginScreenOptions();
                            break;
                        }

                        int clientID = ((PayloadUserID)serverResponse.payload).clientID;
                        if (clientID != -1) {
                            // Valid connection
                            this.ID = clientID;
                            this.clientDirectoryPath = String.join(File.separator, "src", "main", "resources", "ClientDirectories", "Client" + clientID, "");
                            this.clientDirectory = new Directory(this.clientDirectoryPath, clientID);
                            System.out.println("✓ Login successful! Welcome client " + Integer.toString(this.ID));

                            EnterApplication();

                            // After log out we want to print the login screen options again
                            PrintLoginScreenOptions();
                        } else {
                            // Wrong credentials
                            System.out.println("✗ Login failed: Incorrect username or password");
                            PrintLoginScreenOptions();
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
                            this.clientDirectoryPath = String.join(File.separator, "src", "main", "resources", "ClientDirectories", "Client" + clientID, "");
                            this.clientDirectory = new Directory(this.clientDirectoryPath, clientID);
                            System.out.println("✓ Account created successfully! Welcome client " + Integer.toString(this.ID));

                            EnterApplication();

                            // After log out we want to print the login screen options again
                            PrintLoginScreenOptions();
                        } else {
                            System.out.println("✗ Signup failed: Username already exists");
                            PrintLoginScreenOptions();
                        }
                    }
                    break;
                    default:
                        System.out.println("Invalid option. Please choose 0, 1, or 2.");
                        PrintLoginScreenOptions();
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

    public void EnterApplication() {
        boolean isRunning = true;

        try {
            while (isRunning && !isShuttingDown) {
                refresh(false);
                PrintUserOptions();

                int option;
                String input = sc.nextLine().trim();
                try {
                    option = input.isEmpty() ? 0 : Integer.parseInt(input);

                    if (option < 0 || option > 9) {
                        System.out.println("Invalid option. Please choose a number between 0 and 9.");
                        PrintUserOptions();
                        continue;
                    }
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
                            System.out.println("-----------------------------------");
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
                                System.out.println("-----------------------------------");
                                break;
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a number.");
                            System.out.println("-----------------------------------");
                            break;
                        }

                        if (option2 == -1) {
                            System.out.println("-----------------------------------");
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
                                System.out.println("-----------------------------------");
                                break;
                            }
                        } catch(NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a number.");
                            System.out.println("-----------------------------------");
                            break;
                        }

                        if (option3 == -1) {
                            System.out.println("-----------------------------------");
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
                        System.out.println("-----------------------------------");
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
                                System.out.println("-----------------------------------");
                                break;
                            }
                        } catch(NumberFormatException e) {
                            System.out.println("✗ Invalid input. Please enter a number.");
                            System.out.println("-----------------------------------");
                            break;
                        }

                        if (followUserID == -1) {
                            System.out.println("Follow request cancelled.");
                            System.out.println("-----------------------------------");
                            break;
                        }

                        if (followUserID == this.ID) {
                            System.out.println("✗ You cannot follow yourself.");
                            System.out.println("-----------------------------------");
                            break;
                        }

                        pRequest.clientIDDestination = followUserID;

                        System.out.println("Sending follow request to user ID " + followUserID + "...");
                        this.oStream.writeObject(followMessage);
                        this.oStream.flush();

                        System.out.println("✓ Follow request sent successfully!");
                        System.out.println("-----------------------------------");
                    }
                    break;
                    case 4:
                    {
                        // Unfollow
                        System.out.println("\n--------------- UNFOLLOW ---------------");

                        if (this.followingDatas.size() == 0) {
                            System.out.println("You are not following anyone.");
                            System.out.println("-----------------------------------");
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
                                System.out.println("-----------------------------------");
                                break;
                            }
                        } catch(NumberFormatException e) {
                            System.out.println("✗ Invalid input. Please enter a number.");
                            System.out.println("-----------------------------------");
                            break;
                        }

                        if (option2 == -1) {
                            System.out.println("Unfollow cancelled.");
                            System.out.println("-----------------------------------");
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
                        System.out.println("-----------------------------------");
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

                        System.out.println("-----------------------------------");
                    }
                    break;
                    case 6:
                    {
                        // Access Profile
                        System.out.println("\n----------- PROFILE ACCESS -----------");

                        if (this.followingDatas.size() == 0) {
                            System.out.println("You are not following anyone.");
                            System.out.println("-----------------------------------");
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
                                System.out.println("-----------------------------------");
                                break;
                            }
                        } catch(NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a number.");
                            System.out.println("-----------------------------------");
                            break;
                        }

                        if (choice2 == -1) {
                            System.out.println("-----------------------------------");
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
                                System.out.println("-----------------------------------");
                                break;
                            }
                        } catch(NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a number.");
                            System.out.println("-----------------------------------");
                            break;
                        }

                        if (profileChoice == -1 || profileChoice == this.ID) {
                            System.out.println("-----------------------------------");
                            break;
                        }

                        pRequest.clientIDDestination = this.followingDatas.get(profileChoice).ID;

                        this.oStream.writeObject(requestMessage);
                        this.oStream.flush();

                        // Receive answer either "Access Profile Denied Reason: not following specified client" or
                        // Receive text with Profile_31ClientID of user with clientID = ID then print it
                        PayloadText pText = (PayloadText)this.iStream.readObject();

                        System.out.println(pText.text);
                        System.out.println("-----------------------------------");
                    }
                    break;
                    case 7:
                    {
                        // Upload
                        System.out.println("\n------------- UPLOAD -------------");
                        Message uploadMessage = new Message();
                        uploadMessage.type = MessageType.UPLOAD;
                        PayloadUpload pUpload = new PayloadUpload();
                        uploadMessage.payload = pUpload;
                        pUpload.clientID = this.ID;

                        System.out.print("Image name to upload: ");
                        String imageName = sc.nextLine().trim();
                        if (!imageName.endsWith(".png")) {
                            imageName += ".png";
                        }
                        pUpload.imageName = imageName;

                        // Load image data
                        try {
                            // First check if file exists in client directory
                            _File imageFile = this.clientDirectory.GetFile(pUpload.imageName);

                            // Check if the file actually exists on disk
                            File diskFile = new File(this.clientDirectoryPath + pUpload.imageName);
                            if (!diskFile.exists()) {
                                System.out.println("✗ Error: Image file '" + pUpload.imageName + "' not found in your directory.");
                                System.out.println("   Please make sure the file exists in: " + this.clientDirectoryPath);
                                System.out.println("-----------------------------------");
                                break;
                            }

                            pUpload.imageData = (byte[])imageFile.ReadFile();

                            if (pUpload.imageData == null || pUpload.imageData.length == 0) {
                                System.out.println("✗ Error: Image file is empty or corrupted: " + pUpload.imageName);
                                System.out.println("-----------------------------------");
                                break;
                            }
                        } catch (Exception e) {
                            System.out.println("✗ Error: Could not read image file: " + pUpload.imageName);
                            System.out.println("-----------------------------------");
                            break;
                        }

                        // Get text name
                        String photoName = pUpload.imageName;
                        String[] tokens = photoName.split("\\.");
                        pUpload.textName = tokens[0];
                        if (!pUpload.textName.endsWith(".txt")) {
                            pUpload.textName += ".txt";
                        }

                        // Enhanced multilingual text input
                        System.out.println("\nText input options:");
                        System.out.println("1) English only");
                        System.out.println("2) Greek only");
                        System.out.println("3) Both languages");
                        System.out.print("Choice (1-3): ");

                        String textOption = sc.nextLine().trim();

                        switch (textOption) {
                            case "1":
                                // English only
                                System.out.print("English text: ");
                                String englishText = sc.nextLine().trim();
                                if (!englishText.isEmpty()) {
                                    pUpload.addText(Language.ENGLISH, englishText);
                                    pUpload.acompanyingText = englishText;
                                } else {
                                    System.out.println("✗ Error: Text is required!");
                                    System.out.println("-----------------------------------");
                                    continue;
                                }
                                break;
                    
                            case "2":
                                // Greek only
                                System.out.print("Greek text: ");
                                String greekText = sc.nextLine().trim();
                                if (!greekText.isEmpty()) {
                                    pUpload.addText(Language.GREEK, greekText);
                                    pUpload.acompanyingText = greekText;
                                } else {
                                    System.out.println("✗ Error: Text is required!");
                                    System.out.println("-----------------------------------");
                                    continue;
                                }
                                break;
                    
                            case "3":
                                // Both languages
                                System.out.print("English text: ");
                                String englishBoth = sc.nextLine().trim();
                                System.out.print("Greek text: ");
                                String greekBoth = sc.nextLine().trim();
                    
                                if (englishBoth.isEmpty() || greekBoth.isEmpty()) {
                                    System.out.println("✗ Error: Both languages are required when selecting 'Both'!");
                                    System.out.println("-----------------------------------");
                                    continue;
                                }
                    
                                pUpload.addText(Language.ENGLISH, englishBoth);
                                pUpload.addText(Language.GREEK, greekBoth);
                                pUpload.acompanyingText = pUpload.getFormattedText(Language.BOTH);
                                break;
                    
                            default:
                                System.out.println("✗ Error: Invalid option!");
                                System.out.println("-----------------------------------");
                                continue;
                        }

                        System.out.println("Uploading...");
                        this.oStream.writeObject(uploadMessage);
                        this.oStream.flush();

                        System.out.println("✓ Upload complete!");
                        System.out.println("-----------------------------------");
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
                        pDownload.name = sc.nextLine().trim();
                        if (!pDownload.name.endsWith(".png")) {
                            pDownload.name += ".png";
                        }

                        // Language preference selection
                        System.out.println("\nText language preference:");
                        System.out.println("1) English only");
                        System.out.println("2) Greek only");
                        System.out.println("3) Both languages (mixed)");
                        System.out.print("Choice (1-3): ");

                        String langChoice = sc.nextLine().trim();
                        switch (langChoice) {
                            case "1":
                                pDownload.preferredLanguage = Language.ENGLISH;
                                break;
                            case "2":
                                pDownload.preferredLanguage = Language.GREEK;
                                break;
                            case "3":
                                pDownload.preferredLanguage = Language.BOTH;
                                break;
                            default:
                                pDownload.preferredLanguage = Language.BOTH;
                                System.out.println("Invalid choice, defaulting to both languages.");
                                break;
                        }

                        System.out.println("\nSending download request...");
                        this.oStream.writeObject(downloadMessage);
                        this.oStream.flush();

                        String photoName = pDownload.name;

                        // Image to download and client that has it
                        System.out.println("Receiving response...");
                        Message response = (Message)this.iStream.readObject();

                        if (response.type == MessageType.DOWNLOAD_PHOTO) {
                            PayloadDownload responsePayload = (PayloadDownload)response.payload;
                            System.out.println("Found photo in client " + responsePayload.clientID + "'s directory");

                            // 3-way handshake and image download (existing code)
                            Message syn = new Message();
                            syn.type = MessageType.SYN;
                            long t1 = System.currentTimeMillis();

                            System.out.println("Syn");
                            this.oStream.writeObject(syn);
                            this.oStream.flush();

                            System.out.println("SynAck");
                            Message response1 = (Message)this.iStream.readObject();
                            long t2 = System.currentTimeMillis();

                            // Validate the SYN_ACK message
                            if (response1.type != MessageType.SYN_ACK) {
                                System.err.println("Expected SYN_ACK message but received: " + response1.type);
                                System.out.println("✗ Download failed due to protocol error");
                                System.out.println("-----------------------------------");
                                break;
                            }

                            int timeout = (int)(t2 - t1);

                            Message ack = new Message();
                            ack.type = MessageType.ACK;
                            ack.payload = timeout;

                            System.out.println("Ack with timeout");
                            this.oStream.writeObject(ack);
                            this.oStream.flush();

                            System.out.println("Receiving Image Parameters...");
                            Message response2 = (Message)this.iStream.readObject();

                            // Validate the response message
                            if (response2.type != MessageType.SYN_ACK) {
                                System.err.println("Expected SYN_ACK message but received: " + response2.type);
                                System.out.println("✗ Download failed due to protocol error");
                                System.out.println("-----------------------------------");
                                break;
                            }

                            System.out.println("ACK Image Parameters");
                            Message response3 = (Message)this.iStream.readObject();

                            // Validate the ACK message
                            if (response3.type != MessageType.ACK) {
                                System.err.println("Expected ACK message but received: " + response3.type);
                                System.out.println("✗ Download failed due to protocol error");
                                System.out.println("-----------------------------------");
                                break;
                            }

                            int imageBytes = (int)response3.payload;

                            // Download image data
                            ArrayList<Byte> reconstructedImage = new ArrayList<Byte>();
                            int packetsToReceive = _ceil((float)imageBytes / 1024);

                            for (int i = 0; i < packetsToReceive; ++i) {
                                try {
                                    this.iStream.readObject();
                                    byte[] packet = (byte[])this.iStream.readObject();

                                    for (byte b : packet) {
                                        reconstructedImage.add(b);
                                    }
                                } catch (SocketTimeoutException e) {
                                    // Retransmission logic
                                    Message retransmissionMessage = new Message();
                                    retransmissionMessage.type = MessageType.SYN;
                                    retransmissionMessage.payload = i;

                                    this.oStream.writeObject(retransmissionMessage);
                                    this.oStream.flush();

                                    byte[] packet = (byte[])this.iStream.readObject();

                                    for (byte b : packet) {
                                        reconstructedImage.add(b);
                                    }
                                }
                            }

                            // Save image
                            if (reconstructedImage.size() > 0) {
                                Byte[] imageData = reconstructedImage.toArray(new Byte[0]);
                                byte[] primitiveImageData = new byte[imageData.length];
                                for (int i = 0; i < imageData.length; i++) {
                                    primitiveImageData[i] = imageData[i];
                                }

                                photoName = pDownload.name;
                                _File imageFile = this.clientDirectory.GetFile(photoName);
                                imageFile.WriteFile(primitiveImageData);
                                System.out.println("✓ Image downloaded: " + photoName);
                            }

                            // Download accompanying text
                            PayloadText textFile = (PayloadText)this.iStream.readObject();

                            if (textFile.text == null || textFile.text.trim().isEmpty()) {
                                System.out.println("No accompanying text available for this photo");
                            } else {
                                String displayText = extractTextForLanguage(textFile.text, pDownload.preferredLanguage);

                                System.out.println("\n--- ACCOMPANYING TEXT ---");
                                System.out.println(displayText);
                                System.out.println("-----------------------------------");

                                // Save text file
                                String textFileName = photoName.substring(0, photoName.lastIndexOf('.')) + ".txt";
                                _File textFileLocal = this.clientDirectory.GetFile(textFileName);
                                textFileLocal.WriteFile(displayText);
                                System.out.println("✓ Text saved to: " + textFileName);
                            }

                            System.out.println("✓ Download complete!");
                        } else if (response.type == MessageType.ERROR) {
                            System.out.println("✗ Photo not found or doesn't have text in the preferred language");
                            System.out.println("-----------------------------------");
                        }
                    }
                    break;
                    case 9:
                    {
                        // Refresh
                        refresh(true);
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

    // Helper method to extract text based on language preference
    private String extractTextForLanguage(String fullText, Language preferredLanguage) {
        if (fullText == null || fullText.trim().isEmpty()) return "";

        // If no language tags, return as-is
        if (!fullText.contains("[EN]") && !fullText.contains("[EL]")) {
            return fullText;
        }

        if (preferredLanguage == Language.BOTH) {
            // Format all languages with clear separators
            return formatAllLanguages(fullText);
        }

        // Extract specific language
        String[] lines = fullText.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inTargetLanguage = false;
        String targetTag = (preferredLanguage == Language.ENGLISH) ? "[EN]" : "[EL]";

        for (String line : lines) {
            if (line.trim().equals("[EN]") || line.trim().equals("[EL]")) {
                inTargetLanguage = line.trim().equals(targetTag);
            } else if (inTargetLanguage) {
                if (result.length() > 0) result.append("\n");
                result.append(line);
            }
        }

        // If target language not found, return formatted all languages
        if (result.length() == 0) {
            return formatAllLanguages(fullText) + "\n[Note: " + preferredLanguage.name() + " version not available]";
        }

        return result.toString();
    }

    private String formatAllLanguages(String fullText) {
        if (!fullText.contains("[EN]") && !fullText.contains("[EL]")) {
            return fullText;
        }

        String[] lines = fullText.split("\n");
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            if (line.trim().equals("[EN]")) {
                if (result.length() > 0) result.append("\n");
                result.append("=== ENGLISH ===\n");
            } else if (line.trim().equals("[EL]")) {
                if (result.length() > 0) result.append("\n");
                result.append("=== GREEK ===\n");
            } else {
                result.append(line).append("\n");
            }
        }

        return result.toString().trim();
    }

    private void refresh(boolean flag) {
        try {
            if (flag) {
                System.out.println("\n------------- REFRESH -------------");
            }

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

            if (flag) {
                System.out.println("✓ Refresh complete!");
                System.out.println("Updated notifications: " + this.notifications.size());
                System.out.println("-----------------------------------");
            }
        } catch (IOException e) {
            throw new RuntimeException();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException();
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
