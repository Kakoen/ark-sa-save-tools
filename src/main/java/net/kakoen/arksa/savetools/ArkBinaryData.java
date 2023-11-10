package net.kakoen.arksa.savetools;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static net.kakoen.arksa.savetools.ArkSaSaveDatabase.byteArrayToUUID;

@Slf4j
public class ArkBinaryData {

	ByteBuffer byteBuffer;

	@Getter
	private Map<Integer, String> names;

	public ArkBinaryData(byte[] data) {
		this.byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
	}

	public ArkBinaryData(byte[] values, Map<Integer, String> names) {
		this.byteBuffer = ByteBuffer.wrap(values).order(ByteOrder.LITTLE_ENDIAN);
		this.names = names;
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
		readBytes(count);
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

	public double readFloat() {
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
			String n = names.get(readInt());
			if(n != null) {
				log.info("Found name: {} at {}", n, i);
				i += 3;
			}
		}
	}

	public void findNamesAndStrings() {
		log.info("--- Looking for names and strings ---");
		for(int i = 0; i < size() - 4; i++) {
			setPosition(i);
			String n = readName();
			if(n != null) {
				setPosition(i);
				String bytes = bytesToHex(readBytes(4));
				log.info("Found name: {} at {} ({})", n, i, bytes);
			} else {
				setPosition(i);
				try {
					String s = readString();
					if(s != null) {
						log.info("Found string: {} at {}", s, i);
					}
				} catch(Exception ignored) {

				}
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
		String name = names.get(readInt());
		int alwaysZero = readInt();
		if(alwaysZero != 0) {
			ArkSaveUtils.debugLog("Always zero is not zero: {}", alwaysZero, new Throwable());
		}
		return name;
	}

	public String readUUID() {
		return byteArrayToUUID(readBytes(16)).toString();
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

}
