import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileSystemNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import POD.FileData;

// Virtual File System
public class Directory {

    private int clientID;
    private String rootPath;
    private DirectoryNode rootFolder;

    private ConcurrentHashMap<String, _File> localFilePathToFile;

    public synchronized ArrayList<String> ComputeUnsynchronizedFilePaths(FileData[] localDirectoryFileData) {
        ArrayList<String> filesPathsUnsynchronized = new ArrayList<String>();

        if(localDirectoryFileData == null) return filesPathsUnsynchronized;
        if(localDirectoryFileData.length == 0) return filesPathsUnsynchronized;

        Set<FileData> localDirectoryFileDataSet = new HashSet<FileData>();

        for(int i = 0; i < localDirectoryFileData.length; ++i) {
            localDirectoryFileDataSet.add(localDirectoryFileData[i]);
        }

        FileData[] remoteDirectoryFileDatas = ComputeFileDatas();

        for(FileData remoteFileData : remoteDirectoryFileDatas) {
            if(!localDirectoryFileDataSet.contains(remoteFileData)) {
                filesPathsUnsynchronized.add(remoteFileData.filePath);
            } else {
                FileData foundLocalFileData = new FileData("", new BigInteger("0"));

                // query all localFileDatas
                // guaranteed to find localFileData mathcing remoteFileData
                for(FileData localFileData : localDirectoryFileDataSet) {
                    if(localFileData.equals(remoteFileData)) {
                        foundLocalFileData = localFileData;
                        break;
                    }
                }
                // compare localFileData with remoteFileData checksums
                if(!foundLocalFileData.checksum.equals(remoteFileData.checksum)) {
                    filesPathsUnsynchronized.add(foundLocalFileData.filePath);
                }
            }
        }

        return filesPathsUnsynchronized;
    }

    public synchronized FileData[] ComputeFileDatas() {

        if(localFilePathToFile.size() == 0) return null;

        FileData[] fileDatas = new FileData[localFilePathToFile.size()];
        BigInteger[] checksums = new BigInteger[localFilePathToFile.size()];
        _File[] files = new _File[localFilePathToFile.size()];

        // linearize hashmap
        int i = 0;
        for(_File file : localFilePathToFile.values()) {
            files[i] = file;
            i++;
        }

        for(int j = 0; j < files.length; ++j) {
            checksums[j] = files[j].GetChecksum();
        }

        for(int j = 0; j < files.length; ++j) {
            fileDatas[j] = new FileData(files[j].GetLocalFilePath(), checksums[j]);
        }

        return fileDatas;
    }

    public BigInteger GetChecksum(String filePath) {
        return localFilePathToFile.get(filePath).GetChecksum();
    }

    public synchronized void SetFile(String fileName) {
        if(this.localFilePathToFile.get(fileName) != null) {
            // file already exists
            return;
        } 

        _File f = new _File(fileName, this.rootPath + fileName);
        this.localFilePathToFile.put(fileName, f);
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

    public Directory(String root, int clientID) {
        this.clientID = clientID;
        this.rootPath = root;
        CreateHierarchy(root);

        HashSet<String> filePaths = new HashSet<String>();
        rootFolder.GetFilePaths(filePaths, this.rootPath);
        this.localFilePathToFile = new ConcurrentHashMap<String, _File>();

        for(String filePath : filePaths) {
            this.localFilePathToFile.put(filePath, new _File(filePath, this.rootPath + filePath));
        }
        // PrintDirectory();
    }

    public String GetRootPath() {
        return this.rootPath;
    }

    private synchronized void CreateHierarchy(String root) {
        this.rootFolder = new DirectoryNode(root, 1, true);
    }

    public synchronized void PrintDirectory() {
        for(String localFilePath : this.localFilePathToFile.keySet()) {
            System.out.println(this.rootPath + localFilePath);
        }
    }

    // public synchronized void PrintHierarchy() {
    // 
    //     rootFolder.Print();
    // }

    public static void main(String[] args) {
        System.out.println("Directory Test");

        // Create a test directory
        String testPath = "src/main/resources/TestDirectory/";
        Directory testDir = new Directory(testPath, 999);

        // Print initial directory contents
        System.out.println("\nInitial directory contents:");
        testDir.PrintDirectory();

        // Create some files
        System.out.println("\nAdding test files...");
        testDir.SetFile("test1.txt");
        testDir.SetFile("test2.txt");
        testDir.SetFile("image.png");

        // Print updated directory contents
        System.out.println("\nUpdated directory contents:");
        testDir.PrintDirectory();

        // Test getting a file
        System.out.println("\nGetting file test1.txt:");
        _File file = testDir.GetFile("test1.txt");
        if (file != null) {
            System.out.println("File found: " + file.GetLocalFilePath());
        } else {
            System.out.println("File not found");
        }

        System.out.println("\nDirectory test completed");
    }
}
