import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * DirectoryNode represents a directory in the file system.
 * It can recursively list its contents and provide information about its structure.
 */
public class DirectoryNode {

    private String pathName;
    private int depth;
    private boolean isDirectory;
    private ArrayList<DirectoryNode> children;

    public DirectoryNode(String pathName, int depth, boolean isDirectory) {
        this.pathName = pathName;
        this.depth = depth;
        this.isDirectory = isDirectory;
        this.children = new ArrayList<DirectoryNode>();

        if (isDirectory) {
            File[] filePaths = new File(pathName).listFiles();

            if (filePaths == null) return;

            for (int i = 0; i < filePaths.length; ++i) {
                boolean isChildDirectory = filePaths[i].isDirectory();
                children.add(new DirectoryNode(pathName + "/" + filePaths[i].getName(), depth + 1, isChildDirectory));
            }
        }
    }

    public void GetFilePaths(HashSet<String> filePaths, String rootPath) {
        if (!this.isDirectory) {
            String escapedSeparator = "/"; //File.separator.equals("\\") ? "\\\\" : File.separator;
            filePaths.add(this.pathName.replaceAll(rootPath + escapedSeparator, ""));
            return;
        }

        for (int i = 0; i < this.children.size(); ++i) {
            this.children.get(i).GetFilePaths(filePaths, rootPath);
        }
    }

    public int GetDepth() {
        return this.depth;
    }

    public void Print() {
        String directory = isDirectory ? "/" : "";
        int idx = this.pathName.lastIndexOf("/");
        String name;

        if (idx < 0 || idx >= this.pathName.length() - 1) {
            name = this.pathName;
        } else {
            name = this.pathName.substring(idx + 1);
        }

        String blank = "    ".repeat(depth - 1);
        System.out.println(blank + "|---" + name + directory);

        for (int i = 0; i < this.children.size(); ++i) {
            this.children.get(i).Print();
        }
    }

    public String GetPathName() {
        return this.pathName;
    }
}
