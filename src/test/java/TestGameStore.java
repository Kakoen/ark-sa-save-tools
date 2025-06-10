import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.ArkProfile;
import net.kakoen.arksa.savetools.ArkSaSaveDatabase;
import net.kakoen.arksa.savetools.ArkTribe;
import net.kakoen.arksa.savetools.store.TribeAndPlayerData;
import net.kakoen.arksa.savetools.utils.HashUtils;
import net.kakoen.arksa.savetools.utils.JsonUtils;
import net.openhft.hashing.LongHashFunction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

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
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    public void canHashAllGameObjectsUsingSha256() throws Exception {
        try (ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(TestConstants.TEST_SAVED_ARKS_FILE.toFile())) {
            Set<UUID> allGameObjectUuids = arkSaSaveDatabase.getAllGameObjectUuids();
            Instant start = Instant.now();
            arkSaSaveDatabase.getHashOfObjects(allGameObjectUuids, HashUtils.defaultJvmHashAlgorithm("SHA-256"));
            Instant end = Instant.now();
            log.info("Hashed {} objects in {} ms using SHA-256", allGameObjectUuids.size(), end.toEpochMilli() - start.toEpochMilli());
        }
    }

    @Test
    public void canHashAllGameObjectsUsingXXHash() throws Exception {
        try (ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(TestConstants.TEST_SAVED_ARKS_FILE.toFile())) {
            Set<UUID> allGameObjectUuids = arkSaSaveDatabase.getAllGameObjectUuids();
            Instant start = Instant.now();
            LongHashFunction xxHashFunction = LongHashFunction.xx3();
            arkSaSaveDatabase.getHashOfObjects(allGameObjectUuids, xxHashFunction::hashBytes);
            Instant end = Instant.now();

            log.info("Hashed {} objects in {} ms using xxHash", allGameObjectUuids.size(), end.toEpochMilli() - start.toEpochMilli());
        }
    }

}
