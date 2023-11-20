package net.kakoen.arksa.savetools;

import lombok.Data;
import net.kakoen.arksa.savetools.struct.ArkVector;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Data
public class SaveContext {

    private Map<Integer, String> names;
    private List<String> parts;
    private Map<UUID, ArkVector> actorLocations;

    public Optional<ArkVector> getActorLocation(UUID uuid) {
        return Optional.ofNullable(actorLocations.get(uuid));
    }
}
