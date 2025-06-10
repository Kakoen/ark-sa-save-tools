package net.kakoen.arksa.savetools.property;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.ArkBinaryData;
import net.kakoen.arksa.savetools.ArkGenericType;
import net.kakoen.arksa.savetools.ArkPropertyContainer;
import net.kakoen.arksa.savetools.ArkSaveUtils;
import net.kakoen.arksa.savetools.struct.ArkStructType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Data
@NoArgsConstructor
@Slf4j
public class ArkProperty<T> {

    private String name;
    private String type;
    private T value;

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int position;

    public ArkProperty(ArkPropertyHeader header, T value) {
        this(header.getPropertyName(), header.getArkValueType().getName(), header.getPosition(), value);
    }

    public ArkProperty(String name, String type, int position, T value) {
        this.name = name;
        this.type = type;
        this.position = position;
        this.value = value;
    }

    public static ArkProperty<?> readProperty(ArkBinaryData byteBuffer) {
        return readProperty(byteBuffer, false);
    }

    public static ArkProperty<?> readProperty(ArkBinaryData byteBuffer, boolean inArray) {
        ArkPropertyHeader propertyHeader = ArkPropertyHeader.read(byteBuffer);
        if (propertyHeader == null) {
            return null;
        }

        ArkSaveUtils.debugLog("Reading property {}", propertyHeader);

        return switch (propertyHeader.getArkValueType()) {
            case Boolean -> readBooleanProperty(propertyHeader, byteBuffer);
            case Float, Int, Int8, Double, UInt32, UInt64, UInt16, Int16, Int64, String, Name, SoftObject, Object ->
                    readPrimitiveProperty(propertyHeader, byteBuffer);
            case Byte -> readByteProperty(propertyHeader, byteBuffer);
            case Struct -> readStructProperty(propertyHeader, byteBuffer, inArray);
            case Array -> readArrayProperty(propertyHeader, byteBuffer);
            case Map -> readMapProperty(propertyHeader, byteBuffer);
            case Set -> readSetProperty(propertyHeader, byteBuffer);
        };
    }

    private static ArkProperty<?> readStructProperty(ArkPropertyHeader header, ArkBinaryData byteBuffer, boolean inArray) {
        String structType;

        if (!byteBuffer.currentParseContext().useUE55Structure()) {
            header.readLegacyDataSizeAndPosition(byteBuffer);
            structType = byteBuffer.readName();
            byte flag = byteBuffer.readByte();
        } else {
            structType = header.getGenericType().getSubTypes().getFirst().getValue();
        }

        ArkSaveUtils.debugLog("Reading struct property {} of type {}", header, structType);

        return new ArkProperty<>(header, readStructPropertyValue(byteBuffer, header.getDataSize(), structType, inArray));
    }

    private static ArkProperty<?> readByteProperty(ArkPropertyHeader header, ArkBinaryData byteBuffer) {
        String enumType = null;
        if(!byteBuffer.currentParseContext().useUE55Structure()) {
            header.readLegacyDataSizeAndPosition(byteBuffer);
            enumType = byteBuffer.readName();
            byte flags = byteBuffer.readByte();
        } else {
            if (header.getGenericType().hasSubTypes()) {
                enumType = header.getGenericType().getSubTypes().getFirst().getValue();
            }
        }

        if (enumType == null || enumType.equals("None")) {
            return new ArkProperty<>(header, byteBuffer.readUnsignedByte());
        } else {
            return new ArkProperty<>(header, byteBuffer.readName());
        }
    }

    private static ArkProperty<?> readBooleanProperty(ArkPropertyHeader header, ArkBinaryData byteBuffer) {
        if (!byteBuffer.currentParseContext().useUE55Structure()) {
            header.readLegacyDataSizeAndPosition(byteBuffer);
        }

        return new ArkProperty<>(header, readPropertyValue(header.getArkValueType(), byteBuffer));
    }

