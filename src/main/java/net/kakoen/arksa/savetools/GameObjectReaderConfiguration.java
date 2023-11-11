package net.kakoen.arksa.savetools;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@Builder
@Data
public class GameObjectReaderConfiguration {

    private Predicate<UUID> uuidFilter;
    private Predicate<Optional<String>> classNameFilter;
    private Predicate<ArkGameObject> gameObjectFilter;

    /**
     * Output .bin files for every game object that matches the filter to this directory.
     * Can be null, in which no data is written to disk.
     */
    private Path binaryFilesOutputDirectory;

    /**
     * Output .json files for every game object that matches the filter to this directory.
     * Can be null, in which no data is written to disk.
     */
    private Path jsonFilesOutputDirectory;

}
