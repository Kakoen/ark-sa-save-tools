package net.kakoen.arksa.savetools;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.property.ArkValueType;
import net.kakoen.arksa.savetools.struct.ActorTransform;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static net.kakoen.arksa.savetools.ArkSaSaveDatabase.byteArrayToUUID;

@Slf4j
public class ArkBinaryData {

    ByteBuffer byteBuffer;

    @Getter
    private final SaveContext saveContext;

    private Deque<ParseContext> parseContext = new ArrayDeque<>();

    public ArkBinaryData(byte[] data) {
        this.byteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        this.saveContext = new SaveContext();
    }

    public ArkBinaryData(byte[] values, SaveContext saveContext) {
        this.byteBuffer = ByteBuffer.wrap(values).order(ByteOrder.LITTLE_ENDIAN);
        this.saveContext = saveContext;
    }

    public String readString() {
        int length = readInt();
        if (length == 0) {
            return null;
        }

        boolean isMultiByte = length < 0;

        if (isMultiByte) {
            String result = new String(readChars(Math.abs(length)), 0, Math.abs(length) - 1);
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

    public int remaining() {
        return byteBuffer.remaining();
    }

    public byte[] readBytes(int count) {
        byte[] bytes = new byte[count];
        if (count > byteBuffer.remaining()) {
            log.error("Trying to read more bytes than remaining! {} > {}", count, byteBuffer.remaining());
            throw new IllegalStateException("Trying to read more bytes than remaining!");
        }
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
        for (byte b : bytes) {
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

    public List<String> findNames() {
        if (!saveContext.hasNameTable()) {
            return List.of();
        }
        log.info("--- Looking for names ---");
        List<String> found = new ArrayList<>();
        for (int i = 0; i < size() - 4; i++) {
            setPosition(i);
            int intValue = readInt();
            String n = saveContext.getNames().get(intValue);
            if (n == null && saveContext.hasConstantNameTable() && intValue != 0) {
                n = saveContext.getConstantNameTable().get(intValue);
            }
            if (n != null) {
                found.add(n);
                setPosition(i);
                log.info("Found name: {} ({}) at 0x{} (hex)", n, readBytesAsHex(4), Integer.toHexString(i));
                i += 3;
            }
        }
        return found;
    }

    public double readDouble() {
        return byteBuffer.getDouble();
    }

    public short readUnsignedByte() {
        return (short) (readByte() & 0xFF);
    }

    public byte readByte() {
        return byteBuffer.get();
    }

    public String readName() {
        if (!saveContext.hasNameTable()) {
            return readString();
        }
        String name = saveContext.getName(readInt());
        int identifier = readInt();

        if (name != null && identifier != 0) {
            return name + "_" + identifier;
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
        log.error("Data that was not recognized ({} bytes): \n{}", data.length, ArkSaveUtils.getHexDump(data, 32));
        new ArkBinaryData(data, saveContext).findNames();
    }

    public BigInteger readUInt64() {
        return new BigInteger(Long.toUnsignedString(byteBuffer.getLong()));
    }

    public long readLong() {
        return byteBuffer.getLong();
    }

    public Map<UUID, ActorTransform> readActorTransforms() {
        Map<UUID, ActorTransform> actorTransforms = new HashMap<>();
        UUID terminationUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID uuid = readUUID();
        while (!uuid.equals(terminationUUID)) {
            actorTransforms.put(uuid, new ActorTransform(this));
            uuid = readUUID();
        }
        return actorTransforms;
    }

    public void expect(Object expected, Object read) {
        if (!Objects.equals(expected, read)) {
            log.warn("Unexpected data, expected {}, but was {} at {}", expected, read, getPosition(), new Throwable());
        }
    }

    public List<String> readStringsArray() {
        List<String> result = new ArrayList<>();
        long count = readUInt32();
        for (int i = 0; i < count; i++) {
            result.add(readString());
        }
        return result;
    }

    public List<String> readNames(int nameCount) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < nameCount; i++) {
            result.add(saveContext.isReadNamesAsStrings() ? readString() : readName());
        }
        return result;
    }

    public ArkValueType readValueTypeByName() {
        int position = getPosition();
        String keyTypeName = readName();

        ArkValueType keyType = ArkValueType.fromName(keyTypeName);
        if(keyType == null) throw new IllegalStateException("Unknown value type " + keyTypeName + " at position " + position);
        return keyType;
    }

    public String readPart() {
        int partIndex = byteBuffer.getInt();
        if(partIndex >= 0 && partIndex < saveContext.getParts().size()) {
            return saveContext.getParts().get(partIndex);
        }
        return null;
    }

    public int peekInt() {
        int position = getPosition();
        int value = readInt();
        setPosition(position);
        return value;
    }

    public List<UUID> readUuids() {
        int uuidCount = readInt();
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < uuidCount; i++) {
            uuids.add(readUUID());
        }
        return uuids;
    }

    public List<String> readNames() {
        //Keep reading names until null
        List<String> names = new ArrayList<>();
        String name = readName();
        while (name != null) {
            names.add(name);
            name = readName();
        }
        return names;
    }

    public byte[] peekBytes(int i) {
        int position = getPosition();
        byte[] bytes = readBytes(i);
        setPosition(position);
        return bytes;
    }

    public void pushParseContext(ParseContext parseContext) {
        this.parseContext.push(parseContext);
    }

    public ParseContext currentParseContext() {
        return parseContext.peek();
    }

    public void popParseContext() {
        this.parseContext.pop();
    }

    public ArkGenericType readArkGenericType() {
        return readArkGenericTypes(1).getFirst();
    }

    public List<ArkGenericType> readArkGenericTypes(int count) {
        List<ArkGenericType> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            ArkGenericType type = new ArkGenericType();
            type.setValue(readName());
            int subTypes = readInt();
            if(subTypes > 0) type.setSubTypes(readArkGenericTypes(subTypes));
            result.add(type);
        }

        return result;
    }

    public List<String> readTypes() {
        int readAnother = readInt();
        List<String> result = new ArrayList<>();
        while(readAnother > 0) {
            result.add(readName());
            readAnother = readInt();
        }
        return result;
    }

    public void initializeParseContext(SaveContext saveContext) {
        parseContext.push(new ParseContext(ArchiveType.SAVE, saveContext.getSaveVersion()));
    }
}
