package net.kakoen.arksa.savetools.struct;

import lombok.Data;
import net.kakoen.arksa.savetools.ArkBinaryData;

@Data
public class ArkRotator {
    double pitch;
    double yaw;
    double roll;

    public ArkRotator(ArkBinaryData binaryData) {
        this.pitch = binaryData.readDouble();
        this.yaw = binaryData.readDouble();
        this.roll = binaryData.readDouble();
    }

}
