import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.ArkSaSaveDatabase;
import net.kakoen.arksa.savetools.store.TribeAndPlayerData;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

@Slf4j
@Disabled
public class TestGameStore {

    @Test
    public void canReadAllPlayersAndTribesFromStore() throws Exception {
        try (ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(TestConstants.TEST_SAVED_ARKS_FILE.toFile())) {
            TribeAndPlayerData tribeAndPlayerData = arkSaSaveDatabase.getTribeAndPlayerData();
            tribeAndPlayerData.getTribeIdentifiers().forEach(tribeAndPlayerData::getArkTribe);
            tribeAndPlayerData.getPlayerIdentifiers().forEach(tribeAndPlayerData::getArkProfile);
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
