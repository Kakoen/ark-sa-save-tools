package net.kakoen.arksa.savetools.struct;

import lombok.Data;
import net.kakoen.arksa.savetools.ArkBinaryData;

@Data
public class ArkUniqueNetIdRepl {

    private byte unknown;
    private String valueType;
    private byte[] value;

    public ArkUniqueNetIdRepl(ArkBinaryData byteBuffer) {
        unknown = byteBuffer.readByte();
        valueType = byteBuffer.readString();
        byte length = byteBuffer.readByte();
        value = byteBuffer.readBytes(length);
    }

}
