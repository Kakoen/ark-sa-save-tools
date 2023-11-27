package net.kakoen.arksa.savetools;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
public class GameObjectReaderConfiguration extends GameObjectParserConfiguration {

    private Predicate<UUID> uuidFilter;
    private Predicate<Optional<String>> blueprintNameFilter;
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
