import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import Messages.PayloadClientGraph;
import Messages.PayloadClientRequest;
import Messages.PayloadCredentials;
import Messages.PayloadDownload;
import Messages.PayloadText;
import Messages.PayloadUpload;
import Messages.PayloadUserID;
import POD.UserAccountData;


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

            //PayloadUserID pUser = (PayloadUserID)((Message)this.iStream.readObject()).payload;

            //Thread.currentThread().setName(Integer.toString(pUser.clientID));

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

                        oStream.writeObject(loginResponse);
                        oStream.flush();
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
                            Directory sourceDirectory = this.server.GetDirectory(pRequest.clientIDSource);
                            Set<String> removeLines = new HashSet<String>();
                            removeLines.add(String.format("%d follow request", pRequest.clientIDDestination));
                            sourceDirectory.GetFile(sourceDirectory.GetLocalNotificationsName()).RemoveFile(removeLines);

                            sourceNode.AddFollower(targetNode);
                            targetNode.AddFollowing(sourceNode);
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
                            sourceDirectory.GetFile(sourceDirectory.GetLocalNotificationsName()).RemoveFile(removeLines);
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
                                othersFile.AppendFile(appendLines);
                                // System.out.println("DEBUG: Follow request successfully added to server directory");

                                // Send a follow request message back to the client
                                Message responseMessage = new Message();
                                responseMessage.type = MessageType.FOLLOW_REQUEST;
                                responseMessage.payload = null;

                                this.oStream.writeObject(responseMessage);
                                this.oStream.flush();
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
                                String notifications = followingDirectory.GetNotifications();

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
                            String userNotifications = userDirectory.GetNotifications();

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
                        Directory userDirectory = this.server.GetDirectory(pRequest.clientIDDestination);

                        PayloadText pText = new PayloadText();
                        pText.text = "Access Profile Denied Reason: not following specified client";

                        if (userDirectory != null) {
                            pText.text = userDirectory.GetProfile();
                        }

                        this.oStream.writeObject(pText);
                        this.oStream.flush();
                    }
                    break;
                    case UPLOAD:
                    {
                        PayloadUpload pUpload = (PayloadUpload)connectionMessage.payload;

                        // Write to the correct directory append to clientProfile also
                        Directory clientDirectory = this.server.GetDirectory(pUpload.clientID);

                        // Save image
                        clientDirectory.SetFile(pUpload.imageName);
                        clientDirectory.GetFile(pUpload.imageName).WriteFile(pUpload.imageData);

                        // Save multilingual text with proper formatting
                        String textContent = "";

                        if (pUpload.hasText(Language.ENGLISH) && pUpload.hasText(Language.GREEK)) {
                            // Both languages available
                            textContent = pUpload.getFormattedText(Language.BOTH);
                        } else if (pUpload.hasText(Language.ENGLISH)) {
                            // English only
                            textContent = "[EN]\n" + pUpload.getText(Language.ENGLISH);
                        } else if (pUpload.hasText(Language.GREEK)) {
                            // Greek only
                            textContent = "[EL]\n" + pUpload.getText(Language.GREEK);
                        } else {
                            textContent = pUpload.acompanyingText != null ? pUpload.acompanyingText : "";
                        }

                        // Save the text file
                        clientDirectory.SetFile(pUpload.textName);
                        clientDirectory.GetFile(pUpload.textName).WriteFile(textContent);

                        // Update profile of the uploader
                        ArrayList<String> appendList = new ArrayList<String>();
                        appendList.add(String.format("%d posted %s", pUpload.clientID, pUpload.imageName));
                        clientDirectory.GetFile(clientDirectory.GetLocalProfileName()).AppendFile(appendList);

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
                                            followerOthersFile.AppendFile(followerNotification);
                                        }
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error updating follower " + followerID + " notifications: " + e.getMessage());
                                    // Continue with other followers even if one fails
                                }
                            }

                            System.out.println("Post recorded for " + followerIDs.size() + " followers");
                        }
                    }
                    break;
                    case DOWNLOAD_PHOTO:
                    {
                        PayloadDownload pDownload = (PayloadDownload)connectionMessage.payload;
                        int srcClientID = pDownload.clientID;
                        String photoName = pDownload.name;

                        // Reply from which client it gets the image
                        Message responseMessage = new Message();
                        responseMessage.type = MessageType.DOWNLOAD_PHOTO;
                        PayloadDownload pResponse = new PayloadDownload();
                        responseMessage.payload = pResponse;

                        // Find client with phtoName
                        SocialGraphNode srcClientNode = this.server.GetSocialGraph().GetUserNode(srcClientID); 
                        Set<Integer> followings = srcClientNode.GetFollowingIDs();

                        _File photoFile = null;
                        int directoryClientID = 0;

                        for (Integer followingID : followings) {
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

                        // 3-way handshake
                        System.out.println("Syn");
                        Message syn = (Message)this.iStream.readObject();

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
                        PayloadDownload pDownload3 = (PayloadDownload)ack.payload;

                        System.out.println("Timeout: " + Integer.toString(pDownload3.timeout));
                        this.connectionSocket.setSoTimeout(pDownload3.timeout);

                        Message timeoutMessage = new Message();

                        System.out.println("Sending Image Parameters");
                        this.oStream.writeObject(timeoutMessage);
                        this.oStream.flush();
 
                        // Contains buffered Image serialized so when we deserialize we need to do ImageIO.write
                        byte[] serializedBytes = (byte[])photoFile.ReadFile();

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

                        // Validate the ACK message
                        if (response3.type != MessageType.ACK) {
                            System.err.println("Expected ACK message but received: " + response3.type);
                            // Handle error appropriately
                            Message errorMessage = new Message();
                            errorMessage.type = MessageType.ERROR;
                            this.oStream.writeObject(errorMessage);
                            this.oStream.flush();
                            break;
                        }

                        int i = 0;
                        int serializedBytesOffset = 0;

                        while (i < 10) {
                            System.out.println("i: " + Integer.toString(i));
                            CommandAPDU commandAPDU = null;

                            try {
                                System.out.println("Reading CommandAPDU...");
                                commandAPDU = (CommandAPDU)this.iStream.readObject();
                            } catch (SocketTimeoutException e) {
                                this.connectionSocket.setSoTimeout(0);
                                this.iStream.readObject();
                                // Ignore CommandAPDU receive the second one
                            }

                            if (i == 3) {
                                System.out.println("Reading CommandAPDU...");
                                commandAPDU = (CommandAPDU)this.iStream.readObject();
                            }

                            if (commandAPDU.nc > 0) {
                                // We have received timeout
                                ByteArrayInputStream baoo = new ByteArrayInputStream(commandAPDU.commandData);
                                ObjectInputStream oi = new ObjectInputStream(baoo);
                                this.connectionSocket.setSoTimeout(oi.readInt());
                            }

                            ResponseAPDU responseAPDU = new ResponseAPDU();
                            responseAPDU.sw1sw2 = 0x9000;
                            responseAPDU.responseData = new byte[commandAPDU.ne];

                            System.arraycopy(serializedBytes, serializedBytesOffset, responseAPDU.responseData, 0, commandAPDU.ne);
                            serializedBytesOffset += commandAPDU.ne;

                            System.out.println("Sending ResponseAPDU...");
                            this.oStream.writeObject(responseAPDU);
                            this.oStream.flush();
                            i++;
                        }

                        System.out.println("Finished!");

                        PayloadText pText = new PayloadText();
                        String[] tokens = photoName.split("\\.");
                        String textFileName = tokens[0] + ".txt";
                        _File textFile = null;

                        try {
                            textFile = this.server.GetDirectory(directoryClientID).GetFile(textFileName);
                            if (textFile != null) {
                                String fullText = (String)textFile.ReadFile();

                                // Send the full text - client will handle language preference
                                pText.text = fullText;
                            } else {
                                pText.text = null;
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
                            srcUserDirectory.GetFile(textFileName).WriteFile(pText.text);
                        }

                        // Photo file is not null
                        srcUserDirectory.SetFile(photoName);
                        srcUserDirectory.GetFile(photoName).WriteFile(serializedBytes);
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

                        Object contents = this.server.GetDirectory(pDownload.clientID).GetFile(pDownload.name).ReadFile();

                        this.oStream.writeObject(contents);
                        this.oStream.flush();
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
}
