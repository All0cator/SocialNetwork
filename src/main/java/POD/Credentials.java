package POD;

import java.io.Serializable;

/**
 * Credentials class represents user credentials with a username and password.
 * It implements Serializable for object serialization and Comparable for sorting.
 */
public class Credentials implements Serializable, Comparable<Credentials> { 
    public String userName;
    public String password;

    public Credentials() {
        userName = "";
        password = "";
    }

    public Credentials(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;

        return this.userName.equals(((Credentials)other).userName) && this.password.equals(((Credentials)other).password); 
    }

    @Override
    public int hashCode() {
        int code = 0;

        if (this.userName != null) {
            code += this.userName.hashCode();
        }

        if (this.password != null) {
            code += 31 * this.password.hashCode();
        }

        return code;
    }

    @Override
    public int compareTo(Credentials other) {
        if (other == null) return 1;

        int userNameComparison = this.userName.compareTo(other.userName);
        if (userNameComparison != 0) {
            return userNameComparison;
        }

        return this.password.compareTo(other.password);
    }

    @Override
    public String toString() {
        return String.format("{UserName: %s, Password: %s}", this.userName, this.password);
    }
}
