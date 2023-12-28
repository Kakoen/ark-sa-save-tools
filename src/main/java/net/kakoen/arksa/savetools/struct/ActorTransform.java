package net.kakoen.arksa.savetools.struct;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.ArkBinaryData;

@Data
@AllArgsConstructor
@Slf4j
public class ActorTransform {
    private double x;
    private double y;
    private double z;
    private double pitch;
    private double yaw;
    private double roll;

    public ActorTransform(ArkBinaryData reader) {
        x = reader.readDouble();
        y = reader.readDouble();
        z = reader.readDouble();
        pitch = reader.readDouble();
        yaw = reader.readDouble();
        roll = reader.readDouble();
        reader.skipBytes(8);
    }

    public ActorTransform(ArkVector vector, ArkRotator rotator) {
        this.x = vector.getX();
        this.y = vector.getY();
        this.z = vector.getZ();
        this.pitch = rotator.getPitch();
        this.yaw = rotator.getYaw();
        this.roll = rotator.getRoll();
    }
}
