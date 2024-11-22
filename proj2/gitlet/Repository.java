package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /** The object directory, use for store blob and commit */
    public static final File OBJECT_DIR = join(GITLET_DIR, "object");

    /** The commits directory, use for store the commit object (name is SHA-1 hash value) */
    public static final File COMMIT_DIR = join(OBJECT_DIR, "commits");

    /** The blobs directory, use for store the blob object (name is SHA-1 hash value)
     * each blob object contains the saved content
     */

    public static final File BLOB_DIR = join(OBJECT_DIR, "blobs");

    /** A file that contains the stage object to perform add and remove */
    public static final File STAGING_AREA_FILE = join(GITLET_DIR, "INDEX");

    /** The refs directory, use to store branches and other folder like remote */
    public static final File REF_DIR = join(GITLET_DIR, "refs");

    /** The branch directory, use to store different branches */
    public static final File BRANCH_DIR = join(REF_DIR, "heads");

    /** The default branch, master */
    public static File MASTER_BRANCH = join(BRANCH_DIR, "master");

    /** A file named HEAD that tracking of the current working branch, default is
     * refs/head/master
     */
    public static File HEAD_FILE = join(GITLET_DIR, "HEAD");


    /* TODO: fill in the rest of this class. */


    /** initialized a new .gitlet repo and make the CWD to be a gitlet track folder */
    public static void init() {
        if (GITLET_DIR.exists()) {
            existWithError("A Gitlet version-control system already exists in the current directory.");
        } else {
            GITLET_DIR.mkdir();
            OBJECT_DIR.mkdir();
            COMMIT_DIR.mkdir();
            BLOB_DIR.mkdir();
            REF_DIR.mkdir();
            BRANCH_DIR.mkdir();
            StageArea stage = new StageArea();
            stage.saveStage();
            try {
                MASTER_BRANCH.createNewFile();
                HEAD_FILE.createNewFile();
            } catch (IOException e) {
                throw new Error(e.getMessage());
            }
            Commit firstCommit = new Commit("initial commit", null);
            firstCommit.saveCommit();
            Utils.writeContents(MASTER_BRANCH, firstCommit.getID());
            // write the path to HEAD file, current path is to the default branch master
            Utils.writeContents(HEAD_FILE, ".gitlet/refs/heads/master");
        }
    }

    /** add a copy of the file as it currently exist in the staging area */
    public static void stageForAddition(String fileName) {
        // check to see if this file exist in CWD
        File filePath = Utils.join(CWD, fileName);
        if (!filePath.exists()) {
            Utils.existWithError("File does not exist.");
        }

        // check to see if this file is exist as same in last commit
        // if yes, do nothing and also remove it from addition area if exist
        // also, remove it from removal area if exist

        byte[] blob = Utils.readContents(filePath);
        String UID = Utils.sha1(blob);
        Commit lastCommit = Commit.fromFile();
        StageArea stageArea = Utils.readObject(STAGING_AREA_FILE, StageArea.class);
        stageArea.removeFromRemoval(fileName);
        if (lastCommit.getFileMap().containsKey(fileName)) {
            if (lastCommit.getFileMap().get(fileName).equals(UID)) {
                stageArea.removeFromAddition(fileName);
                return;
            }
        }

        // save a new blob
        File blobFile = Utils.join(BLOB_DIR, UID);
        try {
            blobFile.createNewFile();
            Utils.writeContents(blobFile, blob);
        } catch (IOException e) {
            throw new Error(e.getMessage());
        }
        stageArea.addToAddition(fileName, UID);
    }

    /** create a new commit, saves a snapshot of track files in current commit
     * and staging area so they can be restored at a later time.
     * By default, each commit snapshot will be exactly same as its parent
     * Addition: 1. File in addition area that has same file name but different content,
     * the new commit should track the latest version 2. file is not track by parent
     * should now be track by current commit
     * Removal: 1. File in removal area will be untracked in the new commit
     */
    public static void createCommit(String message) {
        if (message.isEmpty()) {
            Utils.existWithError("Please enter a commit message.");
        }
        StageArea stageArea = Utils.readObject(STAGING_AREA_FILE, StageArea.class);
        // handle the failure case
        if (stageArea.getAdditionMap().isEmpty() && stageArea.getRemovalSet().isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        // create a new commit and set last commit as its parent
        Commit lastCommit = Commit.fromFile();
        Commit newCommit = new Commit(message, lastCommit.getID());
        newCommit.setFileMap(new HashMap<>(lastCommit.getFileMap()));

        // add, remove or modify the fileMap
        Map<String, String> fileMap = newCommit.getFileMap();
        Map<String, String> additionArea = stageArea.getAdditionMap();
        Set<String> removalArea = stageArea.getRemovalSet();
        for (String key : additionArea.keySet()) {
            fileMap.put(key, additionArea.get(key));
        }
        for (String key : removalArea) {
            if (fileMap.containsKey(key)) {
                fileMap.remove(key);
            }
        }

        // update the UID and then save this commit
        newCommit.setID();
        newCommit.saveCommit();

        // clean the stage area
        stageArea.clear();

        // update the current branch to point the new commit
        File pathToWorkingBranch = Utils.join(CWD, Utils.readContentsAsString(HEAD_FILE));
        Utils.writeContents(pathToWorkingBranch, newCommit.getID());
    }

    /** remove the file from addition area and if the file is tracked in
     * current commit, we will stage it for removal and delete it from user's working dir
     */
    public static void stageForRemoval(String fileName) {
        StageArea stageArea = Utils.readObject(STAGING_AREA_FILE, StageArea.class);
        Commit lastCommit = Commit.fromFile();
        File filePath = Utils.join(CWD, fileName);
        boolean inAdditionArea = false;
        if (stageArea.getAdditionMap().containsKey(fileName)) {
            inAdditionArea = true;
        }

        // unstage the file if it is currently staged for addition
        stageArea.removeFromAddition(fileName);

        // if the file is tracked in current commit, remove it
        Map<String, String> fileMap = lastCommit.getFileMap();
        if (fileMap.containsKey(fileName)) {
            stageArea.addToRemoval(fileName);
            if (filePath.exists()) {
                filePath.delete();
            }
        } else {
            if (!inAdditionArea) {
                Utils.existWithError("No reason to remove the file.");
            }
        }

    }

    /** starting at the current head commit, display information about each commit backwards along
     * the commit tree until initial commit
     */
    public static void getLogs() {
        Commit lastCommit = Commit.fromFile();
        Utils.printCommit(lastCommit);
        while (lastCommit.getParentID() != null) {
            File pathToParentFile = Utils.join(Repository.COMMIT_DIR, lastCommit.getParentID());
            Commit parent = Utils.readObject(pathToParentFile, Commit.class);
            Utils.printCommit(parent);
            lastCommit = parent;
        }
    }

    /** 3 usages for checkout */
    public static void checkout(String[] args) {
        if (args.length <= 1 || args.length > 4) {
            Utils.existWithError("Incorrect operands.");
        }

        // 3 cases
        int argsLength = args.length;
        if (argsLength == 3) {
            if(!args[1].equals("--")) {
                Utils.existWithError("Incorrect operands.");
            }
            Commit lastCommit = Commit.fromFile();
            if (!lastCommit.getFileMap().containsKey(args[2])) {
                Utils.existWithError("File does not exist in that commit.");
            } else {
                File filePath = Utils.join(CWD, args[2]);
                File pathToBlob = Utils.join(BLOB_DIR, lastCommit.getFileMap().get(args[2]));
                try {
                    filePath.createNewFile();
                    Utils.writeContents(filePath, Utils.readContents(pathToBlob));
                } catch (IOException e) {
                    throw new Error(e.getMessage());
                }
            }
        } else if (argsLength == 4) {
            if(!args[2].equals("--")) {
                Utils.existWithError("Incorrect operands.");
            }
            String id = args[1];
            if (id.length() != 40) {
                List<String> allCommitID = Utils.plainFilenamesIn(COMMIT_DIR);
                for (String s : allCommitID) {
                    if (s.substring(0, id.length()).equals(id)) {
                        id = s;
                        break;
                    }
                }
            }
            File pathToSpecificCommit = Utils.join(COMMIT_DIR, id);
            if (!pathToSpecificCommit.exists()) {
                existWithError("No commit with that id exists.");
            }
            Commit specificCommit = Utils.readObject(pathToSpecificCommit, Commit.class);
            if (!specificCommit.getFileMap().containsKey(args[3])) {
                Utils.existWithError("File does not exist in that commit.");
            }
            File filePath = Utils.join(CWD, args[3]);
            File pathToBlob = Utils.join(BLOB_DIR, specificCommit.getFileMap().get(args[3]));
            try {
                filePath.createNewFile();
                Utils.writeContents(filePath, Utils.readContents(pathToBlob));
            } catch (IOException e) {
                throw new Error(e.getMessage());
            }
        } else {
            String branchName = args[1];
            File pathToCheckoutBranch = Utils.join(BRANCH_DIR, branchName);
            if (!pathToCheckoutBranch.exists()) {
                existWithError("No such branch exists.");
            }

            File pathToCurrBranch = Utils.join(CWD, Utils.readContentsAsString(HEAD_FILE));
            String s1 = Utils.readContentsAsString(HEAD_FILE);
            String s2 = ".gitlet/refs/heads/" + branchName;
            if (s1.equals(s2)) {
                existWithError("No need to checkout the current branch.");
            }

            // retrieve all the files in CWD,
            // an untracked file means those neither staged for addition nor tracked
            // two cases: pop error if would be overwritten or keep it if not
            Commit lastCommit = Commit.fromFile();
            String checkoutCommitID = Utils.readContentsAsString(pathToCheckoutBranch);
            Commit checkoutCommit = Utils.readObject(Utils.join(COMMIT_DIR, checkoutCommitID), Commit.class);
            List<String> allFilesInCWD = Utils.plainFilenamesIn(CWD);
            for (String fileName : allFilesInCWD) {
                if (!lastCommit.getFileMap().containsKey(fileName)) {
                    if (checkoutCommit.getFileMap().containsKey(fileName)) {
                        Utils.existWithError("There is an untracked file in the way; delete it, or add and commit it first.");
                    }
                }
            }

            // if the files is tracked in curr branch but not in checkout branch, remove it
            for (String fileName : lastCommit.getFileMap().keySet()) {
                File trackedFilePath = Utils.join(CWD, fileName);
                if (!checkoutCommit.getFileMap().containsKey(fileName)) {
                    if (trackedFilePath.exists()) {
                        trackedFilePath.delete();
                    }
                }
            }

            // checkout all the files in head commit in checkout branch
            for (String fileName : checkoutCommit.getFileMap().keySet()) {
                String[] input = new String[]{"checkout", checkoutCommitID, "--", fileName};
                checkout(input);
            }

            // change HEAD pointer
            Utils.writeContents(HEAD_FILE, ".gitlet/refs/heads/" + branchName);

        }
    }

    // global-log, like the log command but list all the commits
    public static void getAllCommits() {
        List<String> allCommit = Utils.plainFilenamesIn(COMMIT_DIR);
        for (String fileName : allCommit) {
            File pathToCommit = Utils.join(COMMIT_DIR, fileName);
            Commit commit = Utils.readObject(pathToCommit, Commit.class);
            Utils.printCommit(commit);
        }
    }

    // find all commits that has same message
    public static void findCommit(String message) {
        List<String> allCommit = Utils.plainFilenamesIn(COMMIT_DIR);
        boolean find = false;
        for (String fileName : allCommit) {
            File pathToCommit = Utils.join(COMMIT_DIR, fileName);
            Commit commit = Utils.readObject(pathToCommit, Commit.class);
            if (commit.getMessage().equals(message)) {
                System.out.println(commit.getID());
                find = true;
            }
        }
        if (!find) {
            System.out.println("Found no commit with that message.");
        }
    }

    // print the status of current git repo
    public static void printStatus() {
        System.out.println("=== Branches ===");
        List<String> branchesList = Utils.plainFilenamesIn(BRANCH_DIR);
        if (branchesList.size() == 1) {
            System.out.println("*" + branchesList.get(0));
        } else {
            String branch1 = branchesList.get(0);
            String branch2 = branchesList.get(1);
            File currBranchPath = Utils.join(CWD, Utils.readContentsAsString(HEAD_FILE));
            String currHeadID = Utils.readContentsAsString(currBranchPath);
            if (Utils.readContentsAsString(Utils.join(BRANCH_DIR, branch1)).equals(currHeadID)) {
                System.out.println("*" + branch1);
                System.out.println(branch2);
            } else {
                System.out.println("*" + branch2);
                System.out.println(branch1);
            }
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        StageArea stageArea = Utils.readObject(STAGING_AREA_FILE, StageArea.class);
        List<String> filesList = new ArrayList<>();
        for (String fileName : stageArea.getAdditionMap().keySet()) {
            filesList.add(fileName);
        }
        Collections.sort(filesList);
        for (String fileName : filesList) {
            System.out.println(fileName);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        List<String> removalList = new ArrayList<>();
        for (String fileName : stageArea.getRemovalSet()) {
            removalList.add(fileName);
        }
        Collections.sort(removalList);
        for (String fileName : removalList) {
            System.out.println(fileName);
        }
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();

        System.out.println("=== Untracked Files ===");
        System.out.println();
    }

    // create a new branch with the given name
    public static void addBranch(String branchName) {
        File newBranch = join(BRANCH_DIR, branchName);
        if (newBranch.exists()) {
            Utils.existWithError("A branch with that name already exists.");
        }
        try {
            newBranch.createNewFile();
        } catch (IOException e) {
            throw new Error(e.getMessage());
        }
        File workingBranch = Utils.join(CWD, Utils.readContentsAsString(HEAD_FILE));
        String currHeadCommitID = Utils.readContentsAsString(workingBranch);
        Utils.writeContents(newBranch, currHeadCommitID);
    }

    // remove a branch with the given name
    public static void removeBranch(String branchName) {
        File deletedBranch = join(BRANCH_DIR, branchName);
        if (!deletedBranch.exists()) {
            Utils.existWithError("AA branch with that name does not exist.");
        }
        File workingBranch = Utils.join(CWD, Utils.readContentsAsString(HEAD_FILE));
        String currHeadCommitID = Utils.readContentsAsString(workingBranch);
        if (Utils.readContentsAsString(deletedBranch).equals(currHeadCommitID)) {
            Utils.existWithError("Cannot remove the current branch.");
        }
        deletedBranch.delete();
    }

    // check out all files tracked by the given commit
    public static void reset(String commitID) {
        if (!Utils.join(COMMIT_DIR, commitID).exists()) {
            Utils.existWithError("No commit with that id exists.");
        }
        Commit checkOutCommit = Utils.readObject(Utils.join(COMMIT_DIR, commitID), Commit.class);
        Commit currentCommit = Commit.fromFile();
        List<String> filesInCWD = Utils.plainFilenamesIn(CWD);
        for (String fileName : filesInCWD) {
            // untracked file
            if (!currentCommit.getFileMap().containsKey(fileName)) {
                if (checkOutCommit.getFileMap().containsKey(fileName)) {
                    Utils.existWithError("There is an untracked file in the way; delete it, or add and commit it first.");
                }
            } else {
                // delete all tracked file in current
                File fileToBeDelete = Utils.join(CWD, fileName);
                fileToBeDelete.delete();
            }
        }
        // now we can check out all files that in given commit
        for (String fileName : checkOutCommit.getFileMap().keySet()) {
            checkout(new String[]{"checkout", commitID, "--", fileName});
        }
        // move the curr branch head to point the given commit
        File pathToCurrBranch = Utils.join(CWD, Utils.readContentsAsString(HEAD_FILE));
        Utils.writeContents(pathToCurrBranch, commitID);
        // clean the stage area
        StageArea stageArea = Utils.readObject(STAGING_AREA_FILE, StageArea.class);
        stageArea.clear();
    }
}
