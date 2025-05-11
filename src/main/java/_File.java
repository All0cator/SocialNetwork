import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

import javax.imageio.ImageIO;

import Messages.PayloadImage;

public class _File {
    private String localFilePath;
    private String globalFilePath;
    private boolean isDirty;
    private BigInteger checksum;

    public _File(String localFilePath, String globalFilePath) {
        this.localFilePath = localFilePath;
        this.globalFilePath = globalFilePath;
        this.isDirty = true; // so that the first time we call get checksum it calculates it
    }

    public String GetLocalFilePath() {
        return this.localFilePath;
    }

    public String GetGlobalFilePath() {
        return this.globalFilePath;
    }

    public synchronized BigInteger GetChecksum() {
        if(isDirty) {
            // Generate checksum
            MessageDigest messageDigest;
            byte[] fileBytes;
            try {
                messageDigest = MessageDigest.getInstance("SHA-256");
                fileBytes = this.GetBytes();
                messageDigest.update(fileBytes);
                this.checksum = new BigInteger(messageDigest.digest());
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException();
            } catch (IOException e) {
                throw new RuntimeException();
            }

            isDirty = false;
        }

        return this.checksum;
    }

    private synchronized byte[] GetBytes() throws IOException {
        return Files.readAllBytes(Paths.get(this.globalFilePath));
    }

    public String GetFileExtension() {
        int extensionIndex = globalFilePath.lastIndexOf("."); 

        if(extensionIndex != -1) {
            return globalFilePath.substring(extensionIndex);
        } else {
            return "";
        }
    }

    // returns either a String with \n characters for txt files and for images a byte[] containing the Image 
    public synchronized Object ReadFile() {
        Object result = null;

        String fileExtension = this.GetFileExtension();

        if(fileExtension.equals("")) {
            throw new RuntimeException();
        }
        
        if(GetFileExtension().equals(".txt")) {
            // it is a text file
            result = "";

            BufferedReader br = null;

            try {
                br = new BufferedReader(new FileReader(new File(this.globalFilePath)));

                String line = null;

                while((line = br.readLine()) != null) {
                    result += line + "\n";
                }

            } catch(IOException e) {
                throw new RuntimeException();
            } finally {
                if(br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        throw new RuntimeException();
                    }
                }
            }

        } else {
            // it is an image
            try {
                BufferedImage bImage = ImageIO.read(new File(this.globalFilePath));

                ByteArrayOutputStream oStream = new ByteArrayOutputStream();

                ImageIO.write(bImage, fileExtension.substring(1), oStream);

                result = oStream.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException();
            }
            
        }

        return result;
    }

    public synchronized void WriteFile(Object content) {
        String fileExtension = this.GetFileExtension();

        if(fileExtension.equals("")) {
            throw new RuntimeException();
        }

        if(fileExtension.equals(".txt")) {
            String lines = (String)content;

            FileWriter fwriter = null;

            try {
                fwriter = new FileWriter(new File(this.globalFilePath));
                fwriter.write(lines);

            } catch(IOException e) {
                throw new RuntimeException();
            } finally {
                if(fwriter != null) {
                    try {
                        fwriter.close();
                    } catch (IOException e) {
                        throw new RuntimeException();
                    }
                }
            }
        } else {
            // is it an image
            byte[] data = (byte[])content;


            ByteArrayInputStream bao = new ByteArrayInputStream(data);
            try {
            BufferedImage bImage = ImageIO.read(bao);

            // . in regex means any character we have to escape it
            String[] tokens = this.GetFileExtension().split("\\.");

            ImageIO.write(bImage, tokens[1], new File(this.globalFilePath));
            } catch (IOException e) {
                throw new RuntimeException();
            }
        }
    }

    public synchronized void AppendFile(ArrayList<String> lines) {

        FileWriter fWriter = null;

        try {
            fWriter = new FileWriter(this.globalFilePath, true);
            
            for(String line : lines) {
                fWriter.write(line + "\n");
                this.isDirty = true;
            }
        } catch (IOException e) {
            throw new RuntimeException();
        } finally {
            if(fWriter != null) {
                try {
                    fWriter.close();
                } catch (IOException e) {
                    throw new RuntimeException();
                }
            }
        }
    }

    // Only for .txt files
    public synchronized void RemoveFile(Set<String> lines) {

        FileWriter fWriter = null;

        String[] fileLines = ((String)ReadFile()).split("\n");

        
        try {
            fWriter = new FileWriter(this.globalFilePath);
            
            if(fileLines != null) {
                for(int i = 0; i < fileLines.length; ++i) {
                    if(!lines.contains(fileLines[i])) {
                        fWriter.write(fileLines[i] + "\n");
                        this.isDirty = true;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException();
        } finally {
            if(fWriter != null) {
                try {
                    fWriter.close();
                } catch (IOException e) {
                    throw new RuntimeException();
                }
            }
        }
    }
}
