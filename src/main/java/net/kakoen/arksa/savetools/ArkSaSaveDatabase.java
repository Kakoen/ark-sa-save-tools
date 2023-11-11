package net.kakoen.arksa.savetools;

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

    private Map<Integer, String> names;
    private List<String> parts;
    private final static int MAX_IN_LIST = 10000;

    public ArkSaSaveDatabase(File arkFile) throws SQLException {
        this.sqliteDb = arkFile;
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + arkFile.getAbsolutePath());
        readHeader();
    }

    private void readHeader() throws SQLException {
        ArkBinaryData headerData = getCustomValue("SaveHeader");
        headerData.skipBytes(18);
        readParts(headerData);
        headerData.skipBytes(8);
        readNames(headerData);
    }

    private void readParts(ArkBinaryData headerData) {
        parts = new ArrayList<>();
        while (true) {
            String name = headerData.readString();
            if (name == null) {
                break;
            }
            parts.add(name);
            headerData.readInt();
        }
    }

    public Map<UUID, ArkGameObject> getGameObjects(GameObjectReaderConfiguration readerConfiguration) throws SQLException, IOException {
        try (ResultSet resultSet = connection.createStatement().executeQuery("SELECT key, value FROM game")) {
            Map<UUID, ArkGameObject> gameObjects = new HashMap<>();
            while (resultSet.next()) {
                UUID uuid = byteArrayToUUID(resultSet.getBytes("key"));
                if (readerConfiguration.getUuidFilter() != null && !readerConfiguration.getUuidFilter().test(uuid)) {
                    continue;
                }

                ArkBinaryData byteBuffer = new ArkBinaryData(resultSet.getBytes("value"), names);
                String className = byteBuffer.readName();
                if (readerConfiguration.getClassNameFilter() != null && !readerConfiguration.getClassNameFilter().test(Optional.ofNullable(className))) {
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
                    log.error("Error parsing " + uuid + ", debug info following", e);
                    ArkSaveUtils.enableDebugLogging = true;
                    byteBuffer.setPosition(startOfObject);
                    new ArkGameObject(uuid, className, byteBuffer);
                    ArkSaveUtils.enableDebugLogging = false;
                    throw e;
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

    private void readNames(ArkBinaryData headerData) {
        names = new HashMap<>();
        while (headerData.hasMore()) {
            int id = headerData.readInt();
            String idName = headerData.readString();
            names.put(id, idName);
        }
    }

    public ArkBinaryData getCustomValue(String key) throws SQLException {
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT value FROM custom WHERE key = '" + key + "' LIMIT 1");
        if (resultSet.next()) {
            return new ArkBinaryData(resultSet.getBytes("value"));
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    public Map<UUID, ArkGameObject> getGameObjectsByIds(Collection<UUID> uuids) throws SQLException {
        if (uuids.isEmpty()) {
            return Collections.emptyMap();
        }

        if (uuids.size() > MAX_IN_LIST) {
            Map<UUID, ArkGameObject> gameObjects = new HashMap<>();
            List<UUID> uuidList = new ArrayList<>(uuids);
            for (int i = 0; i < uuidList.size(); i += MAX_IN_LIST) {
                gameObjects.putAll(getGameObjectsByIds(uuidList.subList(i, Math.min(i + MAX_IN_LIST, uuidList.size()))));
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
                    ArkBinaryData byteBuffer = new ArkBinaryData(resultSet.getBytes("value"), names);
                    String className = byteBuffer.readName();
                    gameObjects.put(actualUuid, new ArkGameObject(actualUuid, className, byteBuffer));
                }
            }
        }
        return gameObjects;
    }

    public ArkGameObject getGameObjectById(UUID uuid) throws SQLException {
        return getGameObjectsByIds(Collections.singleton(uuid)).get(uuid);
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
