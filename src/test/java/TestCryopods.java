import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.ArchiveType;
import net.kakoen.arksa.savetools.ArkBinaryData;
import net.kakoen.arksa.savetools.ArkGameObject;
import net.kakoen.arksa.savetools.ArkPropertyContainer;
import net.kakoen.arksa.savetools.ArkSaSaveDatabase;
import net.kakoen.arksa.savetools.GameObjectReaderConfiguration;
import net.kakoen.arksa.savetools.ParseContext;
import net.kakoen.arksa.savetools.SaveContext;
import net.kakoen.arksa.savetools.cryopod.Cryopod;
import net.kakoen.arksa.savetools.utils.JsonUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class TestCryopods {

    private final static Path OUT_BIN = Path.of("c:\\tmp\\out\\bin");
    private final static Path OUT_JSON = Path.of("c:\\tmp\\out\\json");

    public static void main(String[] args) {
        try (ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(TestConstants.TEST_SAVED_ARKS_FILE.toFile())) {
            GameObjectReaderConfiguration readerConfiguration = GameObjectReaderConfiguration.builder()
                    .blueprintNameFilter(name -> name.isPresent() && name.get().contains("PrimalItem_WeaponEmptyCryopod_C"))
                    .binaryFilesOutputDirectory(OUT_BIN)
                    .jsonFilesOutputDirectory(OUT_JSON)
                    .throwExceptionOnParseError(true)
                    //.uuidFilter(uuid -> uuid.equals(UUID.fromString("6f138c51-5abd-0f4e-bfee-6f03fc5475da")))
                    .build();

            Map<UUID, ArkGameObject> objects = arkSaSaveDatabase.getGameObjects(readerConfiguration);
            for (ArkGameObject gameObject : objects.values()) {
                List<ArkPropertyContainer> byteArrays = getByteArrays(gameObject).orElse(null);
                if (byteArrays == null) continue;

                Cryopod cryopod = new Cryopod(gameObject.getUuid());

                //Read dino and status component
                byteArrays.get(0).getArrayPropertyValue("Bytes", Byte.class)
                        .ifPresent(bytes -> cryopod.parseDinoAndStatusComponentData(bytes, readerConfiguration));

                //Read saddle
                if(byteArrays.size() > 1) {
                    cryopod.setSaddle(parseProperties(
                            byteArrays.get(1).getArrayPropertyValue("Bytes", Byte.class).orElse(null),
                            arkSaSaveDatabase.getSaveContext()
                    ));
                }

                //Read costume
                if(byteArrays.size() > 2) {
                    cryopod.setCostume(parseProperties(
                            byteArrays.get(2).getArrayPropertyValue("Bytes", Byte.class).orElse(null),
                            arkSaSaveDatabase.getSaveContext()
                    ));
                }

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

    private static ArkPropertyContainer parseProperties(List<Byte> byteData, SaveContext saveContext) {
        if (byteData == null || byteData.isEmpty()) {
            return null;
        }

        byte[] bytes = toByteArray(byteData);

        ArkBinaryData data = new ArkBinaryData(bytes);

        int archiveVersion = data.readInt();

        if (archiveVersion == 0x1bedead) {
            // New archive version marked by 0x1bedead
            archiveVersion = data.readInt();
        }
        data.pushParseContext(new ParseContext(ArchiveType.ARK_ARCHIVE, archiveVersion));

        if (archiveVersion >= 7) {
            data.skipBytes(8);
        }

        ArkPropertyContainer container = new ArkPropertyContainer();
        container.readProperties(data);

        return container;
    }

    private static Optional<List<ArkPropertyContainer>> getByteArrays(ArkGameObject object) {
        return object.getArrayPropertyValue("CustomItemDatas", ArkPropertyContainer.class)
                .map(data -> !data.isEmpty() ? data.getFirst() : null)
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