    private static ArkProperty<?> readPrimitiveProperty(ArkPropertyHeader header, ArkBinaryData byteBuffer) {
        if (!byteBuffer.currentParseContext().useUE55Structure()) {
            header.readLegacyDataSizeAndPosition(byteBuffer);
            byte unknownByte = byteBuffer.readByte();
        }

        return new ArkProperty<>(header, ensureBytesRead(header, byteBuffer, header.getDataSize(),
            () -> readPropertyValue(header.getArkValueType(), byteBuffer)
        ));
    }

    private static ArkProperty<?> readMapProperty(ArkPropertyHeader header, ArkBinaryData byteBuffer) {
        ArkValueType keyType, valueType;
        if (byteBuffer.currentParseContext().useUE55Structure()) {
            List<ArkGenericType> subTypes = header.getGenericType().getSubTypes();
            keyType = ArkValueType.fromGenericType(subTypes.get(0));
            valueType = ArkValueType.fromGenericType(subTypes.get(1));
        } else {
            header.readLegacyDataSizeAndPosition(byteBuffer);
            keyType = ArkValueType.fromName(byteBuffer.readName());
            valueType = ArkValueType.fromName(byteBuffer.readName());
            byte flags = byteBuffer.readByte();
        }

        header.setDataStart(byteBuffer.getPosition());

        List<ArkProperty<?>> value = ensureBytesRead(header, byteBuffer, header.getDataSize(), () -> {
            byteBuffer.expect(0, byteBuffer.readInt());
            int count = byteBuffer.readInt();
            List<ArkProperty<?>> mapEntries = new ArrayList<>();
            for(int i = 0; i < count; i++) {
                mapEntries.add(readStructMap(keyType, byteBuffer));
            }
            return mapEntries;
        });

        return new ArkProperty<>(header, new ArkPropertyContainer(value));
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
        return new ArkProperty<>(keyName, "MapProperty", 0, new ArkPropertyContainer(propertyValues));
    }

    private static ArkProperty<ArkSet> readSetProperty(ArkPropertyHeader header, ArkBinaryData byteBuffer) {
        ArkValueType valueType;

        if (byteBuffer.currentParseContext().useUE55Structure()) {
            ArkGenericType genericType = header.getGenericType().getSubTypes().getFirst();
            valueType = genericType.asValueType();
        } else {
            header.readLegacyDataSizeAndPosition(byteBuffer);
            valueType = byteBuffer.readValueTypeByName();
            byte flags = byteBuffer.readByte();
        }

        header.setDataStart(byteBuffer.getPosition());

        byteBuffer.expect(0, byteBuffer.readInt());
        int count = byteBuffer.readInt();

        Set<Object> values = new LinkedHashSet<>();
        for(int i = 0; i < count; i++) {
            values.add(readPropertyValue(valueType, byteBuffer));
        }

        if(header.getDataEnd() != byteBuffer.getPosition()) {
            log.error("Set read incorrectly, bytes left to read {}: {}", header.getDataEnd() - byteBuffer.getPosition(), values);
        }
        return new ArkProperty<>(header, new ArkSet(valueType, values));

    }

