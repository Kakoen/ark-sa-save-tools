package net.kakoen.arksa.savetools.property;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.ArkBinaryData;
import net.kakoen.arksa.savetools.ArkPropertyContainer;
import net.kakoen.arksa.savetools.struct.ArkStructType;
import net.kakoen.arksa.savetools.struct.UnknownStruct;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Slf4j
public class ArkProperty<T> {

	private String name;
	private String type;
	private T value;

	@JsonInclude(JsonInclude.Include.NON_DEFAULT)
	private int position;

	@JsonIgnore
	private byte unknownByte;

	public ArkProperty(String name, String type, int position, byte unknownByte, T value) {
		this.name = name;
		this.type = type;
		this.position = position;
		this.unknownByte = unknownByte;
		this.value = value;
	}

	public static ArkProperty<?> readProperty(ArkBinaryData byteBuffer) {
		return readProperty(byteBuffer, false);
	}

	public static ArkProperty<?> readProperty(ArkBinaryData byteBuffer, boolean inArray) {
		String key = byteBuffer.readSingleName();
		int someInt = byteBuffer.readInt();

		if (key == null || key.equals("None")) {
			return null;
		}

		String type = byteBuffer.readName();
		int dataSize = byteBuffer.readInt();
		int position = byteBuffer.readInt();

		int startDataPosition = byteBuffer.getPosition();

		switch(type) {
			case "BoolProperty":
				short value = byteBuffer.readShort();
				return new ArkProperty<>(key, type, position, (byte)0, value != 0);
			case "FloatProperty":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readFloat());
			case "NameProperty":
				byte unknownByte = byteBuffer.readByte();
				String name = byteBuffer.readSingleName();
				int unknownInt = byteBuffer.readInt();
				return new ArkProperty<>(key, type, position, unknownByte, name);
			case "IntProperty":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readInt());
			case "Int8Property":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readByte());
			case "DoubleProperty":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readDouble());
			case "UInt32Property":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readUInt32());
			case "UInt64Property":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readUInt64());
			case "UInt16Property":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readUInt16());
			case "Int16Property":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readShort());
			case "Int64Property":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readLong());
			case "StrProperty":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readString());
			case "ByteProperty":
				String enumType = byteBuffer.readName();
				if(enumType.equals("None")) {
					return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readByte());
				} else {
					return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readName());
				}
			case "StructProperty":
				String structType = byteBuffer.readName();
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), readStructProperty(byteBuffer, dataSize, structType, inArray));
			case "ObjectProperty":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), readObjectProperty(byteBuffer));
			case "SoftObjectProperty":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readBytesAsHex(dataSize));
			case "ArrayProperty":
				return readArrayProperty(key, type, position, byteBuffer, dataSize);
			case "MapProperty":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), readProperty(byteBuffer));
			default:
				throw new RuntimeException("Unknown property type " + type + " with data size " + dataSize + " at position " + startDataPosition);
		}
	}
	private static List<Object> readStructArray(ArkBinaryData byteBuffer, String arrayType, int count) {
		List<Object> structArray = new ArrayList<>();
		String name = byteBuffer.readName();
		String type = byteBuffer.readName();
		int dataSize = byteBuffer.readInt();
		int position = byteBuffer.readInt();
		String structType = byteBuffer.readName();
		byte unknownByte = byteBuffer.readByte();
		byteBuffer.skipBytes(16);
		for (int i = 0; i < count; i++) {
			structArray.add(readStructProperty(byteBuffer, dataSize, structType, true));
		}
		return structArray;
	}

	private static Object readObjectProperty(ArkBinaryData byteBuffer) {
		boolean isName = byteBuffer.readShort() == 1;
		if (isName) {
			return byteBuffer.readName();
		} else {
			return byteBuffer.readUUIDAsString();
		}
	}

	private static Object readStructProperty(ArkBinaryData byteBuffer, int dataSize, String structType, boolean inArray) {
		if (!inArray)
			byteBuffer.skipBytes(16);

		ArkStructType arkStructType = ArkStructType.fromTypeName(structType);
		if (arkStructType != null) {
			return arkStructType.getConstructor().apply(byteBuffer);
		}

		int position = byteBuffer.getPosition();

		try {
			ArkPropertyContainer properties = readStructProperties(byteBuffer);

			if (byteBuffer.getPosition() != position + dataSize && !inArray) {
				throw new Exception(String.format("Position %d before reading struct type %s of size %d, expecting end at %d, but was %d after reading struct", position, structType, dataSize, position + dataSize, byteBuffer.getPosition()));
			}
			return properties;
		} catch(Exception e) {
			log.error("Failed to read struct, reading as blob", e);
			byteBuffer.setPosition(position);
			byte[] data = byteBuffer.readBytes(dataSize);
			byteBuffer.debugBinaryData(data);
			return new UnknownStruct(structType, byteBuffer.bytesToHex(data));
		}
	}

	private static ArkPropertyContainer readStructProperties(ArkBinaryData byteBuffer) {
		List<ArkProperty<?>> properties = new ArrayList<>();
		ArkProperty<?> structProperty = readProperty(byteBuffer);
		while (structProperty != null) {
			properties.add(structProperty);
			structProperty = readProperty(byteBuffer);
		}
		return new ArkPropertyContainer(properties);
	}

	private static ArkProperty<?> readArrayProperty(String key, String type, int position, ArkBinaryData byteBuffer, int dataSize) {
		String arrayType = byteBuffer.readName();
		byte endOfStruct = byteBuffer.readByte();
		int arrayLength = byteBuffer.readInt();
		int bufferPosition = byteBuffer.getPosition();
		if (arrayType.equals("StructProperty")) {
			try {
				List<Object> structArray = readStructArray(byteBuffer, arrayType, arrayLength);
				int bytesLeft = bufferPosition + dataSize - 4 - byteBuffer.getPosition();
				if (bytesLeft != 0) {
					log.error("Struct array read incorrectly, bytes left to read {}: {}", bytesLeft, structArray);
					if (bytesLeft > 0) {
						byteBuffer.debugBinaryData(byteBuffer.readBytes(bytesLeft));
					}
					throw new Exception(String.format("Struct array read incorrectly, bytes left: %d (reading as binary data instead)", bytesLeft));
				}
				return new ArrayProperty<>(key, type, position, endOfStruct, arrayType, arrayLength, structArray, null);
			} catch(Exception e) {
				log.error("Failed to read struct array", e);
				byteBuffer.setPosition(bufferPosition);
				byte[] data = byteBuffer.readBytes(dataSize - 4);
				byteBuffer.debugBinaryData(data);
				return new ArrayProperty<>(key, type, position, endOfStruct, arrayType, arrayLength, byteBuffer.bytesToHex(data), null);
			}
		} else {
			return new ArrayProperty<>(key, type, position, endOfStruct, arrayType, arrayLength, byteBuffer.bytesToHex(byteBuffer.readBytes(dataSize - 4)), null);
		}
	}
}
