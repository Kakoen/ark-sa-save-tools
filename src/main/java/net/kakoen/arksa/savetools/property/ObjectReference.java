package net.kakoen.arksa.savetools.property;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.ArkBinaryData;

@Data
@Slf4j
public class ObjectReference {

    public final static int TYPE_ID = 0;
    public final static int TYPE_PATH = 1;
    public final static int TYPE_PATH_NO_TYPE = 2;
    public final static int TYPE_NAME = 3;
    public final static int TYPE_UUID = 4;
    public final static int TYPE_UNKNOWN = -1;

    private int type;
    private Object value;

    public ObjectReference(ArkBinaryData reader) {
        if (reader.getSaveContext().hasNameTable()) {
            boolean isName = reader.readShort() == 1;
            if (isName) {
                type = TYPE_PATH;
                value = reader.readName();
            } else {
                type = TYPE_UUID;
                value = reader.readUUIDAsString();
            }
            return;
        }

        int objectType = reader.readInt();
        if (objectType == -1) {
            type = TYPE_UNKNOWN;
            value = null;
        } else if (objectType == 0) {
            type = TYPE_ID;
            value = reader.readInt();
        } else if (objectType == 1) {
            type = TYPE_PATH;
            value = reader.readString();
        } else {
            reader.skipBytes(-4);
            type = TYPE_PATH_NO_TYPE;
            value = reader.readString();
        }
    }
}
