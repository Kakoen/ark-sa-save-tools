package net.kakoen.arksa.savetools;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Reads Ark: Survival Ascended *.arkprofile files
 */
public class ArkProfile extends ArkArchive {

    public ArkProfile(Path file) throws IOException {
        super(file);
    }

    public ArkProfile(ArkBinaryData data, SaveContext saveContext) {
        super(data, saveContext);
    }

    public ArkObject getProfile() {
        return getObjectByClass("/Game/PrimalEarth/CoreBlueprints/PrimalPlayerDataBP.PrimalPlayerDataBP_C");
    }

}
