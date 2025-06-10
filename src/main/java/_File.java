import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;


public class _File {
    private String localFilePath;
    private String globalFilePath;
    private boolean isDirty;
    private BigInteger checksum;

    // Lock for synchronizing access to the file
    private final ReentrantReadWriteLock rwLock;
    private final ReentrantReadWriteLock.ReadLock readLock;
    private final ReentrantReadWriteLock.WriteLock writeLock;
    
    // Timeout for lock acquisition (in seconds)
    private static final long LOCK_TIMEOUT = 30;

    public _File(String localFilePath, String globalFilePath) {
        this.localFilePath = localFilePath;
        this.globalFilePath = globalFilePath;
        this.isDirty = true;  // So that the first time we call get checksum it calculates it

        this.rwLock = new ReentrantReadWriteLock(true);
        this.readLock = rwLock.readLock();
        this.writeLock = rwLock.writeLock();
    }

    private boolean acquireReadLock() {
        try {
            return readLock.tryLock(LOCK_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean acquireWriteLock() {
        try {
            return writeLock.tryLock(LOCK_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void releaseReadLock() {
        if (rwLock.getReadHoldCount() > 0) {
            readLock.unlock();
        }
    }

    private void releaseWriteLock() {
        if (rwLock.isWriteLockedByCurrentThread()) {
            writeLock.unlock();
        }
    }

    // Non-blocking lock methods
    public boolean tryAcquireReadLock() {
        try {
            return readLock.tryLock(100, TimeUnit.MILLISECONDS);  // Very short timeout
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public boolean tryAcquireWriteLock() {
        try {
            return writeLock.tryLock(100, TimeUnit.MILLISECONDS);  // Very short timeout
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // Check if file is currently locked for writing
    public boolean isWriteLocked() {
        return rwLock.isWriteLocked();
    }

    // Check if file has any locks
    public boolean hasActiveLocks() {
        return rwLock.getReadLockCount() > 0 || rwLock.isWriteLocked();
    }

    // Safe read method that can fail gracefully
    public synchronized Object tryReadFile() {
        if (!tryAcquireReadLock()) {
            return null;  // Indicates file is locked
        }

        try {
            Object result = null;
            String fileExtension = this.GetFileExtension();

            if (fileExtension.equals("")) {
                throw new RuntimeException("File has no extension");
            }

            if (".txt".equals(fileExtension)) {
                StringBuilder resultBuilder = new StringBuilder();

                try (BufferedReader br = new BufferedReader(new FileReader(new File(this.globalFilePath)))) {
                    String line;
                    boolean firstLine = true;

                    while ((line = br.readLine()) != null) {
                        if (!firstLine) {
                            resultBuilder.append("\n");
                        }
                        resultBuilder.append(line);
                        firstLine = false;
                    }

                    result = resultBuilder.toString();
                } catch(IOException e) {
                    throw new RuntimeException("Failed to read text file: " + e.getMessage(), e);
                }
            } else {
                // Handle images if needed
                try {
                    BufferedImage bImage = ImageIO.read(new File(this.globalFilePath));
                    if (bImage == null) {
                        throw new RuntimeException("Failed to read image file - unsupported format or file doesn't exist");
                    }

                    ByteArrayOutputStream oStream = new ByteArrayOutputStream();
                    String formatName = fileExtension.substring(1);

                    if (!ImageIO.write(bImage, formatName, oStream)) {
                        throw new RuntimeException("Failed to write image in format: " + formatName);
                    }

                    result = oStream.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read image file: " + e.getMessage(), e);
                }
            }

            return result;
        } finally {
            releaseReadLock();
        }
    }

    public String GetLocalFilePath() {
        return this.localFilePath;
    }

    public String GetGlobalFilePath() {
        return this.globalFilePath;
    }

    public synchronized BigInteger GetChecksum() {
        if (!acquireWriteLock()) {
            throw new RuntimeException("Failed to acquire write lock for checksum calculation");
        }

        try {
            if (isDirty) {
                // Generate checksum
                MessageDigest messageDigest;
                byte[] fileBytes;
                try {
                    messageDigest = MessageDigest.getInstance("SHA-256");
                    fileBytes = this.getBytes();
                    messageDigest.update(fileBytes);
                    this.checksum = new BigInteger(1, messageDigest.digest()); // Use 1 for positive values
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException("SHA-256 algorithm not available", e);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read file for checksum", e);
                }

                isDirty = false;
            }

            return this.checksum;
        } finally {
            releaseWriteLock();
        }
    }

    private synchronized byte[] getBytes() throws IOException {
        return Files.readAllBytes(Paths.get(this.globalFilePath));
    }

    public String GetFileExtension() {
        int extensionIndex = globalFilePath.lastIndexOf("."); 

        if (extensionIndex != -1) {
            return globalFilePath.substring(extensionIndex);
        } else {
            return "";
        }
    }

    // Returns either a String with \n characters for txt files and for images a byte[] containing the Image 
    public synchronized Object ReadFile() {
        if (!acquireReadLock()) {
            throw new RuntimeException("Failed to acquire read lock");
        }

        try {
            Object result = null;
            String fileExtension = this.GetFileExtension();

            if (fileExtension.equals("")) {
                throw new RuntimeException("File has no extension");
            }

            if (".txt".equals(fileExtension)) {
                // It is a text file
                StringBuilder resultBuilder = new StringBuilder();
                
                try (BufferedReader br = new BufferedReader(new FileReader(new File(this.globalFilePath)))) {
                    String line;
                    boolean firstLine = true;

                    while ((line = br.readLine()) != null) {
                        if (!firstLine) {
                            resultBuilder.append("\n");
                        }
                        resultBuilder.append(line);
                        firstLine = false;
                    }

                    result = resultBuilder.toString();
                } catch(IOException e) {
                    throw new RuntimeException("Failed to read text file: " + e.getMessage(), e);
                }
            } else {
                // It is an image
                try {
                    BufferedImage bImage = ImageIO.read(new File(this.globalFilePath));
                    if (bImage == null) {
                        throw new RuntimeException("Failed to read image file - unsupported format or file doesn't exist");
                    }
                    
                    ByteArrayOutputStream oStream = new ByteArrayOutputStream();
                    String formatName = fileExtension.substring(1);
                    
                    if (!ImageIO.write(bImage, formatName, oStream)) {
                        throw new RuntimeException("Failed to write image in format: " + formatName);
                    }
                    
                    result = oStream.toByteArray();
                } catch (IOException e) {
                    System.err.println("Image file path: " + new File(this.globalFilePath).getAbsolutePath());
                    throw new RuntimeException("Failed to read image file: " + e.getMessage(), e);
                }
            }

            return result;
        } finally {
            releaseReadLock();
        }
    }

    public synchronized void WriteFile(Object content) {
        if (!acquireWriteLock()) {
            throw new RuntimeException("Failed to acquire write lock");
        }

        try {
            String fileExtension = this.GetFileExtension();

            if (fileExtension.equals("")) {
                throw new RuntimeException("File has no extension");
            }

            if (".txt".equals(fileExtension)) {
                String lines = (String)content;
                
                try (FileWriter fwriter = new FileWriter(new File(this.globalFilePath))) {
                    fwriter.write(lines);
                } catch(IOException e) {
                    throw new RuntimeException("Failed to write text file: " + e.getMessage(), e);
                }
            } else {
                // Is it an image
                byte[] data = (byte[])content;
                
                try (ByteArrayInputStream bao = new ByteArrayInputStream(data)) {
                    BufferedImage bImage = ImageIO.read(bao);
                    if (bImage == null) {
                        throw new RuntimeException("Failed to read image data");
                    }

                    String formatName = fileExtension.substring(1);
                    
                    if (!ImageIO.write(bImage, formatName, new File(this.globalFilePath))) {
                        throw new RuntimeException("Failed to write image in format: " + formatName);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to write image file: " + e.getMessage(), e);
                }
            }

            this.isDirty = true;  // Mark as dirty after writing
        } finally {
            releaseWriteLock();
        }
    }

    public synchronized void AppendFile(ArrayList<String> lines) {
        if (!acquireWriteLock()) {
            throw new RuntimeException("Failed to acquire write lock");
        }

        try {
            try (FileWriter fWriter = new FileWriter(this.globalFilePath, true)) {
                boolean isFirstWrite = new File(this.globalFilePath).length() == 0;
                
                for (int i = 0; i < lines.size(); ++i) {
                    if (!isFirstWrite || i > 0) {
                        fWriter.write("\n");
                    }
                    fWriter.write(lines.get(i));
                    isFirstWrite = false;
                }
                
                this.isDirty = true;
            } catch (IOException e) {
                throw new RuntimeException("Failed to append to file: " + e.getMessage(), e);
            }
        } finally {
            releaseWriteLock();
        }
    }

    // Only for .txt files
    public synchronized void RemoveFile(Set<String> lines) {
        if (!acquireWriteLock()) {
            throw new RuntimeException("Failed to acquire write lock");
        }

        try {
            String fileContent = (String) ReadFileInternal();  // Internal method to avoid double locking
            String[] fileLines = fileContent.split("\n");
            
            try (FileWriter fWriter = new FileWriter(this.globalFilePath)) {
                boolean firstLineWritten = false;

                for (String line : fileLines) {
                    if (!lines.contains(line)) {
                        if (firstLineWritten) {
                            fWriter.write("\n");
                        }
                        fWriter.write(line);
                        firstLineWritten = true;
                        this.isDirty = true;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to remove lines from file: " + e.getMessage(), e);
            }
        } finally {
            releaseWriteLock();
        }
    }

    // Internal method for reading without acquiring lock (when lock is already held)
    private Object ReadFileInternal() {
        String fileExtension = this.GetFileExtension();

        if (fileExtension.equals("")) {
            throw new RuntimeException("File has no extension");
        }

        if (".txt".equals(fileExtension)) {
            StringBuilder resultBuilder = new StringBuilder();
            
            try (BufferedReader br = new BufferedReader(new FileReader(new File(this.globalFilePath)))) {
                String line;
                boolean firstLine = true;

                while ((line = br.readLine()) != null) {
                    if (!firstLine) {
                        resultBuilder.append("\n");
                    }
                    resultBuilder.append(line);
                    firstLine = false;
                }

                return resultBuilder.toString();
            } catch(IOException e) {
                throw new RuntimeException("Failed to read text file: " + e.getMessage(), e);
            }
        } else {
            // Handle image files if needed
            throw new RuntimeException("ReadFileInternal not implemented for image files");
        }
    }
}
