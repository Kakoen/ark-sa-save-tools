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

        String valueTypeName = byteBuffer.readName();
        if(valueTypeName == null) {
            return null;
        }

        ArkValueType valueType = ArkValueType.fromName(valueTypeName);
        if (valueType == null) {
            throw new RuntimeException("Unknown property type " + valueTypeName + " at position " + byteBuffer.getPosition());
        }

        int dataSize = byteBuffer.readInt();
        int position = byteBuffer.readInt();

        int startDataPosition = byteBuffer.getPosition();

        switch (valueType) {
            case Boolean:
                return new ArkProperty<>(key, valueTypeName, position, (byte) 0, readPropertyValue(valueType, byteBuffer));
            case Float, Int, Int8, Double, UInt32, UInt64, UInt16, Int16, Int64, String, Name, SoftObject, Object:
                return new ArkProperty<>(key, valueTypeName, position, byteBuffer.readByte(), readPropertyValue(valueType, byteBuffer));
            case Byte:
                String enumType = byteBuffer.readName();
                if (enumType.equals("None")) {
                    return new ArkProperty<>(key, valueTypeName, position, byteBuffer.readByte(), byteBuffer.readByte());
                } else {
                    return new ArkProperty<>(key, valueTypeName, position, byteBuffer.readByte(), byteBuffer.readName());
                }
            case Struct:
                String structType = byteBuffer.readName();
                return new ArkProperty<>(key, valueTypeName, position, byteBuffer.readByte(), readStructProperty(byteBuffer, dataSize, structType, inArray));
            case Array:
                return readArrayProperty(key, valueTypeName, position, byteBuffer, dataSize);
            case Map:
                return new ArkProperty<>(key, valueTypeName, position, byteBuffer.readByte(), readProperty(byteBuffer));
            default:
                throw new RuntimeException("Unknown property type " + valueTypeName + " with data size " + dataSize + " at position " + startDataPosition);
        }
    }

    private static String readSoftObjectPropertyValue(ArkBinaryData byteBuffer) {
        String objName = byteBuffer.readSingleName();
        byteBuffer.expect("00 00 00 00 00 00 00 00 ", byteBuffer.readBytesAsHex(8)); //Could it be a UUID?
        return objName;
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

    private static String readObjectProperty(ArkBinaryData byteBuffer) {
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
        } catch (Exception e) {
            log.error("Failed to read struct, reading as blob", e);
            byteBuffer.setPosition(position);
            byte[] data = byteBuffer.readBytes(dataSize);
            byteBuffer.debugBinaryData(data);
            return new UnknownStruct(structType, byteBuffer.bytesToHex(data));
        }
    }

    private static Object readStructProperty(ArkBinaryData byteBuffer, int dataSize, boolean inArray) {
        String structType = byteBuffer.readName();
        return readStructProperty(byteBuffer, dataSize, structType, inArray);
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
                return new ArrayProperty<>(key, type, position, endOfStruct, arrayType, arrayLength, structArray);
            } catch (Exception e) {
                log.error("Failed to read struct array", e);
                byteBuffer.setPosition(bufferPosition);
                byte[] data = byteBuffer.readBytes(dataSize - 4);
                byteBuffer.debugBinaryData(data);
                return new ArrayProperty<>(key, type, position, endOfStruct, arrayType, arrayLength, byteBuffer.bytesToHex(data));
            }
        } else {
            int expectedEndOfArrayPosition = bufferPosition + dataSize - 4;
            List<Object> array = new ArrayList<>();
            ArkValueType valueType = ArkValueType.fromName(arrayType);
            if (valueType == null) {
                throw new RuntimeException("Unknown array type " + arrayType + " at position " + byteBuffer.getPosition());
            }

            for (int i = 0; i < arrayLength; i++) {
                array.add(readPropertyValue(valueType, byteBuffer));
            }
            if (expectedEndOfArrayPosition != byteBuffer.getPosition()) {
                log.error("Array read incorrectly, bytes left to read {}: {}", expectedEndOfArrayPosition - byteBuffer.getPosition(), array);
                byteBuffer.debugBinaryData(byteBuffer.readBytes(expectedEndOfArrayPosition - byteBuffer.getPosition()));
                throw new RuntimeException(String.format("Array read incorrectly, bytes left: %d (reading as binary data instead)", expectedEndOfArrayPosition - byteBuffer.getPosition()));
            }
            return new ArrayProperty<>(key, type, position, endOfStruct, arrayType, arrayLength, array);
        }
    }

    private static Object readPropertyValue(ArkValueType valueType, ArkBinaryData byteBuffer) {
        switch (valueType) {
            case Byte, Int8:
                return byteBuffer.readByte();
            case Double:
                return byteBuffer.readDouble();
            case Float:
                return byteBuffer.readFloat();
            case Int:
                return byteBuffer.readInt();
            case Object:
                return readObjectProperty(byteBuffer);
            case String:
                return byteBuffer.readString();
            case UInt32:
                return byteBuffer.readUInt32();
            case UInt64:
                return byteBuffer.readUInt64();
            case UInt16:
                return byteBuffer.readUInt16();
            case Int16:
                return byteBuffer.readShort();
            case Int64:
                return byteBuffer.readLong();
            case Name:
                return byteBuffer.readName();
            case Boolean:
                return byteBuffer.readShort() == 1;
            case Struct:
                return readStructProperty(byteBuffer, byteBuffer.readInt(), true);
            case SoftObject:
                return readSoftObjectPropertyValue(byteBuffer);
            default:
                throw new RuntimeException("Cannot read value type yet: " + valueType + " at position " + byteBuffer.getPosition());
        }
    }
}
