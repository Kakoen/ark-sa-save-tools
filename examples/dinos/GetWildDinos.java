import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.*;
import net.kakoen.arksa.savetools.property.ObjectReference;
import net.kakoen.arksa.savetools.struct.ActorTransform;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;

@Slf4j
public class GetWildDinos {

    public static final Path TEST_SAVED_ARKS_FILE = Path.of("C:\\tmp\\Extinction_WP.ark");

    public static void main(String[] args) {
        try (ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(TEST_SAVED_ARKS_FILE.toFile())) {

            // Get wild dinos
            Map<UUID, ArkGameObject> wildDinos = getWildDinos(arkSaSaveDatabase);

            // Get status components of wild dinos, which hold stats of the dino
            Map<UUID, ArkGameObject> statusComponentsByDinoId = getStatusComponentsByDinoId(arkSaSaveDatabase, wildDinos);
            displayDinoInfo(wildDinos, statusComponentsByDinoId);
        } catch (Exception e) {
            throw new RuntimeException("Failed reading wild dinos", e);
        }
    }

    private static Map<UUID, ArkGameObject> getWildDinos(ArkSaSaveDatabase database) throws SQLException, IOException {
        Predicate<ArkGameObject> isDino = obj -> obj.hasProperty("DinoID1");
        Predicate<ArkGameObject> isOwned = obj -> obj.hasProperty("TribeName") || obj.hasProperty("TamedAtTime");
        Predicate<ArkGameObject> isWildDino = isDino.and(isOwned.negate());

        GameObjectReaderConfiguration config = GameObjectReaderConfiguration.builder()
                .gameObjectFilter(isWildDino)
                .build();

        return database.getGameObjects(config);
    }

    private static Map<UUID, ArkGameObject> getStatusComponentsByDinoId(ArkSaSaveDatabase database, Map<UUID, ArkGameObject> wildDinos) throws SQLException {
        Map<UUID, UUID> statusComponentIds = new HashMap<>();

        wildDinos.values().forEach(dino ->
                dino.getPropertyValue("MyCharacterStatusComponent", ObjectReference.class)
                        .ifPresent(statusRef -> statusComponentIds.put(dino.getUuid(), UUID.fromString((String) statusRef.getValue()))));

        Map<UUID, ArkGameObject> statusComponentsById = database.getGameObjectsByIds(statusComponentIds.values());
        Map<UUID, ArkGameObject> statusComponentsByDinoId = new HashMap<>();

        wildDinos.forEach((dinoId, dino) -> {
            UUID statusId = statusComponentIds.get(dinoId);
            statusComponentsByDinoId.put(dinoId, statusComponentsById.get(statusId));
        });

        return statusComponentsByDinoId;
    }

    private static void displayDinoInfo(Map<UUID, ArkGameObject> wildDinos, Map<UUID, ArkGameObject> statusComponents) {
        wildDinos.values().forEach(dino -> {
            ArkGameObject statusComponent = statusComponents.get(dino.getUuid());
            if (statusComponent == null) return; // This should not happen

            Integer level = statusComponent.getPropertyValue("BaseCharacterLevel", Integer.class).orElse(null);
            if (level == null) return; // Coel don't have levels, for example

            ActorTransform location = dino.getLocation();
            String type = dino.getBlueprint().split("\\.")[1];
            boolean isFemale = dino.getPropertyValue("bIsFemale", Boolean.class).orElse(false);

            // This is a map from stat (const) to levels applied in that stat
            Map<Integer, Short> baseStats = statusComponent.getPropertiesByPosition("NumberOfLevelUpPointsApplied", Short.class);

            log.info("{} {} Lv {} at ({}, {}, {}), Stats: {}", isFemale ? "Female" : "Male", type, level, location.getX(), location.getY(), location.getZ(), baseStats);
        });
    }
}
