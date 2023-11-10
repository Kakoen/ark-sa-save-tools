package net.kakoen.arksa.savetools;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;
import java.util.function.BiPredicate;

@Slf4j
public class ArkSaSaveDatabase implements AutoCloseable {

	private final File sqliteDb;
	private final Connection connection;

	private Map<Integer, String> names;
	private List<String> parts;

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
		while(true) {
			String name = headerData.readString();
			if (name == null) {
				break;
			}
			parts.add(name);
			headerData.readInt();
		}
	}
	public Map<UUID, ArkGameObject> getGameObjects(BiPredicate<String, UUID> filter) throws SQLException, IOException {
		try(ResultSet resultSet = connection.createStatement().executeQuery("SELECT key, value FROM game")) {
			Map<UUID, ArkGameObject> gameObjects = new HashMap<>();
			while (resultSet.next()) {
				UUID uuid = byteArrayToUUID(resultSet.getBytes("key"));
				ArkBinaryData byteBuffer = new ArkBinaryData(resultSet.getBytes("value"), names);
				String className = byteBuffer.readName();
				if (className != null && filter.test(className, uuid)) {
//					File outFile = new File("/tmp/out/" + className + "/" + uuid + ".bin");
//					outFile.getParentFile().mkdirs();
//					Files.write(outFile.toPath(), resultSet.getBytes("value"));
					int startOfObject = byteBuffer.getPosition();
					try {
						gameObjects.put(uuid, new ArkGameObject(uuid, className, byteBuffer));
					} catch(Exception e) {
						log.error("Error parsing " + uuid + ", debug info following", e);
						ArkSaveUtils.enableDebugLogging = true;
						byteBuffer.setPosition(startOfObject);
						new ArkGameObject(uuid, className, byteBuffer);
						ArkSaveUtils.enableDebugLogging = false;
						throw e;
					}
				}
			}
			return gameObjects;
		}
	}

	private void readNames(ArkBinaryData headerData) {
		names = new HashMap<>();
		while(headerData.hasMore()) {
			int id = headerData.readInt();
			String idName = headerData.readString();
			names.put(id, idName);
		}
	}

	public ArkBinaryData getCustomValue(String key) throws SQLException {
		ResultSet resultSet = connection.createStatement().executeQuery("SELECT value FROM custom WHERE key = '" + key + "' LIMIT 1");
		if(resultSet.next()) {
			return new ArkBinaryData(resultSet.getBytes("value"));
		}
		return null;
	}

	@Override
	public void close() throws Exception {
		if(connection != null && !connection.isClosed()) {
			connection.close();
		}
	}

	public ArkGameObject getGameObjectById(UUID uuid) throws SQLException {
		String query = "SELECT key, value FROM game WHERE key = ? LIMIT 1";
		try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
			preparedStatement.setBytes(1, UUIDToByteArray(uuid));
			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (resultSet.next()) {
					UUID actualUuid = byteArrayToUUID(resultSet.getBytes("key"));
					ArkBinaryData byteBuffer = new ArkBinaryData(resultSet.getBytes("value"), names);
					String className = byteBuffer.readName();
					return new ArkGameObject(actualUuid, className, byteBuffer);
				}
			}
		}
		return null;
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