    private static String readSoftObjectPropertyValue(ArkBinaryData byteBuffer) {
        if (!byteBuffer.getSaveContext().hasNameTable()) {
            return "[" + String.join(", ", byteBuffer.readNames()) + "]";
        }

        List<String> result = new ArrayList<>();
        while (true) {
            String name = byteBuffer.readName();
            result.add(name);
            int next = byteBuffer.readInt();
            if (next == 0) {
                break;
            }
            byteBuffer.skipBytes(-4);
        }

        return "[" + String.join(", ", result) + "]";
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
            structArray.add(readStructPropertyValue(byteBuffer, dataSize, structType, true));
        }
        return structArray;
    }

    private static ObjectReference readObjectProperty(ArkBinaryData byteBuffer) {
        return new ObjectReference(byteBuffer);
    }

    private static Object readStructPropertyInArrayUE55(ArkBinaryData byteBuffer, String elementType) {
        ArkStructType arkStructType = ArkStructType.fromTypeName(elementType);
        if (arkStructType != null) {
            ArkSaveUtils.debugLog("Reading struct property of type {} in array", elementType);
            return arkStructType.getConstructor().apply(byteBuffer);
        }

        ArkSaveUtils.debugLog(String.format("Reading struct of type %s as regular struct", elementType));

        return readStructProperties(byteBuffer);
    }

    private static Object readStructPropertyValue(ArkBinaryData byteBuffer, int dataSize, String structType, boolean inArray) {
        if (!byteBuffer.currentParseContext().useUE55Structure() && !inArray) {
            byteBuffer.expect("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 ", byteBuffer.readBytesAsHex(16));
        }

        ArkStructType arkStructType = ArkStructType.fromTypeName(structType);

        if (arkStructType != null) {
            return arkStructType.getConstructor().apply(byteBuffer);
        } else {
            ArkSaveUtils.debugLog(String.format("Could not find struct for type %s", structType));
        }

        int position = byteBuffer.getPosition();

        try {
            ArkPropertyContainer properties = readStructProperties(byteBuffer);

            if (byteBuffer.getPosition() != position + dataSize && !inArray) {
                throw new Exception(String.format("Position %d before reading struct type %s of size %d, expecting end at %d, but was %d after reading struct", position, structType, dataSize, position + dataSize, byteBuffer.getPosition()));
            }
            return properties;
        } catch (Exception e) {
            log.error("Failed to read struct, debug data follows: {}", byteBuffer.currentParseContext());
            byteBuffer.setPosition(position);
            byte[] data = byteBuffer.readBytes(dataSize);
            byteBuffer.debugBinaryData(data);

            try {
                ArkSaveUtils.enableDebugLogging = true;
                byteBuffer.setPosition(position);
                ArkPropertyContainer properties = readStructProperties(byteBuffer);
            } finally {
                ArkSaveUtils.enableDebugLogging = false;
            }

            throw new IllegalStateException("Failed to read struct", e);
            // return new UnknownStruct(structType, byteBuffer.bytesToHex(data));
        }
    }

    private static Object readStructPropertyValue(ArkBinaryData byteBuffer, int dataSize, boolean inArray) {
        String structType = byteBuffer.readName();
        return readStructPropertyValue(byteBuffer, dataSize, structType, inArray);
    }

    private static ArkPropertyContainer readStructProperties(ArkBinaryData byteBuffer) {
        List<ArkProperty<?>> properties = new ArrayList<>();
        while (true) {
            ArkProperty<?> structProperty = readProperty(byteBuffer);
            if (structProperty == null) {
                break;
            }
            properties.add(structProperty);
        }
        return new ArkPropertyContainer(properties);
    }

    private static ArkProperty<?> readStructArrayPropertyUE55(ArkPropertyHeader header, ArkGenericType elementType, ArkBinaryData byteBuffer) {
        ArkGenericType genericType = elementType.getSubTypes().getFirst();

        List<Object> value = ensureBytesRead(header, byteBuffer, header.getDataSize(), () -> readStructArrayUE55(byteBuffer, genericType.getValue()));

        return new ArrayProperty<>(header, genericType.getValue(), value);
    }

    private static <T> T ensureBytesRead(ArkPropertyHeader propertyHeader, ArkBinaryData byteBuffer, int dataSize, Supplier<T> readFunction) {
        int startPosition = byteBuffer.getPosition();
        T value = readFunction.get();
        int bytesRead = byteBuffer.getPosition() - startPosition;

        if (bytesRead != dataSize) {
            log.error("Data read incorrectly while reading {}, expected {} bytes but read {} bytes:", propertyHeader, dataSize, bytesRead, new Throwable());
            byteBuffer.setPosition(startPosition);
            byteBuffer.debugBinaryData(byteBuffer.readBytes(dataSize));
            throw new IllegalStateException("Data read incorrectly while reading " + propertyHeader);
        }

        return value;
    }

    private static List<Object> readStructArrayUE55(ArkBinaryData byteBuffer, String elementType) {
        List<Object> structArray = new ArrayList<>();
        int count = byteBuffer.readInt();
        for (int i = 0; i < count; i++) {
            structArray.add(readStructPropertyInArrayUE55(byteBuffer, elementType));
        }
        return structArray;
    }

    private static ArkProperty<?> readArrayProperty(ArkPropertyHeader header, ArkBinaryData byteBuffer) {
        String elementType;

        if (byteBuffer.currentParseContext().useUE55Structure()) {
            ArkGenericType elementGenericType = header.getGenericType().getSubTypes().getFirst();
            elementType = elementGenericType.getValue();

            if (elementType.equals("StructProperty")) {
                return readStructArrayPropertyUE55(header, elementGenericType, byteBuffer);
            }
        } else {
            header.readLegacyDataSizeAndPosition(byteBuffer);
            elementType = byteBuffer.readName();
            byte flags = byteBuffer.readByte();
        }

        header.setDataStart(byteBuffer.getPosition());
        int arrayLength = byteBuffer.readInt();
        if (elementType.equals("StructProperty")) {
            try {
                List<Object> structArray = readStructArray(byteBuffer, elementType, arrayLength);
                int bytesLeft = header.getDataEnd() - byteBuffer.getPosition();
                if (bytesLeft != 0) {
                    log.error("Struct array read incorrectly, bytes left to read {}: {}", bytesLeft, structArray);
                    if (bytesLeft > 0) {
                        byteBuffer.debugBinaryData(byteBuffer.readBytes(bytesLeft));
                    }
                }
                return new ArrayProperty<>(header, elementType, structArray);
            } catch (Exception e) {
                log.error("Failed to read struct array", e);
                byteBuffer.setPosition(header.getDataStart());
                byte[] data = byteBuffer.readBytes(header.getDataSize());
                byteBuffer.debugBinaryData(data);
                return new ArrayProperty<>(header, elementType, byteBuffer.bytesToHex(data));
            }
        } else {
            List<Object> array = new ArrayList<>();
            ArkValueType valueType = ArkValueType.fromName(elementType);
            if (valueType == null) {
                throw new RuntimeException("Unknown array type " + elementType + " at position " + byteBuffer.getPosition());
            }

            //Read byte arrays as unsigned bytes...
            ArkValueType readValueType = valueType == ArkValueType.Byte ? ArkValueType.Int8 : valueType;

            try {
                for (int i = 0; i < arrayLength; i++) {
                    array.add(readPropertyValue(readValueType, byteBuffer));
                }
                if (header.getDataEnd() != byteBuffer.getPosition()) {
                    throw new RuntimeException(String.format("Array read incorrectly, bytes left: %d (reading as binary data instead)", header.getDataEnd() - byteBuffer.getPosition()));
                }
            } catch(Exception e) {
                byteBuffer.setPosition(header.getDataStart());
                String content = byteBuffer.bytesToHex(byteBuffer.readBytes(header.getDataSize()));
                log.error("Array {} of element type {} read incorrectly, returning blob data instead: {}", header, elementType, content, e);
                return new ArrayProperty<>(header, elementType, content);
            }
            return new ArrayProperty<>(header, elementType, array);
        }
    }

    @SuppressWarnings({"unchecked", "unused"})
    static <T> T readPropertyValue(ArkValueType valueType, ArkBinaryData byteBuffer, Class<T> returnType) {
        return (T)readPropertyValue(valueType, byteBuffer);
    }

    static Object readPropertyValue(ArkValueType valueType, ArkBinaryData byteBuffer) {
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
                if (byteBuffer.currentParseContext().useUE55Structure()) {
                    return byteBuffer.readByte() > 0;
                } else {
                    return byteBuffer.readShort() > 0;
                }
            case Struct:
                return readStructPropertyValue(byteBuffer, byteBuffer.readInt(), true);
            case SoftObject:
                return readSoftObjectPropertyValue(byteBuffer);
            default:
                throw new RuntimeException("Cannot read value type yet: " + valueType + " at position " + byteBuffer.getPosition());
        }
    }

}
