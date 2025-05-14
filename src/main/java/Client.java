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

    private Directory testDirectory;
    private Directory clientDirectory;

    // Cache of Client from server refreshes when connecting to server and when refreshing option selected
    private ArrayList<UserAccountData> followerDatas;
    private ArrayList<UserAccountData> followingDatas;
    private ArrayList<String> notifications;

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

    public String GetClientNotificationsFilePath() {
        if(this.ID == -1) return "";
        return this.clientDirectoryPath + "Others_31client" + Integer.toString(this.ID) + ".txt";
    }

    private void PrintLoginScreenOptions() {
        System.out.println("0) Print Available Options");
        System.out.println("1) Login");
        System.out.println("2) Signup");
    }

    private void PrintUserOptions() {
        System.out.println("0) Print Available Options");
        System.out.println("1) Logout");
        System.out.println("2) Accept Follow Request");
        System.out.println("3) Follow Request");
        System.out.println("4) Unfollow");
        System.out.println("5) View Notifications (" + this.notifications.size() + ")");
        System.out.println("6) Access Profile");
        System.out.println("7) Upload");
        System.out.println("8) Download Photo");
        System.out.println("9) Refresh");
    }

    public static void main(String[] args) {
        if(args.length != 4) return;

        new Thread(new Client(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]))).start();
    }

    public Client(String serverHostIP, int serverPort, String hostIP, int port) {
        this.serverHostData = new HostData(serverHostIP, serverPort);
        this.hostData = new HostData(hostIP, port);
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
            // Get Followers, Followings, Notifications

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
                        this.oStream.flush();

                        serverResponse = (Message)this.iStream.readObject();

                        int clientID = ((PayloadUserID)serverResponse.payload).clientID;
                        if(clientID != -1) {
                            // valid connection
                            this.ID = clientID;
                            this.clientDirectoryPath = "src/main/resources/ClientDirectories/Client" + Integer.toString(clientID) + "/";
                            this.clientDirectory = new Directory(this.clientDirectoryPath, clientID);
                            System.out.println("Welcome client " + Integer.toString(this.ID));

                            EnterApplication();
                        }
                        else {
                            // wrong credentials
                            System.out.println("Wrong Credentials!");
                        }
                    }
                    break;
                    case 2:
                    {
                        serverMessage.type = MessageType.SIGNUP;
                        PayloadCredentials pCredentials = new PayloadCredentials();
                        serverMessage.payload = pCredentials;

                        pCredentials.credentials = new Credentials();

                        System.out.print("Username:");
                        pCredentials.credentials.userName = sc.nextLine();
                        System.out.print("Password:");
                        pCredentials.credentials.password = sc.nextLine();

                        this.oStream.writeObject(serverMessage);
                        this.oStream.flush();

                        serverResponse = (Message)this.iStream.readObject();

                        int clientID = ((PayloadUserID)serverResponse.payload).clientID;

                        if(clientID != -1) {
                            // valid connection
                            this.ID = clientID;
                            this.clientDirectoryPath = "src/main/resources/ClientDirectories/Client" + Integer.toString(clientID) + "/";
                            this.clientDirectory = new Directory(this.clientDirectoryPath, clientID);
                            System.out.println("Welcome client " + Integer.toString(this.ID));

                            EnterApplication();
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

    public void EnterApplication() {
        PrintUserOptions();

        boolean isRunning = true;
        try {
            while(isRunning) {
                int option = Integer.parseInt(sc.nextLine());
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
                        ArrayList<String> followRequests = new ArrayList<String>();

                        for(int i = 0; i < notifications.size(); ++i) {
                            if(notifications.get(i).contains("follow request")) {
                                followRequests.add(notifications.get(i));
                            }
                        }

                        if(followRequests.size() == 0) break;

                        for(int i = 0; i < followRequests.size(); ++i) {
                            System.out.println(Integer.toString(i) + ") " + followRequests.get(i));
                        }

                        System.out.print("Choose a request to accept(-1 to go back): ");
                        int option2;
                        do {
                            option2 = Integer.parseInt(this.sc.nextLine()); 
                        } while(option2 < -1 || option2 >= followRequests.size());

                        if(option2 == -1) {
                            break;
                        }

                        String[] followRequestTokens = followRequests.get(option2).split("\\s+");
                        int followerID = Integer.parseInt(followRequestTokens[0]);
                        System.out.println("0) Accept");
                        System.out.println("1) Reject");
                        System.out.println("2) Accept and follow back");
                        System.out.print("Choose an option(-1 to cancel): ");

                        int option3;
                        do {
                            option3 = Integer.parseInt(this.sc.nextLine());
                        } while(option3 < -1 || option3 > 2);

                        if(option3 == -1) {
                            break;
                        }

                        Message requestMessage = new Message();

                        if(option3 == 0) {
                            requestMessage.type = MessageType.FOLLOW_REQUEST_ACCEPT;
                        } else if(option3 == 1) {
                            requestMessage.type = MessageType.FOLLOW_REQUEST_REJECT;
                        } else {
                            // we are accepting and sending another request
                            requestMessage.type = MessageType.FOLLOW_REQUEST_ACCEPT;
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

                        if(option3 == 2) {
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
                    }
                    break;
                    case 3:
                    {
                        // Follow Request
                        Message followMessage = new Message();
                        followMessage.type = MessageType.FOLLOW_REQUEST;
                        PayloadClientRequest  pRequest = new PayloadClientRequest();
                        followMessage.payload = pRequest;
                        pRequest.clientIDSource = this.ID;

                        System.out.print("Input User ID to follow(-1 to go back): ");
                        int followUserID;

                        do {
                            followUserID = Integer.parseInt(this.sc.nextLine()); 
                        } while(followUserID < -1);

                        if(followUserID == -1 || followUserID == this.ID) {
                            break;
                        }

                        pRequest.clientIDDestination = followUserID;

                        this.oStream.writeObject(followMessage);
                        this.oStream.flush();
                    }
                    break;
                    case 4:
                    {
                        // Unfollow
                        if(this.followingDatas.size() == 0) break;

                        for(int i = 0; i < this.followingDatas.size(); ++i) {
                            System.out.println(Integer.toString(i) + ") " + this.followingDatas.get(i));
                        }

                        System.out.print("Choose a user to unfollow(-1 to go back): ");
                        int option2;

                        do {
                            option2 = Integer.parseInt(this.sc.nextLine()); 
                        } while(option2 < -1 || option2 >= this.followerDatas.size());

                        if(option2 == -1) {
                            break;
                        }

                        int unfollowUserID = this.followingDatas.get(option2).ID;

                        // Java's serialization system (ObjectStreaming) uses a cache system when sending the same object twice (serverMessage) it does
                        // not send 2 times the object it sends it only the first time then the second time it sends only the reference since it is cached
                        // break your head in the wall if you do not know this I used a brand new message
                        Message serverMessage3 = new Message();
                        serverMessage3.type = MessageType.UNFOLLOW;
                        PayloadClientRequest pRequest = new PayloadClientRequest();
                        serverMessage3.payload = pRequest;
                        pRequest.clientIDSource = this.ID;
                        pRequest.clientIDDestination = unfollowUserID;

                        this.oStream.writeObject(serverMessage3);
                        this.oStream.flush();

                        // Debug Information
                        serverResponse = (Message)this.iStream.readObject();
                        PayloadClientGraph pGraph = (PayloadClientGraph)serverResponse.payload;

                        for(int i = 0; i < pGraph.followings.size(); ++i) {
                            System.out.println(Integer.toString(i) + ") " + pGraph.followings.get(i));
                        }
                    }
                    break;
                    case 5:
                    {
                        // View notifications...
                        for(int i = 0; i < this.notifications.size(); ++i) {
                            System.out.println(this.notifications.get(i));
                        }
                    }
                    break;
                    case 6:
                    {
                        // Access Profile
                        // Send access profile request with PayloadcClientRequest
                        Message requestMessage = new Message();
                        requestMessage.type = MessageType.ACCESS_PROFILE;
                        PayloadClientRequest pRequest = new PayloadClientRequest();
                        requestMessage.payload = pRequest;
                        pRequest.clientIDSource = this.ID;

                        for(int i = 0; i < this.followingDatas.size(); ++i) {
                            System.out.printf("%d) %s\n", i, this.followingDatas.get(i));
                        }

                        System.out.println("Input Client ID to view Profile: ");
                        int choice2;
                        do {
                            choice2 = Integer.parseInt(this.sc.nextLine());
                        } while(choice2 < -1 || choice2 > this.followingDatas.size());

                        if(choice2 == -1 || choice2 == this.ID) break;

                        pRequest.clientIDDestination = this.followingDatas.get(choice2).ID;

                        this.oStream.writeObject(requestMessage);
                        this.oStream.flush();

                        // Receive answer either "Access Profile Denied Reason: not following specified client" or
                        // receive text with Profile_31ClientID of user with clientID = ID then print it
                        PayloadText pText = (PayloadText)this.iStream.readObject();

                        System.out.println(pText.text);
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
                        System.out.print("Input Photo Name to download: ");
                        pDownload.name = this.sc.nextLine();

                        System.out.println("Sending Download Request");
                        this.oStream.writeObject(downloadMessage);
                        this.oStream.flush();

                        // Image to download and client that has it
                        System.out.println("Receiving Download Response");
                        Message imageSearchResult = (Message)this.iStream.readObject();
                        if(imageSearchResult.type == MessageType.ERROR) {
                            System.out.println("File not found!");
                            break;
                        }

                        // else image search is successfull
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
                        long t2 = System.currentTimeMillis();
                        int timeout = (int)(t2 - t1);

                        Message message2 = new Message();
                        message2.type = MessageType.ACK;
                        PayloadDownload pDownload2 = new PayloadDownload();
                        message2.payload = pDownload2;
                        pDownload2.clientID = clientID;
                        pDownload2.name = photoName;
                        pDownload2.timeout = 5000;  //timeout; // round trip time is not consistent

                        System.out.println("Ack");
                        this.oStream.writeObject(message2);
                        this.oStream.flush();

                        System.out.println("Receiving Image Parameters");
                        // send 3rd message wait for timeout
                        Message response2 = (Message)this.iStream.readObject();

                        // Retransmission
                        System.out.println("Retransmission Image Parameters");
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
                        while(i < 10) {                            
                            // Send parameters
                            CommandAPDU commandAPDU = new CommandAPDU();
                            commandAPDU.nc = 0;
                            commandAPDU.ne = (short)ne;

                            if(i == 9) {
                                // 10th message final packet
                                commandAPDU.ne = (short)neFinalPacket;    
                            }

                            if(i == 2) {
                                // 5th message of client to the server
                                // send also timeout

                                ByteArrayOutputStream bOS = new ByteArrayOutputStream();
                                ObjectOutputStream oS = new ObjectOutputStream(bOS);

                                oS.writeInt(timeout);
                                oS.flush();

                                commandAPDU.commandData = bOS.toByteArray();
                                commandAPDU.nc = (short)commandAPDU.commandData.length;
                            } else if(i == 3) {
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
                                    // send 2 packets handle double ACK in server side
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
                            if(responseAPDU.sw1sw2 == 0x9000) {
                                for(int j = 0; j < commandAPDU.ne; ++j) {
                                    reconstructedImage.add(responseAPDU.responseData[j]);
                                }
                            }
                            i++;
                        }

                        System.out.println("Finished!");
                        PayloadText textFile = (PayloadText)this.iStream.readObject();

                        if(textFile.text == null) {
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
                        for(int j = 0; j < imageData.length; ++j) {
                            imageData2[j] = imageData[j];
                        }

                        ByteArrayInputStream bao = new ByteArrayInputStream(imageData2);
                        BufferedImage bImage = ImageIO.read(bao);

                        // . in regex means any character we have to escape it
                        String[] tokens = photoName.split("\\.");

                        ImageIO.write(bImage, tokens[1], new File(this.clientDirectoryPath + photoName));
                        System.out.println("transmission is completed");
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

                        if(newNotifications != null) {
                            this.notifications.clear();
                            for(String notification : newNotifications) {
                                this.notifications.add(notification);
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

                        // I dont care about type safety I know what Im doing v
                        ArrayList<String> unsynchronizedFilePaths = (ArrayList<String>)this.iStream.readObject();

                        //System.out.println(unsynchronizedFilePaths.size());
                        for(String filePath : unsynchronizedFilePaths) {
                            Message getFileContentsMessage = new Message();
                            getFileContentsMessage.type = MessageType.GET_FILE_CONTENTS;
                            PayloadDownload pDownload = new PayloadDownload();
                            getFileContentsMessage.payload = pDownload;

                            pDownload.name = filePath;
                            pDownload.clientID = this.ID;

                            this.oStream.writeObject(getFileContentsMessage);
                            this.oStream.flush();

                            Object contents = this.iStream.readObject();
                            // if(filePath.contains(".txt")) {
                            //     System.out.println((String)contents);
                            // }
                            this.clientDirectory.SetFile(filePath);
                            this.clientDirectory.GetFile(filePath).WriteFile(contents);;
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

    int _ceil(float x) {
        if(x == (float)((int)x)) {
            return (int)x;
        } else {
            return (int)x + 1;
        }
    }
}
