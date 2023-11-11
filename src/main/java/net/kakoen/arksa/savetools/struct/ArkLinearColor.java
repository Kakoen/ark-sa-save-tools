package net.kakoen.arksa.savetools.struct;

import lombok.Data;
import net.kakoen.arksa.savetools.ArkBinaryData;

@Data
public class ArkLinearColor {
    private float r;
    private float g;
    private float b;
    private float a;

    public ArkLinearColor(ArkBinaryData byteBuffer) {
        r = byteBuffer.readFloat();
        g = byteBuffer.readFloat();
        b = byteBuffer.readFloat();
        a = byteBuffer.readFloat();
    }
}
