package net.kakoen.arksa.savetools;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.utils.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

@Slf4j
public class ArkSaSaveDatabase implements AutoCloseable {

    private final File sqliteDb;
    private final Connection connection;

    @Getter
    private final SaveContext saveContext = new SaveContext();
    private final static int MAX_IN_LIST = 10000;

    public ArkSaSaveDatabase(File arkFile) throws SQLException {
        this.sqliteDb = arkFile;
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + arkFile.getAbsolutePath());
        readHeader();
        readActorLocations();
    }

    private void readActorLocations() throws SQLException {
        ArkBinaryData actorTransforms = getCustomValue("ActorTransforms");
        if (actorTransforms != null) {
            saveContext.setActorLocations(actorTransforms.readActorTransforms());
        }
    }

    private void readHeader() throws SQLException {
        ArkBinaryData headerData = getCustomValue("SaveHeader");
        saveContext.setSaveVersion(headerData.readShort());
        int nameTableOffset = headerData.readInt();
        saveContext.setGameTime(headerData.readDouble());

        saveContext.setParts(readParts(headerData));

        // Unknown data, seems to be always 0...
        headerData.expect(0, headerData.readInt());
        headerData.expect(0, headerData.readInt());

        headerData.setPosition(nameTableOffset);
        saveContext.setNames(readNames(headerData));
    }

    private List<String> readParts(ArkBinaryData headerData) {
        List<String> parts = new ArrayList<>();
        long numberOfParts = headerData.readUInt32();
        for (int i = 0; i < numberOfParts; i++) {
            String name = headerData.readString();
            parts.add(name);
            headerData.readInt();
        }

        return parts;
    }

    public Map<UUID, ArkGameObject> getGameObjects(GameObjectReaderConfiguration readerConfiguration) throws SQLException, IOException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT key, value FROM game")) {

            Map<UUID, ArkGameObject> gameObjects = new HashMap<>();
            while (resultSet.next()) {
                UUID uuid = byteArrayToUUID(resultSet.getBytes("key"));
                if (readerConfiguration.getUuidFilter() != null && !readerConfiguration.getUuidFilter().test(uuid)) {
                    continue;
                }

                ArkBinaryData byteBuffer = new ArkBinaryData(resultSet.getBytes("value"), saveContext);
                String className = byteBuffer.readName();
                if (readerConfiguration.getBlueprintNameFilter() != null && !readerConfiguration.getBlueprintNameFilter().test(Optional.ofNullable(className))) {
                    continue;
                }

                int startOfObject = byteBuffer.getPosition();
                try {
                    ArkGameObject arkGameObject = new ArkGameObject(uuid, className, byteBuffer);
                    if (readerConfiguration.getGameObjectFilter() != null && !readerConfiguration.getGameObjectFilter().test(arkGameObject)) {
                        continue;
                    }

                    gameObjects.put(uuid, arkGameObject);

                    if (readerConfiguration.getJsonFilesOutputDirectory() != null) {
                        Path jsonFile = readerConfiguration.getJsonFilesOutputDirectory().resolve(removeLeadingSlash(Optional.ofNullable(className).orElse("")).resolve(uuid + ".json"));
                        ArkSaveUtils.debugLog("Writing " + jsonFile);
                        JsonUtils.writeJsonToFile(arkGameObject, jsonFile);
                    }

                    if (readerConfiguration.getBinaryFilesOutputDirectory() != null) {
                        Path binFile = readerConfiguration.getBinaryFilesOutputDirectory().resolve(removeLeadingSlash(Optional.ofNullable(className).orElse("")).resolve(uuid + ".bin"));
                        Files.createDirectories(binFile.getParent());
                        ArkSaveUtils.debugLog("Writing " + binFile);
                        Files.write(binFile, resultSet.getBytes("value"));
                    }
                } catch (Exception e) {
                    log.error("Error parsing " + uuid + " of type " + className + ", debug info following", e);
                    ArkSaveUtils.enableDebugLogging = true;
                    byteBuffer.setPosition(startOfObject);
                    try {
                        new ArkGameObject(uuid, className, byteBuffer);
                    } catch (Exception ignored) {

                    }
                    ArkSaveUtils.enableDebugLogging = false;

                    if (readerConfiguration.isThrowExceptionOnParseError()) {
                        throw e;
                    }
                }
            }


            return gameObjects;
        }
    }

    private Path removeLeadingSlash(String path) {
        if (path.startsWith("/")) {
            return Path.of(path.substring(1));
        }
        return Path.of(path);
    }

    private Map<Integer, String> readNames(ArkBinaryData headerData) {
        Map<Integer, String> names = new HashMap<>();
        int numberOfNames = headerData.readInt();
        for (int i = 0; i < numberOfNames; i++) {
            int id = headerData.readInt();
            String name = headerData.readString();
            names.put(id, name);
        }
        return names;
    }

    public ArkBinaryData getCustomValue(String key) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT value FROM custom WHERE key = '" + key + "' LIMIT 1")) {

            if (resultSet.next()) {
                return new ArkBinaryData(resultSet.getBytes("value"), saveContext);
            }
            return null;
        }
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public Map<UUID, ArkGameObject> getGameObjectsByIds(Collection<UUID> uuids) throws SQLException {
        return getGameObjectsByIds(uuids, GameObjectParserConfiguration.builder().build());
    }

    public Map<UUID, ArkGameObject> getGameObjectsByIds(Collection<UUID> uuids, GameObjectParserConfiguration objectParserConfiguration) throws SQLException {
        if (uuids.isEmpty()) {
            return Collections.emptyMap();
        }

        if (uuids.size() > MAX_IN_LIST) {
            Map<UUID, ArkGameObject> gameObjects = new HashMap<>();
            List<UUID> uuidList = new ArrayList<>(uuids);
            for (int i = 0; i < uuidList.size(); i += MAX_IN_LIST) {
                gameObjects.putAll(getGameObjectsByIds(uuidList.subList(i, Math.min(i + MAX_IN_LIST, uuidList.size())), objectParserConfiguration));
            }
            return gameObjects;
        }

        String placeholders = String.join(",", Collections.nCopies(uuids.size(), "?"));
        String query = "SELECT key, value FROM game WHERE key IN (" + placeholders + ")";

        Map<UUID, ArkGameObject> gameObjects = new HashMap<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            // Set UUID parameters
            int parameterIndex = 1;
            for (UUID uuid : uuids) {
                preparedStatement.setBytes(parameterIndex++, UUIDToByteArray(uuid));
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    UUID actualUuid = byteArrayToUUID(resultSet.getBytes("key"));
                    ArkBinaryData byteBuffer = new ArkBinaryData(resultSet.getBytes("value"), saveContext);
                    try {
                        String className = byteBuffer.readName();
                        gameObjects.put(actualUuid, new ArkGameObject(actualUuid, className, byteBuffer));
                    } catch (Exception e) {
                        log.error("Failed reading gameObject with UUID {}, skipping...", actualUuid, e);
                        byteBuffer.setPosition(0);
                        log.error("Data: {}", byteBuffer.readBytesAsHex(byteBuffer.size()));

                        if(objectParserConfiguration.isThrowExceptionOnParseError()) {
                            throw e;
                        }
                    }
                }
            }
        }
        return gameObjects;
    }

    public ArkGameObject getGameObjectById(UUID uuid) throws SQLException {
        return getGameObjectsByIds(Collections.singleton(uuid), GameObjectParserConfiguration.builder().throwExceptionOnParseError(true).build()).get(uuid);
    }

    public static UUID byteArrayToUUID(final byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }

    public static byte[] UUIDToByteArray(final UUID uuid) {
        final ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        return buf.array();
    }
}
