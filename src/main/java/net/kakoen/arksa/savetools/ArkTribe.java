package net.kakoen.arksa.savetools;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Reads Ark: Survival Ascended *.arktribe files
 */
public class ArkTribe extends ArkArchive {

    public ArkTribe(Path file) throws IOException {
        super(file);
    }

    public ArkObject getTribe() {
        return getObjectByClass("/Script/ShooterGame.PrimalTribeData");
    }

}
