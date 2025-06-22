import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
// import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import Messages.Language;
import Messages.Message;
import Messages.MessageType;
import Messages.PayloadApproveComment;
import Messages.PayloadClientGraph;
import Messages.PayloadClientRequest;
import Messages.PayloadComment;
import Messages.PayloadCredentials;
import Messages.PayloadDownload;
import Messages.PayloadText;
import Messages.PayloadUpload;
import Messages.PayloadUserID;
import POD.FileData;
import POD.UserAccountData;


public class ServerActions implements Runnable {

    public int clientID; // this is not connection ID it is the ID of the client using the socket
    private Socket connectionSocket;
    public Server server;

    // Do not send messages in these access them through server
    private ObjectOutputStream oStream;
    private ObjectInputStream iStream;

    public ServerActions(Socket connectionSocket, Server server) {
        this.connectionSocket = connectionSocket;
        this.server = server;
        this.clientID = -1;

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
                    case LOGIN:
                    {
                        PayloadCredentials pCredentials = (PayloadCredentials)connectionMessage.payload;

                        int ID = this.server.GetUserIDFromCredentials(pCredentials.credentials);

                        Message loginResponse = new Message();
                        loginResponse.type = MessageType.LOGIN;
                        PayloadUserID pResult = new PayloadUserID();
                        loginResponse.payload = pResult;
                        pResult.clientID = ID;

                        try {
                            oStream.writeObject(loginResponse);
                            oStream.flush();
                            // System.out.println("DEBUG: Response sent successfully");

                            /*if(this.clientID >= 0) {
                                this.server.UnRegisterOStream(this.clientID);
                                this.clientID = ID;
                                this.server.RegisterOStream(this.clientID, this.oStream);
                            } else {
                                this.clientID = ID;
                                this.server.RegisterOStream(this.clientID, this.oStream);
                            }*/

                            this.clientID = ID;
                            //if(ID >= 0) {
                            //    
                            //}
                        } catch (IOException e) {
                            System.err.println("ERROR: Failed to send login response: " + e.getMessage());
                            throw e;
                        }
                    }
                    break;
                    case SIGNUP:
                    {
                        PayloadCredentials pCredentials = (PayloadCredentials)connectionMessage.payload;

                        int ID = this.server.RegisterUser(pCredentials.credentials);

                        Message registerResponse = new Message();
                        registerResponse.type = MessageType.SIGNUP;
                        PayloadUserID pResult = new PayloadUserID();
                        registerResponse.payload = pResult;
                        pResult.clientID = ID;

                        oStream.writeObject(registerResponse);
                        oStream.flush();
                        
                        //if(ID >= 0) {
                            //this.server.UnRegisterOStream(this.clientID);
                            //this.clientID = ID;
                            //this.server.RegisterOStream(this.clientID, this.oStream);
                        //}

                        this.clientID = ID;
                    }
                    break;
                    case FOLLOW_REQUEST_ACCEPT:
                    {
                        PayloadClientRequest pRequest = (PayloadClientRequest)connectionMessage.payload;

                        // Get source node
                        SocialGraphNode sourceNode = this.server.GetSocialGraph().GetUserNode(pRequest.clientIDSource);
                        // Get target node
                        SocialGraphNode targetNode = this.server.GetSocialGraph().GetUserNode(pRequest.clientIDDestination);

                        if (sourceNode != null && targetNode != null) {
                            // Remove follow request from source's notifications
                            Directory sourceDirectory = this.server.GetDirectory(pRequest.clientIDSource);
                            Set<String> removeLines = new HashSet<String>();
                            removeLines.add(String.format("%d follow request", pRequest.clientIDDestination));
                            sourceDirectory.GetFile(sourceDirectory.GetLocalNotificationsName()).RemoveFile(removeLines, this);

                            // Add the follow relationship
                            sourceNode.AddFollower(targetNode);
                            targetNode.AddFollowing(sourceNode);

                            // Update the SocialGraph.txt file
                            updateSocialGraphFile();
                        }
                    }
                    break;
                    case FOLLOW_REQUEST_REJECT:
                    {
                        PayloadClientRequest pRequest = (PayloadClientRequest)connectionMessage.payload;

                        // Get source node
                        SocialGraphNode sourceNode = this.server.GetSocialGraph().GetUserNode(pRequest.clientIDSource);

                        if (sourceNode != null) {
                            Directory sourceDirectory = this.server.GetDirectory(pRequest.clientIDSource);
                            Set<String> removeLines = new HashSet<String>();
                            removeLines.add(String.format("%d follow request", pRequest.clientIDDestination));
                            sourceDirectory.GetFile(sourceDirectory.GetLocalNotificationsName()).RemoveFile(removeLines, this);
                        }
                    }
                    break;
                    case FOLLOW_REQUEST:
                    {
                        PayloadClientRequest pRequest = (PayloadClientRequest)connectionMessage.payload;

                        // System.out.println("DEBUG: Processing follow request from " + pRequest.clientIDSource + 
                        //                   " to " + pRequest.clientIDDestination);

                        // Follow request goes to the targetNode's directory Others_31clientID
                        Directory targetDirectory = this.server.GetDirectory(pRequest.clientIDDestination);

                        if (targetDirectory != null) {
                            // Ensure the Others file exists
                            String othersFileName = targetDirectory.GetLocalNotificationsName();
                            targetDirectory.SetFile(othersFileName);

                            ArrayList<String> appendLines = new ArrayList<String>();
                            appendLines.add(String.format("%d follow request", pRequest.clientIDSource));

                            _File othersFile = targetDirectory.GetFile(othersFileName);
                            if (othersFile != null) {
                                othersFile.AppendFile(appendLines, this);
                                // System.out.println("DEBUG: Follow request successfully added to server directory");
                            } else {
                                System.err.println("Could not create/access Others file for user " + pRequest.clientIDDestination);
                            }
                        } else {
                            System.err.println("Target directory not found for user " + pRequest.clientIDDestination);
                        }
                    }
                    break;
                    case UNFOLLOW:
                    {
                        PayloadClientRequest pClientRequest = (PayloadClientRequest)connectionMessage.payload;

                        SocialGraph socialGraph = this.server.GetSocialGraph();

                        SocialGraphNode source = socialGraph.GetUserNode(pClientRequest.clientIDSource);
                        SocialGraphNode dest = socialGraph.GetUserNode(pClientRequest.clientIDDestination);

                        if (source != null && dest != null) {
                            source.RemoveFollowing(dest);
                            dest.RemoveFollower(source);

                            // Update the SocialGraph.txt file
                            updateSocialGraphFile();
                        }

                        // Return social graph
                        Message response = new Message();
                        response.type = MessageType.UNFOLLOW;
                        PayloadClientGraph pClientGraph = new PayloadClientGraph();
                        response.payload = pClientGraph;

                        pClientGraph.followings = new ArrayList<UserAccountData>();

                        if (source != null) {
                            Set<Integer> followingIDs = source.GetFollowingIDs();

                            for(Integer followingID : followingIDs) {
                                String userName = this.server.GetUserName(followingID);
                                pClientGraph.followings.add(new UserAccountData(followingID, userName));
                            }
                        }

                        this.oStream.writeObject(response);
                        this.oStream.flush();
                    }
                    break;
                    case GET_CLIENT_GRAPH:
                    {
                        int userID = ((PayloadUserID)connectionMessage.payload).clientID;

                        Message response = new Message();
                        response.type = MessageType.GET_CLIENT_GRAPH;
                        PayloadClientGraph pGraph = new PayloadClientGraph();
                        response.payload = pGraph;

                        pGraph.followers = new ArrayList<UserAccountData>();
                        pGraph.followings = new ArrayList<UserAccountData>();

                        SocialGraphNode userNode = this.server.GetSocialGraph().GetUserNode(userID);

                        if (userNode != null) {
                            Set<Integer> followerIDs = userNode.GetFollowerIDs();
                            Set<Integer> followingIDs = userNode.GetFollowingIDs();

                            for (Integer followerID : followerIDs) {
                                String followerName = this.server.GetUserName(followerID);
                                pGraph.followers.add(new UserAccountData(followerID, followerName));
                            }

                            for (Integer followingID : followingIDs) {
                                String followingName = this.server.GetUserName(followingID);
                                pGraph.followings.add(new UserAccountData(followingID, followingName));
                            }
                        }

                        this.oStream.writeObject(response);
                        this.oStream.flush();
                    }
                    break;
                    case GET_NOTIFICATIONS:
                    {
                        PayloadUserID pUserID = (PayloadUserID)connectionMessage.payload;
                        int userID = pUserID.clientID;
                        SocialGraphNode userNode = this.server.GetSocialGraph().GetUserNode(userID);

                        Message responseMessage = new Message();
                        responseMessage.type = MessageType.GET_NOTIFICATIONS;
                        PayloadText pNotifications = new PayloadText();
                        responseMessage.payload = pNotifications;

                        StringBuilder notificationText = new StringBuilder();

                        if (userNode != null) {
                            // Get posts from people this user follows
                            Set<Integer> followingsIDs = userNode.GetFollowingIDs();

                            for (Integer ID : followingsIDs) {
                                Directory followingDirectory = this.server.GetDirectory(ID);
                                String notifications = followingDirectory.GetNotifications(this);

                                if (notifications != null && notifications.trim().length() > 0) {
                                    String[] lines = notifications.split("\n");
                                    for (String line : lines) {
                                        if (line.trim().contains("posted")) {
                                            notificationText.append(line.trim()).append("\n");
                                        }
                                    }
                                }
                            }

                            // Get user's own notifications (including follow requests) - READ FROM SERVER DIRECTORY
                            Directory userDirectory = this.server.GetDirectory(userID);
                            String userNotifications = userDirectory.GetNotifications(this);

                            // System.out.println("DEBUG: User " + userID + " notifications content: " + userNotifications);

                            if (userNotifications != null && userNotifications.trim().length() > 0) {
                                String[] userNotificationLines = userNotifications.split("\n");

                                for (String notification : userNotificationLines) {
                                    if (notification != null && notification.trim().length() > 0) {
                                        // Add all notifications (posts and follow requests)
                                        notificationText.append(notification.trim()).append("\n");
                                    }
                                }
                            }
                        }

                        pNotifications.text = notificationText.toString();
                        // System.out.println("DEBUG: Sending notifications to client " + userID + ": " + pNotifications.text);

                        this.oStream.writeObject(responseMessage);
                        this.oStream.flush();
                    }
                    break;
                    case ACCESS_PROFILE:
                    {
                        PayloadClientRequest pRequest = (PayloadClientRequest)connectionMessage.payload;

                        // Check if the requesting client follows the target client
                        SocialGraphNode requestingNode = this.server.GetSocialGraph().GetUserNode(pRequest.clientIDSource);
                        boolean isFollowing = false;

                        if (requestingNode != null) {
                            Set<Integer> followingIDs = requestingNode.GetFollowingIDs();
                            isFollowing = followingIDs.contains(pRequest.clientIDDestination);
                        }

                        PayloadText pText = new PayloadText();

                        if (!isFollowing) {
                            pText.text = "Access Profile Denied Reason: not following specified client";
                        } else {
                            Directory userDirectory = this.server.GetDirectory(pRequest.clientIDDestination);

                            if (userDirectory != null) {
                                String profileContent = userDirectory.tryGetProfile();

                                if (profileContent == null) {
                                    pText.text = "Access Profile Denied Reason: profile file not found";
                                } else if ("LOCKED".equals(profileContent)) {
                                    pText.text = "The file is locked! Profile is currently being accessed by another process.";
                                    System.out.println("Client " + pRequest.clientIDSource + " attempted to access locked profile of client " + pRequest.clientIDDestination);
                                } else {
                                    pText.text = profileContent;
                                    System.out.println("Client " + pRequest.clientIDSource + " accessed profile of client " + pRequest.clientIDDestination);
                                }
                            } else {
                                pText.text = "Access Profile Denied Reason: user directory not found";
                            }
                        }

                        this.oStream.writeObject(pText);
                        this.oStream.flush();
                    }
                    break;
                    case UPLOAD:
                    {
                        PayloadUpload pUpload = (PayloadUpload)connectionMessage.payload;

                        try {
                            // Write to the correct directory append to clientProfile also
                            Directory clientDirectory = this.server.GetDirectory(pUpload.clientID);

                            // Save image
                            clientDirectory.SetFile(pUpload.imageName);
                            clientDirectory.GetFile(pUpload.imageName).WriteFile(pUpload.imageData, this);

                            // Save multilingual text with proper formatting
                            String textContent = "";

                            if (pUpload.multiLingualText.hasText(Language.ENGLISH) && pUpload.multiLingualText.hasText(Language.GREEK)) {
                                // Both languages available
                                textContent = pUpload.multiLingualText.getFormattedText(Language.BOTH);
                            } else if (pUpload.multiLingualText.hasText(Language.ENGLISH)) {
                                // English only
                                textContent = "[EN]\n" + pUpload.multiLingualText.getText(Language.ENGLISH);
                            } else if (pUpload.multiLingualText.hasText(Language.GREEK)) {
                                // Greek only
                                textContent = "[EL]\n" + pUpload.multiLingualText.getText(Language.GREEK);
                            } else {
                                textContent = pUpload.acompanyingText != null ? pUpload.acompanyingText : "";
                            }

                            // Save the text file
                            clientDirectory.SetFile(pUpload.textName);
                            clientDirectory.GetFile(pUpload.textName).WriteFile(textContent, this);

                            // Update profile of the uploader
                            ArrayList<String> appendList = new ArrayList<String>();
                            appendList.add(String.format("%d posted %s", pUpload.clientID, pUpload.imageName));
                            clientDirectory.GetFile(clientDirectory.GetLocalProfileName()).AppendFile(appendList, this);

                            // Record this post in the Others_31clientID.txt files of all followers
                            SocialGraphNode uploaderNode = this.server.GetSocialGraph().GetUserNode(pUpload.clientID);
                            if (uploaderNode != null) {
                                Set<Integer> followerIDs = uploaderNode.GetFollowerIDs();

                                for (Integer followerID : followerIDs) {
                                    try {
                                        Directory followerDirectory = this.server.GetDirectory(followerID);
                                        if (followerDirectory != null) {
                                            // Add the post notification to follower's Others file
                                            ArrayList<String> followerNotification = new ArrayList<String>();
                                            followerNotification.add(String.format("%d posted %s", pUpload.clientID, pUpload.imageName));

                                            _File followerOthersFile = followerDirectory.GetFile(followerDirectory.GetLocalNotificationsName());
                                            if (followerOthersFile != null) {
                                                followerOthersFile.AppendFile(followerNotification, this);
                                            }
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Error updating follower " + followerID + " notifications: " + e.getMessage());
                                        // Continue with other followers even if one fails
                                    }
                                }

                                System.out.println("Post recorded for " + followerIDs.size() + " followers");
                            }
                        } catch (Exception e) {
                            System.err.println("ERROR: Upload failed for client " + pUpload.clientID + ": " + e.getMessage());
                        }
                    }
                    break;
                    case DOWNLOAD_PHOTO:
                    {
                        PayloadDownload pDownload = (PayloadDownload)connectionMessage.payload;
                        int srcClientID = pDownload.clientID;
                        String photoName = pDownload.name;
                        String descriptionTextLocalPath = photoName.split("\\.")[0] + ".txt";

                        // Reply from which client it gets the image
                        Message responseMessage = new Message();
                        responseMessage.type = MessageType.DOWNLOAD_PHOTO;
                        PayloadDownload pResponse = new PayloadDownload();
                        responseMessage.payload = pResponse;

                        // Find client with phtoName
                        SocialGraphNode srcClientNode = this.server.GetSocialGraph().GetUserNode(srcClientID); 
                        Set<Integer> followings = srcClientNode.GetFollowingIDs();

                        _File photoFile = null;
                        _File descriptionFile = null;
                        int directoryClientID = 0;
                        
                        // get photoFile that satisfies the description language constraint 
                        for (Integer followingID : followings) {
                            descriptionFile = this.server.GetDirectory(followingID).GetFile(descriptionTextLocalPath);
                            
                            if(descriptionFile == null) continue;
                            
                            String[] descriptionTextLines = ((String)descriptionFile.ReadFile(this)).split("\n"); 
                            
                            Set<String> languageTags = new HashSet<String>();
                            
                            for(int i = 0; i < descriptionTextLines.length; ++i) {
                                if(descriptionTextLines[i].equals("[EN]") || descriptionTextLines[i].equals("[EL]")) {
                                    languageTags.add(descriptionTextLines[i]);
                                }
                            }
                            
                            // assume descriptions always have at least 1 language tag eg. [EN]
                            
                            if(pDownload.preferredLanguage != Language.BOTH) {
                                if(!languageTags.contains("[" + (pDownload.preferredLanguage.getCode().toUpperCase()) + "]")) {
                                    continue;
                                }
                            }
                            
                            photoFile = this.server.GetDirectory(followingID).GetFile(photoName);
                            directoryClientID = followingID;

                            if (photoFile != null) break;
                        }

                        if(photoFile == null) {
                            // File does not exist in following users thus return rejection
                            Message rejectionMessage = new Message();
                            rejectionMessage.type = MessageType.ERROR;
                            rejectionMessage.payload = null;

                            this.oStream.writeObject(rejectionMessage);
                            this.oStream.flush();
                            break;
                        }

                        // Image search successful
                        Message message1 = new Message();
                        message1.type = MessageType.SYN_ACK;
                        PayloadDownload pDownloadResult = new PayloadDownload();
                        message1.payload = pDownloadResult;
                        pDownloadResult.name = photoName;
                        pDownloadResult.clientID = directoryClientID;
                        System.out.println("Sending Image Parameters");
                        this.oStream.writeObject(message1);
                        this.oStream.flush();

                        // Ask permission from image owner and block wait for response
                        // [userID] approval request [photoName]

                        ArrayList<String> ask = new ArrayList<String>();
                        ask.add(Integer.toString(srcClientID) + " approval request " + photoName);

                        Directory imageOwnerDirectory = this.server.GetDirectory(directoryClientID);
                        imageOwnerDirectory.GetFile(imageOwnerDirectory.GetLocalNotificationsName()).AppendFile(ask, this);

                        // 3-way handshake
                        Message syn = (Message)this.iStream.readObject();
                        System.out.println("Syn");

                        if(syn.type == MessageType.ERROR) {
                            // Download not approved
                            break;
                        }

                        // Validate the SYN message
                        if (syn.type != MessageType.SYN) {
                            System.err.println("Expected SYN message but received: " + syn.type);
                            // Handle error appropriately
                            Message errorMessage = new Message();
                            errorMessage.type = MessageType.ERROR;
                            this.oStream.writeObject(errorMessage);
                            this.oStream.flush();
                            break;
                        }

                        Message synAck = new Message();
                        synAck.type = MessageType.SYN_ACK;

                        System.out.println("SynAck");
                        this.oStream.writeObject(synAck);
                        this.oStream.flush();

                        System.out.println("Ack");
                        Message ack = (Message)this.iStream.readObject();
                        int timeout = (int)ack.payload;

                        System.out.println("Timeout: " + Integer.toString(timeout));
                        this.connectionSocket.setSoTimeout(timeout);

                        Message timeoutMessage = new Message();

                        System.out.println("Sending Image Parameters");
                        this.oStream.writeObject(timeoutMessage);
                        this.oStream.flush();
 
                        // Contains buffered Image serialized so when we deserialize we need to do ImageIO.write
                        byte[] serializedBytes = (byte[])photoFile.ReadFile(this);

                        // Retransmission
                        try {
                            System.out.println("Waiting for ACK");
                            this.iStream.readObject();
                        } catch (SocketTimeoutException e) {
                            // Retransmission
                            System.out.println("Server did not receive ACK");

                            Message timeoutMessage2 = new Message();
                            timeoutMessage2.payload = (int)serializedBytes.length;
                            System.out.println("Retransmit Image Parameters");

                            this.oStream.writeObject(timeoutMessage2);
                            this.oStream.flush();

                            // Reset timeout
                            this.connectionSocket.setSoTimeout(0);
                        }

                        System.out.println("ACK Image Parameters");
                        // Wait for ACK to start the Image transmission
                        Message response3 = (Message)this.iStream.readObject();

                        int i = 0;
                        int serializedBytesOffset = 0;

                        int imageBytes = (int)serializedBytes.length;;

                        // Break image into 10 pieces
                        int ne = (int)((float)imageBytes / 10.0f);
                        int neFinalPacket = imageBytes - 9 * _ceil((float)imageBytes / 10.0f);

                        int SEQ = 0;
                        int windowSize = 3;
                        int MAX_SEQ = 8;
                        int window[] = new int[windowSize];

                        // initialize window
                        window[SEQ] = SEQ;
                        window[SEQ + 1] = SEQ + 1;
                        window[SEQ + 2] = SEQ + 2;

                        this.connectionSocket.setSoTimeout(10000);

                        while (i < 10) {
                            System.out.println("i: " + Integer.toString(i));
                            
                            try {
                                ResponseAPDU responseAPDU = (ResponseAPDU)this.iStream.readObject();

                                ByteArrayInputStream bis = new ByteArrayInputStream(responseAPDU.responseData);
                                ObjectInputStream is = new ObjectInputStream(bis);

                                int ACK = is.readInt();
                                System.out.printf("Received ResponseAPDU ACK=%d...\n", ACK);

                                // Success
                                if(responseAPDU.sw1sw2 == 0x9000) {
                                    if(ACK == window[0]) {
                                        // packet sent ACK resonse matches the first packet in the window
                                        // scroll the window
                                        window[0] = window[1];
                                        window[1] = window[2];
                                        window[2] = (window[2] + 1) % MAX_SEQ;

                                        i++;

                                    } else {
                                        // the packet was dropped
                                    }
                                }

                            } catch(SocketTimeoutException e) {

                                SEQ = window[0];

                                int k = 0;

                                int m = windowSize;

                                if(i == 0) {
                                    m = 4;
                                }

                                while(k < m && (i + k) < 10) {
                                    CommandAPDU commandAPDU = new CommandAPDU();
                                    
                                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                    ObjectOutputStream os = new ObjectOutputStream(bos);

                                    os.writeInt(SEQ);
                                    os.flush();

                                    int len;
                                    if(i + k == 9) {
                                        len = neFinalPacket;
                                    } else {
                                        len = ne;
                                    }

                                    commandAPDU.nc = 0;
                                    commandAPDU.ne = (short)(len + bos.toByteArray().length);

                                    byte[] data = new byte[ne];

                                    serializedBytesOffset = (i + k) * ne;

                                    System.arraycopy(serializedBytes, serializedBytesOffset, data, 0, len);
                                    
                                    os.write(data);
                                    os.flush();

                                    commandAPDU.commandData = bos.toByteArray();
                                    
                                    System.out.printf("Sending CommandAPDU SEQ=%d...\n", SEQ);
                                    this.oStream.writeObject(commandAPDU);
                                    this.oStream.flush();

                                    k++;
                                    SEQ = (SEQ + 1) % MAX_SEQ;
                                }
                            }
                        }

                        this.connectionSocket.setSoTimeout(0);

                        System.out.println("Finished!");

                        PayloadText pText = new PayloadText();
                        String[] tokens = photoName.split("\\.");
                        String textFileName = tokens[0] + ".txt";
                        _File textFile = null;

                        try {
                            textFile = this.server.GetDirectory(directoryClientID).GetFile(textFileName);
                            String textLines[] = ((String)textFile.ReadFile(this)).split("\n");

                            if(textLines.length == 0) {
                                pText.text = null;
                            }
                            else {
                                pText.text = "";
                            }

                            // get prefered language
                            for(int j = 0; j < textLines.length; j++) {
                                if(textLines[j].equals("[" + (pDownload.preferredLanguage.getCode().toUpperCase()) + "]")) {
                                    
                                    pText.text += textLines[j] + "\n";

                                    if(j + 1 >= textLines.length) break;
                                    
                                    int m = j + 1;

                                    while(m < textLines.length && !textLines[m].equals("[EL]") && !textLines[m].equals("[EN]")) {
                                        pText.text += textLines[m] + "\n";
                                        m++;
                                    }
                                    
                                    j = m;
                                }
                            }

                        } catch (Exception e) {
                            pText.text = null;
                        }

                        this.oStream.writeObject(pText);
                        this.oStream.flush();

                        // Write image and accompaying text to Server Directory also of the client
                        Directory srcUserDirectory = this.server.GetDirectory(srcClientID);
                        srcUserDirectory.SetFile(textFileName);

                        if (textFile != null) {
                            srcUserDirectory.SetFile(textFileName);
                            srcUserDirectory.GetFile(textFileName).WriteFile(pText.text, this);
                        }

                        // Photo file is not null
                        srcUserDirectory.SetFile(photoName);
                        srcUserDirectory.GetFile(photoName).WriteFile(serializedBytes, this);

                        this.server.AddDownload(photoFile.GetGlobalFilePath());
                    }
                    break;
                    case SYNCHRONIZE_DIRECTORY:
                    {
                        PayloadDirectory pDirectory = (PayloadDirectory)connectionMessage.payload;

                        // Get users directory and compute FileDatas
                        Directory clientDirectory = this.server.GetDirectory(pDirectory.clientID);
                        ArrayList<String> unsynchronizedFilePaths = clientDirectory.ComputeUnsynchronizedFilePaths(pDirectory.fileDatas);

                        this.oStream.writeObject(unsynchronizedFilePaths);
                        this.oStream.flush();
                    }
                    break;
                    case GET_FILE_CONTENTS:
                    {
                        PayloadDownload pDownload = (PayloadDownload)connectionMessage.payload;

                        Object contents = this.server.GetDirectory(pDownload.clientID).GetFile(pDownload.name).ReadFile(this);

                        this.oStream.writeObject(contents);
                        this.oStream.flush();
                    }
                    break;
                    case ASK_COMMENT:
                    {
                        PayloadClientRequest pRequest = (PayloadClientRequest)connectionMessage.payload;

                        Message responseMessage = new Message();
                        PayloadText pText = new PayloadText();
                        responseMessage.payload = pText;


                        SocialGraphNode destClientNode = this.server.GetSocialGraph().GetUserNode(pRequest.clientIDDestination);
                        
                        if(destClientNode == null) {
                            pText.text = null;
                            this.oStream.writeObject(responseMessage);
                            this.oStream.flush();
                            break;
                        }

                        // get all photo names of destinationUser
                        Directory destClientDirectory = this.server.GetDirectory(pRequest.clientIDDestination);

                        FileData[] fileDatas = destClientDirectory.ComputeFileDatas();

                        pText.text = "";

                        if(fileDatas == null) {
                            this.oStream.writeObject(responseMessage);
                            this.oStream.flush();
                            break;
                        } 

                        for(int i = 0; i < fileDatas.length; ++i) {
                            if(fileDatas[i].filePath.endsWith(".png")) {
                                pText.text += fileDatas[i].filePath + "\n";
                            }
                        }

                        this.oStream.writeObject(responseMessage);
                        this.oStream.flush();

                        PayloadComment pComment = (PayloadComment)((Message)this.iStream.readObject()).payload;

                        // write to others profile of destinationClient only if the comment is in the same language as the post text
                        
                        // read accompanying text

                        String[] t = pComment.photoName.split("\\.");

                        System.out.println(t[0] + ".txt");

                        String photoText = (String)(destClientDirectory.GetFile(t[0] + ".txt").ReadFile(this));

                        // find languages

                        boolean isEN = photoText.indexOf("[EN]") != -1;
                        boolean isEL = photoText.indexOf("[EL]") != -1;
                        
                        // if the comment has a language that is not in the file reject do not write anything

                        if((pComment.comment.hasText(Language.ENGLISH) && !isEN)
                        || (pComment.comment.hasText(Language.GREEK) && !isEL)) {
                            // disaproved do not write anything
                            break;
                        }
                        
                        String comment = Integer.toString(pRequest.clientIDSource) + " commented " + "[" + pComment.comment.getOneLineText(Language.BOTH) + "] " + pComment.photoName + " " + Integer.toString(pRequest.clientIDDestination);
                        ArrayList<String> lines = new ArrayList<String>();
                        lines.add(comment);

                        System.out.println(comment);

                        destClientDirectory.GetFile(destClientDirectory.GetLocalNotificationsName()).AppendFile(lines, this);
                    }
                    break;
                    case APPROVE_COMMENT:
                    {
                        PayloadClientRequest pRequest = (PayloadClientRequest)connectionMessage.payload;

                        PayloadApproveComment pComment = (PayloadApproveComment)((Message)this.iStream.readObject()).payload;

                        ArrayList<String> comment = new ArrayList<String>();
                        comment.add(pComment.comment);

                        Set<String> comment2 = new HashSet<String>();
                        comment2.add(comment.get(0));

                        // delete from source
                        Directory srcDirectory = this.server.GetDirectory(pRequest.clientIDSource);
                        srcDirectory.GetFile(srcDirectory.GetLocalNotificationsName()).RemoveFile(comment2, this);

                        if(pComment.isApproved) {
                            // add to destination others profile(the user that commented)
                            Directory destDirectory = this.server.GetDirectory(pRequest.clientIDDestination);
                            destDirectory.GetFile(destDirectory.GetLocalProfileName()).AppendFile(comment, this);
                        }
                    }
                    break;
                    case PERMIT_PHOTO_ACCESS:
                    {
                        // [userID] approval request [photoName]
                        PayloadComment pText = (PayloadComment)connectionMessage.payload;

                        String text = pText.photoName;

                        Set<String> lines = new HashSet<String>();
                        lines.add(text);

                        Directory userDirectory = this.server.GetDirectory(this.clientID);
                        userDirectory.GetFile(userDirectory.GetLocalNotificationsName()).RemoveFile(lines, this);

                        String[] tokens = text.split(" ");
                        int approvedClientID = Integer.parseInt(tokens[0]);

                        String approval = "disapproved";

                        if(pText.isApproved) {
                            approval = "approved";
                        }

                        String message = "Client: " + Integer.toString(this.clientID) + " " + approval + " access permission for image " + tokens[3];
                        this.server.Log(approvedClientID, message);
                    }
                    break;
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

    private void updateSocialGraphFile() {
        this.server.updateSocialGraphFile();
    }

    int _ceil(float x) {
        return (int) Math.ceil(x);
    }

/*
    // Helper method to ServerActions.java
    private void updateSocialGraphFile() {
        try {
            StringBuilder graphContent = new StringBuilder();

            // Write all users in the social graph
            for (int userID = 0; userID < this.server.GetUserCount() + 1; userID++) {
                SocialGraphNode userNode = this.server.GetSocialGraph().GetUserNode(userID);
                if (userNode != null) {
                    graphContent.append(userID);

                    // Add follower IDs
                    Set<Integer> followerIDs = userNode.GetFollowerIDs();
                    for (Integer followerID : followerIDs) {
                        graphContent.append(" ").append(followerID);
                    }

                    if (userID < this.userCount) {
                        graphContent.append("\n");
                    }
                }
            }

            // Write to SocialGraph.txt file
            String graphFilePath = this.server.GetServerDirectoryPath() + "SocialGraph.txt";
            try (FileWriter writer = new FileWriter(graphFilePath)) {
                writer.write(graphContent.toString());
            }

            System.out.println("SocialGraph.txt updated");

        } catch (Exception e) {
            System.err.println("Error updating SocialGraph.txt: " + e.getMessage());
        }
    }
*/
}
