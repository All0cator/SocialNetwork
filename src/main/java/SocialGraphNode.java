import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SocialGraphNode {

    private Integer ID;
    // HashSet handles duplicate values
    private Set<SocialGraphNode> followerNodes;
    private Set<SocialGraphNode> followingNodes; 

    public SocialGraphNode(int ID) {
        this.ID = ID;
        this.followerNodes = ConcurrentHashMap.newKeySet();
        this.followingNodes = ConcurrentHashMap.newKeySet();
    }

    public int GetID() {
        return this.ID;
    }

    // You cannot add while also Getting IDs so this must be synchronized
    public synchronized void AddFollower(SocialGraphNode followerNode) {
        this.followerNodes.add(followerNode);
    }

    public synchronized void AddFollowing(SocialGraphNode followingNode) {
        this.followingNodes.add(followingNode);
    }

    public synchronized void RemoveFollower(SocialGraphNode followerNode) {
        this.followerNodes.remove(followerNode);
    }

    public synchronized  void RemoveFollowing(SocialGraphNode followingNode) {
        this.followingNodes.remove(followingNode);
    }

    // This function returns a non thread safe implementation of hashmap data structure
    public synchronized Set<Integer> GetFollowerIDs() {
        Set<Integer> result = new HashSet<Integer>();

        for(SocialGraphNode node : this.followerNodes) {
            result.add(node.GetID());
        }

        return result;
    }

    // This function returns a non thread safe implementation of hashmap data structure
    public synchronized Set<Integer> GetFollowingIDs() {
        Set<Integer> result = new HashSet<Integer>();

        for (SocialGraphNode node : this.followingNodes) {
            result.add(node.GetID());
        }

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;

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
        for (SocialGraphNode followerNode : this.followerNodes) {
            result += ", " + Integer.toString(followerNode.GetID());
        }

        result += ",\nFollowing: ";
        for (SocialGraphNode followingNode : this.followingNodes) {
            result += ", " + Integer.toString(followingNode.GetID());
        }
        result += "}";

        return result;
    }
}
