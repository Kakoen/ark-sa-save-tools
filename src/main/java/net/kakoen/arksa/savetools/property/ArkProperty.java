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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

        ArkValueType valueType = byteBuffer.readValueTypeByName();

        int dataSize = byteBuffer.readInt();
        int position = byteBuffer.readInt();

        int startDataPosition = byteBuffer.getPosition();

        switch (valueType) {
            case Boolean:
                return new ArkProperty<>(key, valueType.getName(), position, (byte) 0, readPropertyValue(valueType, byteBuffer));
            case Float, Int, Int8, Double, UInt32, UInt64, UInt16, Int16, Int64, String, Name, SoftObject, Object:
                return new ArkProperty<>(key, valueType.getName(), position, byteBuffer.readByte(), readPropertyValue(valueType, byteBuffer));
            case Byte:
                String enumType = byteBuffer.readName();
                if (enumType.equals("None")) {
                    return new ArkProperty<>(key, valueType.getName(), position, byteBuffer.readByte(), byteBuffer.readUnsignedByte());
                } else {
                    return new ArkProperty<>(key, valueType.getName(), position, byteBuffer.readByte(), byteBuffer.readName());
                }
            case Struct:
                String structType = byteBuffer.readName();
                return new ArkProperty<>(key, valueType.getName(), position, byteBuffer.readByte(), readStructProperty(byteBuffer, dataSize, structType, inArray));
            case Array:
                return readArrayProperty(key, valueType.getName(), position, byteBuffer, dataSize);
            case Map:
                return readMapProperty(key, valueType.getName(), position, byteBuffer, dataSize);
            case Set:
                return readSetProperty(key, valueType.getName(), position, byteBuffer, dataSize);
            default:
                throw new RuntimeException("Unsupported property type " + valueType + " with data size " + dataSize + " at position " + startDataPosition);
        }
    }

    private static ArkProperty<?> readMapProperty(String key, String valueTypeName, int position, ArkBinaryData byteBuffer, int dataSize) {
        ArkValueType keyType = byteBuffer.readValueTypeByName();
        ArkValueType valueType = byteBuffer.readValueTypeByName();

        byte unknownByte = byteBuffer.readByte();
        int startOfData = byteBuffer.getPosition();
        byteBuffer.expect(0, byteBuffer.readInt());
        int count = byteBuffer.readInt();

        List<ArkProperty<?>> mapEntries = new ArrayList<>();
        try {
            for(int i = 0; i < count; i++) {
                if(valueType == ArkValueType.Struct) {
                    mapEntries.add(readStructMap(keyType, byteBuffer));
                } else {
                    log.error("Unsupported map value type {}", valueType);
                    throw new IllegalStateException("Unsupported Map value: " + valueType);
                }
            }
        } finally {
            if(byteBuffer.getPosition() != startOfData + dataSize) {
                log.error("Map read incorrectly, bytes left to read {}: {}", startOfData + dataSize - byteBuffer.getPosition(), mapEntries);
                byteBuffer.debugBinaryData(byteBuffer.readBytes(startOfData + dataSize - byteBuffer.getPosition()));
            }
        }

        return new ArkProperty<>(key, valueTypeName, position, unknownByte, new ArkPropertyContainer(mapEntries));
    }

    private static ArkProperty<?> readStructMap(ArkValueType keyType, ArkBinaryData byteBuffer) {
        List<ArkProperty<?>> propertyValues = new ArrayList<>();
        String keyName = readPropertyValue(keyType, byteBuffer, String.class);
        while(true) {
            ArkProperty<?> property = readProperty(byteBuffer);
            if(property == null) {
                break;
            }
            propertyValues.add(property);
        }
        return new ArkProperty<>(keyName, "MapProperty", 0, (byte)0, new ArkPropertyContainer(propertyValues));
    }

    private static ArkProperty<ArkSet> readSetProperty(String key, String valueTypeName, int position, ArkBinaryData byteBuffer, int dataSize) {
        ArkValueType valueType = byteBuffer.readValueTypeByName();
        byte unknownByte = byteBuffer.readByte();
        int startOfData = byteBuffer.getPosition();
        byteBuffer.expect(0, byteBuffer.readInt());
        int count = byteBuffer.readInt();

        Set<Object> values = new LinkedHashSet<>();
        for(int i = 0; i < count; i++) {
            values.add(readPropertyValue(valueType, byteBuffer));
        }

        if(startOfData + dataSize != byteBuffer.getPosition()) {
            log.error("Set read incorrectly, bytes left to read {}: {}", startOfData + dataSize - byteBuffer.getPosition(), values);
        }
        return new ArkProperty<>(key, valueTypeName, position, unknownByte, new ArkSet(valueType, values));

    }

    private static String readSoftObjectPropertyValue(ArkBinaryData byteBuffer) {
        if(!byteBuffer.getSaveContext().hasNameTable()) {
            return "[" + String.join(", ", byteBuffer.readNames()) + "]";
        }

        String result = byteBuffer.readName();
        byteBuffer.expect("00 00 00 00 ", byteBuffer.readBytesAsHex(4));
        return result;
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

    private static ObjectReference readObjectProperty(ArkBinaryData byteBuffer) {
        return new ObjectReference(byteBuffer);
    }

    private static Object readStructProperty(ArkBinaryData byteBuffer, int dataSize, String structType, boolean inArray) {
        if (!inArray) {
            byteBuffer.expect("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ", byteBuffer.readBytesAsHex(16));
        }

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
        int startOfArrayValuesPosition = byteBuffer.getPosition();
        if (arrayType.equals("StructProperty")) {
            try {
                List<Object> structArray = readStructArray(byteBuffer, arrayType, arrayLength);
                int bytesLeft = startOfArrayValuesPosition + dataSize - 4 - byteBuffer.getPosition();
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
                byteBuffer.setPosition(startOfArrayValuesPosition);
                byte[] data = byteBuffer.readBytes(dataSize - 4);
                byteBuffer.debugBinaryData(data);
                return new ArrayProperty<>(key, type, position, endOfStruct, arrayType, arrayLength, byteBuffer.bytesToHex(data));
            }
        } else {
            int expectedEndOfArrayPosition = startOfArrayValuesPosition + dataSize - 4;
            List<Object> array = new ArrayList<>();
            ArkValueType valueType = ArkValueType.fromName(arrayType);
            if (valueType == null) {
                throw new RuntimeException("Unknown array type " + arrayType + " at position " + byteBuffer.getPosition());
            }

            //Read byte arrays as unsigned bytes...
            ArkValueType readValueType = valueType == ArkValueType.Byte ? ArkValueType.Int8 : valueType;

            try {
                for (int i = 0; i < arrayLength; i++) {
                    array.add(readPropertyValue(readValueType, byteBuffer));
                }
                if (expectedEndOfArrayPosition != byteBuffer.getPosition()) {
                    throw new RuntimeException(String.format("Array read incorrectly, bytes left: %d (reading as binary data instead)", expectedEndOfArrayPosition - byteBuffer.getPosition()));
                }
            } catch(Exception e) {
                byteBuffer.setPosition(startOfArrayValuesPosition);
                String content = byteBuffer.bytesToHex(byteBuffer.readBytes(dataSize - 4));
                log.error("Array {} of type {} and length {} read incorrectly at {}, returning blob data instead: {}", key, arrayType, arrayLength, startOfArrayValuesPosition, content, e);
                return new ArrayProperty<>(key, type, position, endOfStruct, arrayType, arrayLength, content);
            }
            return new ArrayProperty<>(key, type, position, endOfStruct, arrayType, arrayLength, array);
        }
    }

    @SuppressWarnings({"unchecked", "unused"})
    private static <T> T readPropertyValue(ArkValueType valueType, ArkBinaryData byteBuffer, Class<T> returnType) {
        return (T)readPropertyValue(valueType, byteBuffer);
    }

    private static Object readPropertyValue(ArkValueType valueType, ArkBinaryData byteBuffer) {
        switch (valueType) {
            case Byte:
                return byteBuffer.readUnsignedByte();
            case Int8:
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
