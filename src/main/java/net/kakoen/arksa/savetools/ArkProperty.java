package net.kakoen.arksa.savetools;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.struct.ArkQuat;
import net.kakoen.arksa.savetools.struct.ArkVector;

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
		String key = byteBuffer.readName();
		if (key == null || key.equals("None")) {
			return null;
		}

		String type = byteBuffer.readName();
		int dataSize = byteBuffer.readInt();
		int position = byteBuffer.readInt();

		switch(type) {
			case "BoolProperty":
				boolean value = byteBuffer.readShort() != 0;
				return new ArkProperty<>(key, type, position, (byte)0, value);
			case "FloatProperty":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readFloat());
			case "NameProperty":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readName());
			case "IntProperty":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readInt());
			case "Int8Property":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readByte());
			case "DoubleProperty":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readDouble());
			case "UInt32Property":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readUInt32());
			case "UInt16Property":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readUInt16());
			case "Int16Property":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readShort());
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
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), readStructProperty(byteBuffer, dataSize, structType, false));
			case "ObjectProperty":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), readObjectProperty(byteBuffer));
			case "SoftObjectProperty":
				return new ArkProperty<>(key, type, position, byteBuffer.readByte(), byteBuffer.readBytesAsHex(dataSize));
			case "ArrayProperty":
				String arrayType = byteBuffer.readName();
				byte endOfStruct = byteBuffer.readByte();
				int arrayLength = byteBuffer.readInt();
				int bufferPosition = byteBuffer.getPosition();
				if (arrayType.equals("StructProperty")) {
					try {
						List<ArkProperty<?>> structArray = readStructArray(byteBuffer, arrayType, arrayLength);
						if (byteBuffer.getPosition() != bufferPosition + dataSize - 4) {
							log.error("Struct array read incorrectly, position doesn't match size: {}", structArray);
							throw new Exception("Struct array read incorrectly, position doesn't match size (reading as binary data instead)");
						}
						return new ArrayProperty<>(key, type, position, endOfStruct, arrayType, arrayLength, structArray, null);
					} catch(Exception e) {
						log.error("Failed to read struct array", e);
						byteBuffer.setPosition(bufferPosition);
						return new ArrayProperty<>(key, type, position, endOfStruct, arrayType, arrayLength, byteBuffer.bytesToHex(byteBuffer.readBytes(dataSize - 4)), null);
					}
				} else {
					return new ArrayProperty<>(key, type, position, endOfStruct, arrayType, arrayLength, byteBuffer.bytesToHex(byteBuffer.readBytes(dataSize - 4)), null);
				}
			default:
				throw new RuntimeException("Unknown property type " + type);
		}
	}

	private static List<ArkProperty<?>> readStructArray(ArkBinaryData byteBuffer, String arrayType, int count) {
		List<ArkProperty<?>> structArray = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			structArray.add(readProperty(byteBuffer, true));
		}
		return structArray;
	}

	private static Object readObjectProperty(ArkBinaryData byteBuffer) {
		boolean isName = byteBuffer.readShort() == 1;
		if (isName) {
			return byteBuffer.readName();
		} else {
			return byteBuffer.readUUID();
		}
	}

	private static Object readStructProperty(ArkBinaryData byteBuffer, int dataSize, String structType, boolean inArray) {
		if (!inArray) {
			byteBuffer.skipBytes(16);
		}
		if (structType.equals("Vector")) {
			return new ArkVector(byteBuffer);
		} else if (structType.equals("Quat")) {
			return new ArkQuat(byteBuffer);
		} else {
			List<ArkProperty<?>> properties = new ArrayList<>();
			ArkProperty<?> structProperty = readProperty(byteBuffer);
			while (structProperty != null) {
				properties.add(structProperty);
				structProperty = readProperty(byteBuffer);
			}
			return properties;
		}
	}
}
