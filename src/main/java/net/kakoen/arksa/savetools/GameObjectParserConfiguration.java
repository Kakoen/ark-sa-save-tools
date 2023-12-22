package net.kakoen.arksa.savetools;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class GameObjectParserConfiguration {
    /**
     * Throws exception immediately when a game object can't be parsed. Otherwise, the game object is skipped.
     */
    private boolean throwExceptionOnParseError;
    private boolean writeBinFileOnParseError;
}
