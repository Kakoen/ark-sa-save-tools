package net.kakoen.arksa.savetools.cryopod;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.ArkBinaryData;
import net.kakoen.arksa.savetools.ArkGameObject;
import net.kakoen.arksa.savetools.ArkPropertyContainer;
import net.kakoen.arksa.savetools.ArkSaveUtils;
import net.kakoen.arksa.savetools.GameObjectReaderConfiguration;
import net.kakoen.arksa.savetools.SaveContext;
import net.kakoen.arksa.savetools.utils.ByteListInputStream;
import net.kakoen.arksa.savetools.utils.WildcardInflaterInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.InflaterInputStream;


@Slf4j
@Data
public class Cryopod {

    private final static Map<Integer, String> NAME_CONSTANTS = new HashMap<>();

    static {
        // Not complete, but enough to read (most) cryopods...
        NAME_CONSTANTS.put(0, "TribeName");
        NAME_CONSTANTS.put(1, "StrProperty");
        NAME_CONSTANTS.put(2, "bServerInitializedDino");
        NAME_CONSTANTS.put(3, "BoolProperty");
        NAME_CONSTANTS.put(5, "FloatProperty");
        NAME_CONSTANTS.put(6, "ColorSetIndices");
        NAME_CONSTANTS.put(7, "ByteProperty");
        NAME_CONSTANTS.put(8, "None");
        NAME_CONSTANTS.put(9, "ColorSetNames");
        NAME_CONSTANTS.put(10, "NameProperty");
        NAME_CONSTANTS.put(11, "TamingTeamID");
        NAME_CONSTANTS.put(12, "UInt64Property"); //???
        NAME_CONSTANTS.put(13, "RequiredTameAffinity");
        NAME_CONSTANTS.put(14, "TamingTeamID");
        NAME_CONSTANTS.put(15, "IntProperty");
        NAME_CONSTANTS.put(19, "StructProperty");
        NAME_CONSTANTS.put(23, "DinoID1");
        NAME_CONSTANTS.put(24, "UInt32Property");
        NAME_CONSTANTS.put(25, "DinoID2");
        NAME_CONSTANTS.put(31, "UploadedFromServerName");
        NAME_CONSTANTS.put(32, "TamedOnServerName");
        NAME_CONSTANTS.put(36, "TargetingTeam");
        NAME_CONSTANTS.put(38, "bReplicateGlobalStatusValues");
        NAME_CONSTANTS.put(39, "bAllowLevelUps");
        NAME_CONSTANTS.put(40, "bServerFirstInitialized");
        NAME_CONSTANTS.put(41, "ExperiencePoints");
        NAME_CONSTANTS.put(42, "CurrentStatusValues");
        NAME_CONSTANTS.put(44, "ArrayProperty");
        NAME_CONSTANTS.put(55, "bIsFemale");
    }

    private List<ArkGameObject> dinoAndStatusComponent;
    private ArkPropertyContainer saddle;
    private ArkPropertyContainer costume;
    private UUID uuid;

    public Cryopod(UUID uuid) {
        this.uuid = uuid;
    }

    public void parseDinoAndStatusComponentData(List<Byte> bytes, GameObjectReaderConfiguration readerConfiguration) {
        try (ByteListInputStream rawInputStream = new ByteListInputStream(bytes);
             InflaterInputStream inflaterInputStream = new InflaterInputStream(rawInputStream);
             WildcardInflaterInputStream inputStream = new WildcardInflaterInputStream(inflaterInputStream)) {

            readHeaderDinoAndStatusComponent(rawInputStream, inputStream, readerConfiguration);

        } catch (Exception e) {
            log.error("Failed to read data for {}", this, e);
            if (readerConfiguration.isThrowExceptionOnParseError()) {
                throw new RuntimeException("Failed to read cryopod data for UUID " + uuid, e);
            }
        }
    }

