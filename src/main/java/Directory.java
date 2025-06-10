import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import POD.FileData;

/*
 * Directory class represents a virtual file system for a user.
 */
public class Directory {

    private int clientID;
    private String rootPath;
    private DirectoryNode rootFolder;

    private ConcurrentHashMap<String, _File> localFilePathToFile;

    public synchronized ArrayList<String> ComputeUnsynchronizedFilePaths(FileData[] localDirectoryFileData) {
        ArrayList<String> filesPathsUnsynchronized = new ArrayList<String>();

        // Get server files
        FileData[] remoteDirectoryFileDatas = ComputeFileDatas();
        
        if (remoteDirectoryFileDatas == null || remoteDirectoryFileDatas.length == 0) {
            return filesPathsUnsynchronized;  // No files on server
        }

        // Handle case where client has no files
        if (localDirectoryFileData == null || localDirectoryFileData.length == 0) {
            // Client has no files, download everything from server
            for (FileData remoteFileData : remoteDirectoryFileDatas) {
                filesPathsUnsynchronized.add(remoteFileData.filePath);
            }
            return filesPathsUnsynchronized;
        }

        // Create a map of local files for faster lookup
        Set<String> localFilePaths = new HashSet<String>();
        Map<String, BigInteger> localFileChecksums = new HashMap<String, BigInteger>();

        for (FileData localFile : localDirectoryFileData) {
            localFilePaths.add(localFile.filePath);
            localFileChecksums.put(localFile.filePath, localFile.checksum);
        }

        // Check each server file
        for (FileData remoteFileData : remoteDirectoryFileDatas) {
            if (!localFilePaths.contains(remoteFileData.filePath)) {
                // File doesn't exist locally, need to download
                filesPathsUnsynchronized.add(remoteFileData.filePath);
            } else {
                // File exists locally, check if content is different
                BigInteger localChecksum = localFileChecksums.get(remoteFileData.filePath);
                if (localChecksum == null || !localChecksum.equals(remoteFileData.checksum)) {
                    // Content is different, need to download
                    filesPathsUnsynchronized.add(remoteFileData.filePath);
                }
            }
        }

        return filesPathsUnsynchronized;
    }

    public synchronized FileData[] ComputeFileDatas() {

        if (localFilePathToFile.size() == 0) return null;

        FileData[] fileDatas = new FileData[localFilePathToFile.size()];
        BigInteger[] checksums = new BigInteger[localFilePathToFile.size()];
        _File[] files = new _File[localFilePathToFile.size()];

        // Linearize hashmap
        int i = 0;
        for (_File file : localFilePathToFile.values()) {
            files[i] = file;
            i++;
        }

        for (int j = 0; j < files.length; ++j) {
            checksums[j] = files[j].GetChecksum();
        }

        for (int j = 0; j < files.length; ++j) {
            fileDatas[j] = new FileData(files[j].GetLocalFilePath(), checksums[j]);
        }

        return fileDatas;
    }

    public BigInteger GetChecksum(String filePath) {
        return localFilePathToFile.get(filePath).GetChecksum();
    }

    public synchronized void SetFile(String fileName) {
        if (this.localFilePathToFile.get(fileName) != null) {
            // File already exists
            return;
        } 

        _File f = new _File(fileName, this.rootPath + fileName);
        this.localFilePathToFile.put(fileName, f);
    }

    // Safe profile access method
    public String tryGetProfile() {
        _File file = localFilePathToFile.get("Profile_31client" + Integer.toString(this.clientID) + ".txt");
        if (file == null) {
            return null;
        }

        Object result = file.tryReadFile();
        if (result == null) {
            return "LOCKED";  // Special indicator
        }

        return (String) result;
    }

    // Safe notifications access method
    public String tryGetNotifications() {
        _File file = localFilePathToFile.get("Others_31client" + Integer.toString(this.clientID) + ".txt");
        if (file == null) {
            return null;
        }

        Object result = file.tryReadFile();
        if (result == null) {
            return "LOCKED";  // Special indicator
        }

        return (String) result;
    }

    public String GetNotifications() {
        _File file = localFilePathToFile.get("Others_31client" + Integer.toString(this.clientID) + ".txt");

        return (String)file.ReadFile();
    }

    public String GetProfile() {
        _File file = localFilePathToFile.get("Profile_31client" + Integer.toString(this.clientID) + ".txt");

        return (String)file.ReadFile();
    }

    public String GetLocalProfileName() {
        return "Profile_31client" + Integer.toString(this.clientID) + ".txt";
    }

    public String GetLocalNotificationsName() {
        return "Others_31client" + Integer.toString(this.clientID) + ".txt";
    }

    public _File GetFile(String localFileName) {        
        _File file = localFilePathToFile.get(localFileName);

        return file;
    }

    public Directory(String root, int clientID) throws IOException {
        this.clientID = clientID;
        this.rootPath = root;
        CreateHierarchy(root);

        HashSet<String> filePaths = new HashSet<String>();
        
        
        rootFolder.GetFilePaths(filePaths, this.rootPath);
        this.localFilePathToFile = new ConcurrentHashMap<String, _File>();
        
        for(String filePath : filePaths) {
            this.localFilePathToFile.put(filePath, new _File(filePath, this.rootPath + filePath));
        }
        
        if(clientID == -1) return; 

        File rootFolder = new File(root);
        File profileFile = new File(this.rootPath + this.GetLocalProfileName());
        File notificationFile = new File(this.rootPath + this.GetLocalNotificationsName());
        
        rootFolder.mkdir();
        
        if(profileFile.createNewFile()) {
            this.localFilePathToFile.put(this.GetLocalProfileName(), new _File(this.GetLocalProfileName(), this.rootPath + this.GetLocalProfileName()));
        }

        if(notificationFile.createNewFile()) {
            this.localFilePathToFile.put(this.GetLocalNotificationsName(), new _File(this.GetLocalNotificationsName(), this.rootPath + this.GetLocalNotificationsName()));
        }

        //PrintDirectory();
    }

    public String GetRootPath() {
        return this.rootPath;
    }

    private synchronized void CreateHierarchy(String root) {
        this.rootFolder = new DirectoryNode(root, 1, true);
    }

    public synchronized void PrintDirectory() {
        for (String localFilePath : this.localFilePathToFile.keySet()) {
            System.out.println(this.rootPath + localFilePath);
        }
    }

    // public synchronized void PrintHierarchy() {
    //     rootFolder.Print();
    // }
}
