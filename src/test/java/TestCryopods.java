import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.*;
import net.kakoen.arksa.savetools.cryopod.Cryopod;
import net.kakoen.arksa.savetools.utils.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public class TestCryopods {

    private final static Path OUT_BIN = Path.of("c:\\tmp\\out\\bin");
    private final static Path OUT_JSON = Path.of("c:\\tmp\\out\\json");

    public static void main(String[] args) {
        try (ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(new File("c:\\tmp\\save\\TheIsland_WP.ark"))) {
            GameObjectReaderConfiguration readerConfiguration = GameObjectReaderConfiguration.builder()
                    .blueprintNameFilter(name -> name.isPresent() && name.get().contains("PrimalItem_WeaponEmptyCryopod_C"))
                    .binaryFilesOutputDirectory(OUT_BIN)
                    .jsonFilesOutputDirectory(OUT_JSON)
                    //.uuidFilter(uuid -> uuid.equals(UUID.fromString("6f138c51-5abd-0f4e-bfee-6f03fc5475da")))
                    .build();

            Map<UUID, ArkGameObject> objects = arkSaSaveDatabase.getGameObjects(readerConfiguration);
            for (ArkGameObject gameObject : objects.values()) {
                List<ArkPropertyContainer> byteArrays = getByteArrays(gameObject).orElse(null);
                if (byteArrays == null) continue;

                Cryopod cryopod = new Cryopod(gameObject.getUuid());

                //Read dino and status component
                byteArrays.get(0).getArrayPropertyValue("Bytes", Byte.class)
                        .map(TestCryopods::toByteArray)
                        .ifPresent(bytes -> cryopod.parseDinoAndStatusComponentData(bytes, readerConfiguration));

                //Read saddle
                cryopod.setSaddle(parseProperties(
                        byteArrays.get(1).getArrayPropertyValue("Bytes", Byte.class).orElse(null)
                ));

                //Read costume
                cryopod.setCostume(parseProperties(
                        byteArrays.get(2).getArrayPropertyValue("Bytes", Byte.class).orElse(null)
                ));

                if (readerConfiguration.getJsonFilesOutputDirectory() != null) {
                    JsonUtils.writeJsonToFile(cryopod, readerConfiguration.getJsonFilesOutputDirectory().resolve("cryopods").resolve(gameObject.getUuid() + ".json"));
                }
            }
            log.info("Found {} objects", objects.size());
        } catch (Exception e) {
            log.error("Something bad happened!", e);
            throw new RuntimeException("Failed to read save file", e);
        }

    }

    private static ArkPropertyContainer parseProperties(List<Byte> byteData) {
        if (byteData == null || byteData.isEmpty()) {
            return null;
        }

        byte[] bytes = toByteArray(byteData);

        ArkBinaryData data = new ArkBinaryData(bytes);
        data.expect(6, data.readInt());
        ArkPropertyContainer container = new ArkPropertyContainer();
        container.readProperties(data);

        return container;
    }

    private static Optional<List<ArkPropertyContainer>> getByteArrays(ArkGameObject object) {
        return object.getArrayPropertyValue("CustomItemDatas", ArkPropertyContainer.class)
                .map(data -> data.get(0))
                .flatMap(itemData -> itemData.getPropertyValue("CustomDataBytes", ArkPropertyContainer.class))
                .flatMap(customDataBytes -> customDataBytes.getArrayPropertyValue("ByteArrays", ArkPropertyContainer.class));
    }

    private static byte[] toByteArray(List<Byte> bytes) {
        byte[] result = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) {
            result[i] = bytes.get(i);
        }
        return result;
    }

}
