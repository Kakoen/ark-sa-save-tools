package net.kakoen.arksa.savetools;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.property.ArkProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
@Slf4j
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
            while (byteBuffer.hasMore()) {
                ArkSaveUtils.debugLog("Position: " + byteBuffer.getPosition());
                ArkProperty<?> arkProperty = ArkProperty.readProperty(byteBuffer);
                if(arkProperty == null) {
                    break;
                }
                ArkSaveUtils.debugLog("Property {}", arkProperty);
                properties.add(arkProperty);
                lastPropertyPosition = byteBuffer.byteBuffer.position();
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

    public <T> Optional<List<T>> getArrayPropertyValue(String name, Class<T> clazz) {
        return Optional.ofNullable((List<T>)getPropertyValue(name, List.class).orElse(null));
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
