package gitlet;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.io.File;

public class StageArea implements Serializable {
    /** map = {fileName : SHA-1 hash} */
    private Map<String, String> additionMap;
    private Set<String> removalSet;

    public StageArea() {
        additionMap = new HashMap<>();
        removalSet = new HashSet<>();
    }
    public void saveStage() {
        File stageFile = Repository.STAGING_AREA_FILE;
        try {
            stageFile.createNewFile();
        } catch (IOException e) {
            throw new Error(e.getMessage());
        }
        Utils.writeObject(stageFile, this);
    }

    public Map<String, String> getAdditionMap() {
        return this.additionMap;
    }

    public Set<String> getRemovalSet() {
        return this.removalSet;
    }

    public void addToAddition(String fileName, String UID) {
        additionMap.put(fileName, UID);
        saveStage();
    }

    public void addToRemoval(String fileName) {
        removalSet.add(fileName);
        saveStage();
    }
    public void removeFromAddition(String fileName) {
        if (additionMap.containsKey(fileName)) {
            additionMap.remove(fileName);
        }
        saveStage();
    }

    public void removeFromRemoval(String fileName) {
        if (removalSet.contains(fileName)) {
            removalSet.remove(fileName);
        }
        saveStage();
    }
    public void clear() {
        additionMap = new HashMap<>();
        removalSet = new HashSet<>();
        saveStage();
    }

}
