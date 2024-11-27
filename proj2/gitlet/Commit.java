package gitlet;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// TODO: any imports you need here

import java.util.Date; // TODO: You'll likely use this in this class

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable{
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private final String message;

    /** Parent ID */
    private final String parentID;

    private String secondParentID;

    /** this commit' ID */
    private String id;

    /** Map data structure for store files corresponding to this version commit
     * {fileName : blob(UID SHA-1 VALUE)}
     */
    private Map<String, String> fileMap;

    /** Timestamp for this commit */
    private String timeStamp;


    /* TODO: fill in the rest of this class. */

    /** Constructor for the commit */
    public Commit(String message, String parentID) {
        this.message = message;
        this.parentID = parentID;
        this.fileMap = new HashMap<String, String>();
        setTimeStamp();
        // FIX_ME!! something might wrong here, about serialization and UID
        this.id = Utils.sha1(Utils.serialize(this));

    }

    public void setSecondParentID(String secondID) {
        this.secondParentID = secondID;
    }

    public String getSecondParentID() {
        return secondParentID;
    }

    public String getParentID() {
        return parentID;
    }

    public void setID() {
        this.id = Utils.sha1(Utils.serialize(this));
    }

    /** save the commit to a file for future use */
    public void saveCommit() {
        File newCommit = Utils.join(Repository.COMMIT_DIR, id);
        try {
            newCommit.createNewFile();
            Utils.writeObject(newCommit, this);
        } catch (IOException e) {
            throw new Error(e.getMessage());
        }
    }

    /** get ID for this commit */
    public String getID() {
        return this.id;
    }

    /** get the message for this commit */
    public String getMessage() {
        return this.message;
    }

    /** get the current commit timestamp and assign it to the timestamp instance variable */
    public void setTimeStamp() {
        // Get the current date and time in UTC
        ZonedDateTime dateTime;
        if (parentID == null) {
            dateTime = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        } else {
            dateTime = ZonedDateTime.now(ZoneId.of("UTC"));
        }

        // Define a formatter with the specified pattern
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E MMM dd hh:mm:ss yyyy Z");

        // Format the dateTime object
        String formattedDate = dateTime.format(formatter);

        this.timeStamp = formattedDate;
    }

    public String getTimeStamp() {
        return this.timeStamp;
    }

    public Map<String, String> getFileMap() {
        return fileMap;
    }

    public void setFileMap(Map<String, String> map) {
        this.fileMap = map;
    }

    public static Commit fromFile() {
        File pathToWorkingBranch = Utils.join(Repository.CWD, Utils.readContentsAsString(Repository.HEAD_FILE));
        File pathToLastCommit = Utils.join(Repository.COMMIT_DIR, Utils.readContentsAsString(pathToWorkingBranch));
        Commit lastCommit = Utils.readObject(pathToLastCommit, Commit.class);
        return lastCommit;
    }
}
