package net.kakoen.arksa.savetools;

import lombok.Data;
import net.kakoen.arksa.savetools.property.ArkProperty;

import java.util.*;

@Data
public class ArkPropertyContainer {

    private List<ArkProperty<?>> properties = new ArrayList<>();

    public ArkPropertyContainer() {
    }

    public ArkPropertyContainer(List<ArkProperty<?>> properties) {
        this.properties = properties;
    }

    public void readProperties(ArkBinaryData byteBuffer) {
        int lastPropertyPosition = byteBuffer.getPosition();
        try {
            ArkProperty<?> arkProperty = ArkProperty.readProperty(byteBuffer);
            while (byteBuffer.hasMore()) {
                if(arkProperty != null) getProperties().add(arkProperty);
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

    public boolean hasProperty(String name) {
        return properties.stream().anyMatch(property -> property.getName().equals(name));
    }

    public <T> Optional<ArkProperty<T>> findProperty(String name) {
        return properties
                .stream()
                .filter(property -> property.getName().equals(name))
                .map(property -> (ArkProperty<T>) property).findFirst();
    }

    public <T> Optional<ArkProperty<T>> findProperty(String name, int position) {
        return properties
                .stream()
                .filter(property -> property.getName().equals(name))
                .filter(property -> property.getPosition() == position)
                .map(property -> (ArkProperty<T>) property)
                .findFirst();
    }

    public <T> Optional<T> getPropertyValue(String name, Class<T> clazz) {
        return (Optional<T>)findProperty(name).map(ArkProperty::getValue);
    }

    public <T> Optional<T> getPropertyValue(String name, int position, Class<T> clazz) {
        return (Optional<T>)findProperty(name, position).map(ArkProperty::getValue);
    }

    public <T> List<ArkProperty<T>> getProperties(String name, Class<T> clazz) {
        return properties
                .stream()
                .filter(property -> property.getName().equals(name))
                .map(property -> (ArkProperty<T>) property)
                .toList();
    }

    public <T> Map<Integer, T> getPropertiesByPosition(String name, Class<T> clazz) {
        return getProperties(name, clazz)
                .stream().collect(HashMap::new, (m, p) -> m.put(p.getPosition(), p.getValue()), HashMap::putAll);
    }

}
