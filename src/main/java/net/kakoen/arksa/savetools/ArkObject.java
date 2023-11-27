package net.kakoen.arksa.savetools;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.kakoen.arksa.savetools.struct.ArkRotator;
import net.kakoen.arksa.savetools.struct.ArkVector;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class ArkObject extends ArkPropertyContainer {

    private UUID uuid;
    private String className;
    private boolean item;
    private List<String> names;
    private boolean fromDataFile;
    private int dataFileIndex;
    private ArkVector vector;
    private ArkRotator rotator;
    private int propertiesOffset;

    public ArkObject(ArkBinaryData reader) {
        uuid = reader.readUUID();
        className = reader.readString();
        item = reader.readBoolean();
        names = reader.readStringsArray();
        fromDataFile = reader.readBoolean();
        dataFileIndex = reader.readInt();
        if(reader.readBoolean()) {
            vector = new ArkVector(reader);
            rotator = new ArkRotator(reader);
        }
        propertiesOffset = reader.readInt();
        reader.expect(0, reader.readInt());
    }
}
