import java.util.HashSet;

public class SocialGraphNode {

    private Integer ID;
    
    // HashSet handles duplicate values
    private HashSet<SocialGraphNode> IDTofollowerNode;
    private HashSet<SocialGraphNode> IDTofollowingNode; 

    public SocialGraphNode(int ID) {
        this.ID = ID;

        this.IDTofollowerNode = new HashSet<SocialGraphNode>();
        this.IDTofollowingNode = new HashSet<SocialGraphNode>();
    }

    public int GetID() {
        return this.ID;
    }

    public synchronized void AddFollower(SocialGraphNode followerNode) {
        this.IDTofollowerNode.add(followerNode);
    }

    public synchronized void AddFollowing(SocialGraphNode followingNode) {
        this.IDTofollowingNode.add(followingNode);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;

        return ((SocialGraphNode)obj).GetID() == this.GetID();
    }

    @Override
    public int hashCode() {
        return this.ID.hashCode();
    }

    @Override
    public String toString() {
        String result = "{ID: " + Integer.toString(this.ID);

        result += ",\nFollowers: ";

        for(SocialGraphNode followerNode : this.IDTofollowerNode) {
            result += ", " + Integer.toString(followerNode.GetID());
        }

        result += ",\nFollowing: ";

        for(SocialGraphNode followingNode : this.IDTofollowingNode) {
            result += ", " + Integer.toString(followingNode.GetID());
        }

        result += "}";

        return result;
    }
}
