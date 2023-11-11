package net.kakoen.arksa.savetools.struct;

import lombok.Data;
import net.kakoen.arksa.savetools.ArkBinaryData;

@Data
public class ArkColor {
    private byte r;
    private byte g;
    private byte b;
    private byte a;

    public ArkColor(ArkBinaryData arkBinaryData) {
        r = arkBinaryData.readByte();
        g = arkBinaryData.readByte();
        b = arkBinaryData.readByte();
        a = arkBinaryData.readByte();
    }

}
