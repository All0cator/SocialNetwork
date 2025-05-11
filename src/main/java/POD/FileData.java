package POD;

import java.io.File;
import java.io.Serializable;
import java.math.BigInteger;

public class FileData implements Serializable, Comparable {
    public String filePath;
    public BigInteger checksum;

    public FileData(String filePath, BigInteger checksum) {
        this.filePath = filePath;
        this.checksum = checksum;
    }

    
    @Override
    public boolean equals(Object other) {
        if(other == null) return false;
        
        return this.filePath.equals(((FileData)other).filePath); 
    }
    
    @Override
    public int hashCode() {
        
        int code = 0;
        
        if(this.filePath != null) {
            code += this.filePath.hashCode();
        }
        
        if(this.checksum != null) {
            code += 31 * this.checksum.hashCode();
        }
        
        return code;
    }
    
    @Override
    public int compareTo(Object other) {
        
        FileData o = (FileData)other;
        
        if(this.filePath.compareTo(o.filePath) == -1) {
            return -1;
        } else if(this.filePath.compareTo(o.filePath) == 1) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return String.format("{filePath: %s, checksum: %s}", this.filePath, this.checksum.toString());
    }
}
