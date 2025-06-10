package net.kakoen.arksa.savetools.property;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.ArkBinaryData;
import net.kakoen.arksa.savetools.ArkGenericType;

@Data
@Slf4j
public class ArkPropertyHeader {

    private String propertyName;
    private ArkGenericType genericType;
    private int dataStart;
    private int dataSize;
    private int position;

    public ArkValueType getArkValueType() {
        ArkValueType arkValueType = ArkValueType.fromGenericType(genericType);
        if (arkValueType == null) {
            log.error("ArkValueType is null for generic type: {}", genericType);
        }
        return arkValueType;
    }

    public static ArkPropertyHeader read(ArkBinaryData byteBuffer) {
        if (!byteBuffer.currentParseContext().useUE55Structure()) {
            return readLegacy(byteBuffer);
        }

        ArkPropertyHeader result = new ArkPropertyHeader();
        result.setPropertyName(byteBuffer.readName());

        if (result.getPropertyName() == null || result.getPropertyName().equals("None")) {
            return null;
        }

        result.setGenericType(byteBuffer.readArkGenericType());
        result.setDataSize(byteBuffer.readInt());

        if (result.getArkValueType() != ArkValueType.Boolean) {
            byte flags = byteBuffer.readByte();
            if ((flags & 0x01) == 1) {
                result.setPosition(byteBuffer.readInt());
            } else {
                result.setPosition(0);
            }
        }

        result.setDataStart(byteBuffer.getPosition());

        return result;
    }

    private static ArkPropertyHeader readLegacy(ArkBinaryData byteBuffer) {
        ArkPropertyHeader result = new ArkPropertyHeader();
        result.setPropertyName(byteBuffer.readName());

        if (result.getPropertyName() == null || result.getPropertyName().equals("None")) {
            return null;
        }

        result.setGenericType(ArkGenericType.fromValueType(byteBuffer.readValueTypeByName()));
        result.setDataStart(byteBuffer.getPosition());

        return result;
    }

    public void readLegacyDataSizeAndPosition(ArkBinaryData byteBuffer) {
        dataSize = byteBuffer.readInt();
        position = byteBuffer.readInt();
    }

    public int getDataEnd() {
        return dataStart + dataSize;
    }
}
