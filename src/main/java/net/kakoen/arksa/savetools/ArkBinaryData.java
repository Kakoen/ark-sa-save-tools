package net.kakoen.arksa.savetools;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.struct.ActorTransform;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static net.kakoen.arksa.savetools.ArkSaSaveDatabase.byteArrayToUUID;

@Slf4j
public class ArkBinaryData {

	ByteBuffer byteBuffer;

	@Getter
	private SaveContext saveContext;

	public ArkBinaryData(byte[] data) {
		this.byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
	}

	public ArkBinaryData(byte[] values, SaveContext saveContext) {
		this.byteBuffer = ByteBuffer.wrap(values).order(ByteOrder.LITTLE_ENDIAN);
		this.saveContext = saveContext;
	}

	public String readString() {
		int length = readInt();
		if(length == 0) {
			return null;
		}

		boolean isMultiByte = length < 0;

		if (isMultiByte) {
			String result = new String(readChars(Math.abs(length)), 0 , Math.abs(length) - 1);
			skipBytes(Math.abs(length) * 2);
			return result;
		}

		String result = new String(readBytes(length - 1), StandardCharsets.UTF_8);
		skipBytes(1);
		return result;
	}

	private char[] readChars(int size) {
		char[] buffer = new char[size];
		byteBuffer.asCharBuffer().get(buffer, 0, size);
		return buffer;
	}

	public void skipBytes(int count) {
		setPosition(getPosition() + count);
	}

	public int readInt() {
		return byteBuffer.getInt();
	}

	public boolean hasMore() {
		return byteBuffer.hasRemaining();
	}

	public byte[] readBytes(int count) {
		byte[] bytes = new byte[count];
		byteBuffer.get(bytes);
		return bytes;
	}

	public boolean readBoolean() {
		return readInt() != 0;
	}

	public float readFloat() {
		return byteBuffer.getFloat();
	}

	public void logRestOfDataInHexForm() {
		byte[] bytes = new byte[byteBuffer.remaining()];
		byteBuffer.get(bytes);
		System.out.println(bytesToHex(bytes));
	}

	public String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for(byte b : bytes) {
			sb.append(String.format("%02X ", b));
		}
		return sb.toString();
	}

	public int size() {
		return byteBuffer.limit();
	}

	public void setPosition(int i) {
		byteBuffer.position(i);
	}

	public void findNames() {
		log.info("--- Looking for names ---");
		for(int i = 0; i < size() - 4; i++) {
			setPosition(i);
			String n = saveContext.getNames().get(readInt());
			if(n != null) {
				log.info("Found name: {} at {}", n, i);
				i += 3;
			}
		}
	}

	public double readDouble() {
		return byteBuffer.getDouble();
	}

	public byte readByte() {
		return byteBuffer.get();
	}

	public String readName() {
		String name = saveContext.getNames().get(readInt());
		int alwaysZero = readInt();
		if(alwaysZero != 0) {
			ArkSaveUtils.debugLog("Always zero is not zero: {}", alwaysZero, new Throwable());
		}
		return name;
	}

	public String readUUIDAsString() {
		return readUUID().toString();
	}

	public UUID readUUID() {
		return byteArrayToUUID(readBytes(16));
	}

	public short readShort() {
		return byteBuffer.getShort();
	}

	public String readBytesAsHex(int dataSize) {
		return bytesToHex(readBytes(dataSize));
	}

	public int getPosition() {
		return byteBuffer.position();
	}

	public long readUInt32() {
		return Integer.toUnsignedLong(readInt());
	}

	public int readUInt16() {
		return Short.toUnsignedInt(readShort());
	}

	public void debugBinaryData(byte[] data) {
		log.error("Data that was not recognized: " + bytesToHex(data));
		new ArkBinaryData(data, saveContext).findNames();
	}

	public BigInteger readUInt64() {
		return new BigInteger(Long.toUnsignedString(byteBuffer.getLong()));
	}

	public String readSingleName() {
		return saveContext.getNames().get(readInt());
	}

	public long readLong() {
		return byteBuffer.getLong();
	}

	public Map<UUID, ActorTransform> readActorTransforms() {
		Map<UUID, ActorTransform> actorTransforms = new HashMap<>();
		UUID terminationUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
		UUID uuid = readUUID();
		while(!uuid.equals(terminationUUID)) {
			actorTransforms.put(uuid, new ActorTransform(this));
			uuid = readUUID();
		}
		return actorTransforms;
	}

	public void expect(Object expected, Object read) {
		if (!Objects.equals(expected, read)) {
			log.warn("Unexpected data, expected {}, but was {}", expected, read, new Throwable());
		}
	}
}
