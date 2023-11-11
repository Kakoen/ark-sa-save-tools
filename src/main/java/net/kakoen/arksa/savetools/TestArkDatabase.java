package net.kakoen.arksa.savetools;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class TestArkDatabase {

    public static void main(String[] args) {
        try (ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(new File("c:\\tmp\\TheIsland_WP.ark"))) {
            GameObjectReaderConfiguration readerConfiguration = GameObjectReaderConfiguration.builder()
                    .classNameFilter(name -> name.isPresent() && name.get().contains("Character_BP_C"))
                    .binaryFilesOutputDirectory(Path.of("c:\\tmp\\out\\bin"))
                    .jsonFilesOutputDirectory(Path.of("c:\\tmp\\out\\json"))
                    .build();

            Map<UUID, ArkGameObject> objects = arkSaSaveDatabase.getGameObjects(readerConfiguration);
            log.info("Found {} objects", objects.size());
        } catch(Exception e) {
            log.error("Something bad happened!", e);
            throw new RuntimeException("Failed to read save file", e);
        }
    }
}
