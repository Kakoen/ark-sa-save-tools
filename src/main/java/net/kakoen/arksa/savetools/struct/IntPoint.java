package net.kakoen.arksa.savetools.struct;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.kakoen.arksa.savetools.ArkBinaryData;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntPoint {
    private int x;
    private int y;

    public IntPoint(ArkBinaryData arkBinaryData) {
        this.x = arkBinaryData.readInt();
        this.y = arkBinaryData.readInt();
    }
}
