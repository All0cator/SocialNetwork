package POD;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * FileData class represents a file with its path and checksum.
 * It implements Serializable for object serialization and Comparable for sorting.
 */
public class FileData implements Serializable, Comparable<FileData> {
    public String filePath;
    public BigInteger checksum;

    public FileData(String filePath, BigInteger checksum) {
        this.filePath = filePath;
        this.checksum = checksum;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;

        return this.filePath.equals(((FileData)other).filePath); 
    }

    @Override
    public int hashCode() {
        int code = 0;

        if (this.filePath != null) {
            code += this.filePath.hashCode();
        }

        if (this.checksum != null) {
            code += 31 * this.checksum.hashCode();
        }

        return code;
    }

    @Override
    public int compareTo(FileData other) {
        if (other == null) return 1;

        if (this.filePath == null && other.filePath == null) return 0;
        if (this.filePath == null) return -1;
        if (other.filePath == null) return 1;

        return this.filePath.compareTo(other.filePath);
    }

    @Override
    public String toString() {
        return String.format("{filePath: %s, checksum: %s}", this.filePath, this.checksum.toString());
    }
}
