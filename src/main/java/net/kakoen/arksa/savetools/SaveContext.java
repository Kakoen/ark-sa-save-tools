package net.kakoen.arksa.savetools;

import lombok.Data;
import net.kakoen.arksa.savetools.struct.ActorTransform;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Data
public class SaveContext {

    private Map<Integer, String> names;
    private List<String> parts;
    private Map<UUID, ActorTransform> actorTransforms;
    private int saveVersion;
    private double gameTime;

    public Optional<ActorTransform> getActorTransform(UUID uuid) {
        return Optional.ofNullable(actorTransforms.get(uuid));
    }
}
