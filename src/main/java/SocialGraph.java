import java.util.HashMap;

public class SocialGraph {
    
    // HashMap provides fast query of a node with a specific userID
    private HashMap<Integer, SocialGraphNode> userIDToNode;
    
    public SocialGraph() {
        this.userIDToNode = new HashMap<Integer, SocialGraphNode>();
    }

    public synchronized SocialGraphNode GetUserNode(int userID) {
        return userIDToNode.get(userID);
    }

    public synchronized void AddUser(int userID, int followersIDs[]) {
        SocialGraphNode userNode = userIDToNode.get(userID);
        if(userNode == null) {
            userNode = new SocialGraphNode(userID);
            userIDToNode.put(userID, userNode);
        }

        if(followersIDs != null) {
            for(int i = 0; i < followersIDs.length; ++i) {
                SocialGraphNode followerNode = userIDToNode.get(followersIDs[i]);
                if(followerNode == null) {
                    followerNode = new SocialGraphNode(followersIDs[i]);
                    userIDToNode.put(followersIDs[i], followerNode);
                }
                
                followerNode.AddFollowing(userNode);
                userNode.AddFollower(followerNode);
            }
        }
    }
}
