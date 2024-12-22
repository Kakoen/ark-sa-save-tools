package net.kakoen.arksa.savetools;

import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class ArkArchive {

    private List<ArkObject> objects = new ArrayList<>();

    public ArkArchive(Path file) throws IOException {
        this(new ArkBinaryData(Files.readAllBytes(file)), new SaveContext());
    }

    public ArkArchive(ArkBinaryData data, SaveContext saveContext) {
        int startPosition = data.getPosition();

        saveContext.setSaveVersion(data.readInt());
        if(saveContext.getSaveVersion() < 5 || saveContext.getSaveVersion() > 6) {
            throw new RuntimeException("Unsupported archive version " + saveContext.getSaveVersion());
        }

        int count = data.readInt();
        for (int i = 0; i < count; i++) {
            objects.add(new ArkObject(data));
        }

        for (ArkObject object : objects) {
            data.setPosition(startPosition + object.getPropertiesOffset());
            object.readProperties(data);
        }

    }

    public ArkObject getObjectByClass(String className) {
        return objects.stream().filter(object -> object.getClassName().equals(className)).findFirst().orElse(null);
    }

    public ArkObject getObjectByUuid(UUID uuid) {
        return objects.stream().filter(object -> object.getUuid().equals(uuid)).findFirst().orElse(null);
    }

    public ArkObject getObjectByIndex(int index) {
        return objects.get(index);
    }

}
