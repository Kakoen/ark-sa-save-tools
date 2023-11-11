package net.kakoen.arksa.savetools;

import lombok.Data;
import net.kakoen.arksa.savetools.property.ArkProperty;

import java.util.ArrayList;
import java.util.List;

@Data
public class ArkPropertyContainer {

    private List<ArkProperty<?>> properties = new ArrayList<>();

    public ArkPropertyContainer() {
    }

    public void readProperties(ArkBinaryData byteBuffer) {
        int lastPropertyPosition = byteBuffer.getPosition();
        try {
            ArkProperty<?> arkProperty = ArkProperty.readProperty(byteBuffer);
            while (byteBuffer.hasMore()) {
                getProperties().add(arkProperty);
                ArkSaveUtils.debugLog("Position: " + byteBuffer.byteBuffer.position());
                lastPropertyPosition = byteBuffer.byteBuffer.position();
                arkProperty = ArkProperty.readProperty(byteBuffer);
                if(arkProperty == null || arkProperty.getName().equals("None")) {
                    return;
                }
                ArkSaveUtils.debugLog("Property {}", arkProperty);
            }
        } catch(Exception e) {
            ArkSaveUtils.debugLog("Could not read properties", e);
            byteBuffer.setPosition(lastPropertyPosition);
            byteBuffer.debugBinaryData(byteBuffer.readBytes(byteBuffer.size() - byteBuffer.getPosition()));
            throw e;
        }

    }

}
