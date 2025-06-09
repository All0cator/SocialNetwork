package POD;

import java.io.Serializable;

public class Credentials implements Serializable, Comparable { 
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
    public int compareTo(Object other) {
        Credentials o = (Credentials)other;

        if (this.userName.compareTo(o.userName)  == -1) {
            return -1;
        } else if(this.userName.compareTo(o.userName) == 1) {
            return 1;
        } else {
            if (this.password.compareTo(o.password) == -1) {
                return -1;
            } else if (this.password.compareTo(o.password) == 1) {
                return 1;
            }

            return 0;
        }
    }

    @Override
    public String toString() {
        return String.format("{UserName: %s, Password: %s}", this.userName, this.password);
    }
}
