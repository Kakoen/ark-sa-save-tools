import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.*;
import net.kakoen.arksa.savetools.utils.JsonUtils;
import net.kakoen.arksa.savetools.utils.WildcardInflaterInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.InflaterInputStream;

@Slf4j
public class TestCryopods {

    private final static Path OUT_BIN = Path.of("c:\\tmp\\out\\bin");
    private final static Path OUT_BIN_CRYOPODS = OUT_BIN.resolve("cryopods");
    private final static Path OUT_JSON = Path.of("c:\\tmp\\out\\json");
    private final static Path OUT_JSON_CRYOPODS = OUT_JSON.resolve("cryopods");

    public static void main(String[] args) {
        try (ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(new File("c:\\tmp\\save\\TheIsland_WP.ark"))) {
            GameObjectReaderConfiguration readerConfiguration = GameObjectReaderConfiguration.builder()
                    .blueprintNameFilter(name -> name.isPresent() && name.get().contains("PrimalItem_WeaponEmptyCryopod_C"))
                    .binaryFilesOutputDirectory(OUT_BIN)
                    .jsonFilesOutputDirectory(OUT_JSON)
                    .build();

            Files.createDirectories(OUT_BIN_CRYOPODS);
            Files.createDirectories(OUT_JSON_CRYOPODS);

            Map<UUID, ArkGameObject> objects = arkSaSaveDatabase.getGameObjects(readerConfiguration);
            for(ArkGameObject cryopod : objects.values()) {
                List<ArkPropertyContainer> byteArrays = getByteArrays(cryopod).orElse(null);
                if(byteArrays == null) continue;

                //Read dino and status component
                byte[] bytes = byteArrays.get(0).getArrayPropertyValue("Bytes", Byte.class)
                        .map(TestCryopods::toByteArray)
                        .orElse(null);

                try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes)) {
                    byte[] header = byteArrayInputStream.readNBytes(12); //Contains constant, size of inflated data, and name table offset?

                    try (InflaterInputStream is = new InflaterInputStream(byteArrayInputStream);
                         WildcardInflaterInputStream wildcardInflater = new WildcardInflaterInputStream(is)) {

                        byte[] inflated = wildcardInflater.readAllBytes();
                        Files.write(OUT_BIN_CRYOPODS.resolve( cryopod.getUuid() + ".bytes0.bin"), inflated);
                        //log.info("Found {} for {}, start: {}", bytes, object, 12);
                    } catch(Exception e) {
                        log.error("Failed to read data for {}", cryopod, e);
                    }
                }

                //Read saddle
                parsePropertiesAndWrite(
                        byteArrays.get(1).getArrayPropertyValue("Bytes", Byte.class).orElse(null),
                        OUT_BIN_CRYOPODS.resolve(cryopod.getUuid() + ".bytes1.bin"),
                        OUT_JSON_CRYOPODS.resolve(cryopod.getUuid() + ".bytes1.json")
                );

                //Read costume
                parsePropertiesAndWrite(
                        byteArrays.get(2).getArrayPropertyValue("Bytes", Byte.class).orElse(null),
                        OUT_BIN_CRYOPODS.resolve(cryopod.getUuid() + ".bytes2.bin"),
                        OUT_JSON_CRYOPODS.resolve(cryopod.getUuid() + ".bytes2.json")
                );
            }
            log.info("Found {} objects", objects.size());
        } catch (Exception e) {
            log.error("Something bad happened!", e);
            throw new RuntimeException("Failed to read save file", e);
        }
    }

    private static void parsePropertiesAndWrite(List<Byte> byteData, Path binOutputPath, Path jsonOutputPath) throws IOException {
        if(byteData == null || byteData.isEmpty()) {
            return;
        }

        byte[] bytes = toByteArray(byteData);
        //Write bytes to path
        if(binOutputPath != null) {
            Files.write(binOutputPath, bytes);
        }

        ArkBinaryData data = new ArkBinaryData(bytes);
        data.expect(6, data.readInt());
        ArkPropertyContainer container = new ArkPropertyContainer();
        container.readProperties(data);

        if(jsonOutputPath != null) {
            JsonUtils.writeJsonToFile(container, jsonOutputPath);
        }
    }

    private static Optional<List<ArkPropertyContainer>> getByteArrays(ArkGameObject object) {
        return object.getArrayPropertyValue("CustomItemDatas", ArkPropertyContainer.class)
                .map(data -> data.get(0))
                .flatMap(itemData -> itemData.getPropertyValue("CustomDataBytes", ArkPropertyContainer.class))
                .flatMap(customDataBytes -> customDataBytes.getArrayPropertyValue("ByteArrays", ArkPropertyContainer.class));
    }

    private static ByteBuffer toByteBuffer(List<Byte> bytes, int start) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.size() - start);
        for(int i = start; i < bytes.size(); i++) {
            byteBuffer.put(bytes.get(i));
        }
        byteBuffer.flip();
        return byteBuffer;
    }

    private static byte[] toByteArray(List<Byte> bytes) {
        byte[] result = new byte[bytes.size()];
        for(int i = 0; i < bytes.size(); i++) {
            result[i] = bytes.get(i);
        }
        return result;
    }

}
