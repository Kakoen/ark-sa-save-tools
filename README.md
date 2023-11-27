# Ark: Survival Ascended Save Tools

This library parses Ark: Survival Ascended world files (*.ark), tribe files (*.arktribe) and player files (*.arkprofile).

## Usage

As the code is in an incomplete and unstable state, usage is only recommended for experienced developers.
The implementation can change drastically at any moment.

### Reading *.ark save files

See `ArkSaSaveDatabase` for a starting point.

Example usage (reads all objects with 'Character_BP_C' in their blueprint path, and outputs both binary and json formatted files):

```java
try (ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(new File("c:\\tmp\\TheIsland_WP.ark"))) {
    GameObjectReaderConfiguration readerConfiguration = GameObjectReaderConfiguration.builder()
            .blueprintNameFilter(name -> name.isPresent() && name.get().contains("Character_BP_C"))
            .binaryFilesOutputDirectory(Path.of("c:\\tmp\\out\\bin"))
            .jsonFilesOutputDirectory(Path.of("c:\\tmp\\out\\json"))
            .build();

    Map<UUID, ArkGameObject> objects = arkSaSaveDatabase.getGameObjects(readerConfiguration);
    log.info("Found {} objects", objects.size());
} catch(Exception e) {
    log.error("Something bad happened!", e);
    throw new RuntimeException("Failed to read save file", e);
}
```

### Reading *.arktribe files

See ArkTribe as a starting point

```java
try {
    ArkTribe arkTribe = new ArkTribe(path);
    String tribeName = arkTribe.getTribe()
            .getPropertyValue("TribeData", ArkPropertyContainer.class)
            .flatMap(tribeData -> tribeData.getPropertyValue("TribeName", String.class))
            .orElse(null);

    log.info("Tribe name: {}", tribeName);
    JsonUtils.writeJsonToFile(arkTribe, Path.of(path.toString().replace(".arktribe", ".json")));
} catch (Exception e) {
    log.error("Could not read tribe " + path, e);
}
```

### Reading *.arkprofile files

See ArkProfile as a starting point

```java
try {
    ArkProfile arkProfile = new ArkProfile(path);
    String playerName = arkProfile.getProfile()
            .getPropertyValue("MyData", ArkPropertyContainer.class)
            .flatMap(myData -> myData.getPropertyValue("PlayerName", String.class))
            .orElse(null);

    log.info("Player name: {}", playerName);
    JsonUtils.writeJsonToFile(arkProfile, Path.of(path.toString().replace(".arkprofile", ".json")));
} catch (Exception e) {
    log.error("Could not read profile " + path, e);
}
```

## Help?

My time is limited, so I might not be able to provide extended support for using this library. But you
can join the Discord I created for this project to find like-minded people.

See https://discord.gg/by2q2Tqjyr