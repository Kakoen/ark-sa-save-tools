package net.kakoen.arksa.savetools;

import lombok.Data;
import net.kakoen.arksa.savetools.struct.ActorTransform;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Data
public class SaveContext {

    private Map<Integer, String> names;
    private Map<Integer, String> constantNameTable;
    private Map<Integer, String> someOtherTable;
    private List<String> parts;
    private Map<UUID, ActorTransform> actorTransforms;
    private int saveVersion;
    private double gameTime;
    private boolean generateUnknownNames;
    private long unknownValue;

    public Optional<ActorTransform> getActorTransform(UUID uuid) {
        return Optional.ofNullable(actorTransforms.get(uuid));
    }

    public boolean hasNameTable() {
        return names != null || constantNameTable != null;
    }

    public String getName(int key) {
        if(names != null && names.containsKey(key)) {
            return names.get(key);
        } else if(constantNameTable != null && constantNameTable.containsKey(key)) {
            return constantNameTable.get(key);
        } else {
            if (generateUnknownNames) {
                String newName = "Unknown_" + key;
                if(names != null) {
                    names.put(key, newName);
                }
                return newName;
            } else {
                return null;
            }
        }
    }

    public void useConstantNameTable(Map<Integer, String> constantNameTable) {
        this.constantNameTable = constantNameTable;
    }
}
