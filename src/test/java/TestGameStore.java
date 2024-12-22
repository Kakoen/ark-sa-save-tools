import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.ArkProfile;
import net.kakoen.arksa.savetools.ArkSaSaveDatabase;
import net.kakoen.arksa.savetools.ArkTribe;
import net.kakoen.arksa.savetools.store.TribeAndPlayerData;
import net.kakoen.arksa.savetools.utils.JsonUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

@Slf4j
@Disabled
public class TestGameStore {

    @Test
    public void canReadAllPlayersFromStore() throws Exception {
        try (ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(TestConstants.TEST_SAVED_ARKS_FILE.toFile())) {
            TribeAndPlayerData tribeAndPlayerData = arkSaSaveDatabase.getTribeAndPlayerData();
            tribeAndPlayerData.getPlayerIdentifiers().forEach((playerId) -> {
                try {
                    ArkProfile arkProfile = tribeAndPlayerData.getArkProfile(playerId);
                    log.info("player id {}", playerId);
                    JsonUtils.writeJsonToFile(arkProfile, TestConstants.TEST_OUTPUT_DIR.resolve("player-" + playerId.getEosId() + ".json"));
                } catch (Exception e) {
                    Path debugFileOutputPath = TestConstants.TEST_OUTPUT_DIR.resolve("player-" + playerId.getEosId() + ".bin");
                    try {
                        Files.write(debugFileOutputPath, tribeAndPlayerData.getArkProfileRawData(playerId));
                    } catch (IOException ex) {
                        throw new RuntimeException("Failed writing binary data", ex);
                    }
                    throw new RuntimeException("Failed reading player, bin file saved at " + debugFileOutputPath, e);
                }
            });

        }
    }

    @Test
    public void canReadAllTribesFromStore() throws Exception {
        try (ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(TestConstants.TEST_SAVED_ARKS_FILE.toFile())) {
            TribeAndPlayerData tribeAndPlayerData = arkSaSaveDatabase.getTribeAndPlayerData();
            tribeAndPlayerData.getTribeIdentifiers().forEach((tribeId) -> {
                try {
                    ArkTribe arkTribe = tribeAndPlayerData.getArkTribe(tribeId);
                    JsonUtils.writeJsonToFile(arkTribe, TestConstants.TEST_OUTPUT_DIR.resolve("tribe-" + tribeId.getTribeId() + ".json"));
                } catch (Exception e) {
                    Path debugFileOutputPath = TestConstants.TEST_OUTPUT_DIR.resolve("tribe-" + tribeId.getTribeId() + ".bin");
                    try {
                        Files.write(debugFileOutputPath, tribeAndPlayerData.getArkTribeRawData(tribeId));
                    } catch (IOException ex) {
                        throw new RuntimeException("Failed to write debug file", ex);
                    }
                    throw new RuntimeException("Failed reading tribe, bin file saved at " + debugFileOutputPath, e);
                }
            });
        }
    }

    @Test
    public void canReadAllGameObjects() throws Exception {
        try (ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(TestConstants.TEST_SAVED_ARKS_FILE.toFile())) {
            arkSaSaveDatabase.getAllGameObjectUuids().parallelStream().forEach(uuid -> {
                try {
                    arkSaSaveDatabase.getGameObjectById(uuid);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