    public void parseDinoAndStatusComponentData(byte[] bytes, GameObjectReaderConfiguration readerConfiguration) {
        try (ByteArrayInputStream rawInputStream = new ByteArrayInputStream(bytes);
             InflaterInputStream inflaterInputStream = new InflaterInputStream(rawInputStream);
             WildcardInflaterInputStream inputStream = new WildcardInflaterInputStream(inflaterInputStream)) {

            readHeaderDinoAndStatusComponent(rawInputStream, inputStream, readerConfiguration);
        } catch (Exception e) {
            log.error("Failed to read data for {}", this, e);
            if (readerConfiguration.isThrowExceptionOnParseError()) {
                throw new RuntimeException("Failed to read cryopod data for UUID " + uuid, e);
            }
        }
    }

    private void readHeaderDinoAndStatusComponent(InputStream rawInputStream, InputStream inflatedInputStream, GameObjectReaderConfiguration readerConfiguration) throws IOException {
        ArkBinaryData headerData = new ArkBinaryData(rawInputStream.readNBytes(12));
        headerData.expect(0x0406, headerData.readInt());
        int inflatedSize = headerData.readInt(); //size of inflated data, before WildcardInflaterInputStream has been applied
        int namesOffset = headerData.readInt();

        byte[] inflatedData = inflatedInputStream.readAllBytes();

        if (readerConfiguration.getBinaryFilesOutputDirectory() != null) {
            Files.createDirectories(readerConfiguration.getBinaryFilesOutputDirectory().resolve("cryopods"));
            Files.write(readerConfiguration.getBinaryFilesOutputDirectory().resolve("cryopods").resolve(uuid + ".bytes0.bin"), inflatedData);
        }

        readDinoAndStatusComponent(new ArkBinaryData(inflatedData), namesOffset);
    }

    private void readDinoAndStatusComponent(ArkBinaryData reader, int namesTableOffset) {
        SaveContext saveContext = reader.getSaveContext();
        saveContext.setNames(readNameTable(reader, namesTableOffset));
        saveContext.useConstantNameTable(NAME_CONSTANTS);
        saveContext.setGenerateUnknownNames(true);

        reader.setPosition(0);

        dinoAndStatusComponent = new ArrayList<>();
        int objectCount = reader.readInt();
        for (int i = 0; i < objectCount; i++) {
            ArkGameObject gameObject = ArkGameObject.readFromCustomBytes(reader);
            dinoAndStatusComponent.add(gameObject);
        }

        for (ArkGameObject gameObject : dinoAndStatusComponent) {
            try {
                if (reader.getPosition() != gameObject.getPropertiesOffset()) {
                    log.warn("Reader position {} does not match properties offset {}, bytes left to read: {}", reader.getPosition(), gameObject.getPropertiesOffset(), gameObject.getPropertiesOffset() - reader.getPosition());
                    reader.setPosition(gameObject.getPropertiesOffset());
                }
                gameObject.readProperties(reader);
                gameObject.readExtraData(reader);
                reader.readInt();
            } catch (Exception e) {
                log.error("Error reading properties for cryopod {}, debug info follows:", this, e);
                ArkSaveUtils.enableDebugLogging = true;
                reader.setPosition(gameObject.getPropertiesOffset());
                gameObject.setProperties(new ArrayList<>());
                gameObject.readProperties(reader);
                gameObject.readExtraData(reader);

                //TODO respect parser configuration to decide whether to throw exception or not. Currently it always throws.
            } finally {
                ArkSaveUtils.enableDebugLogging = false;
            }
        }
    }

    private static Map<Integer, String> readNameTable(ArkBinaryData reader, int namesTableOffset) {
        Map<Integer, String> nameTable = new HashMap<>();
        reader.setPosition(namesTableOffset);
        int nameCount = reader.readInt();
        for (int i = 0; i < nameCount; i++) {
            nameTable.put(i | 0x10000000, reader.readString());
        }

        return nameTable;
    }
}
