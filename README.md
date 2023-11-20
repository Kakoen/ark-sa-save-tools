# Ark: Survival Ascended Save Tools

This library parses Ark: Survival Ascended world files (*.ark).

## Usage

As the code is in an incomplete and unstable state, usage is only recommended for experienced developers.
The implementation can change drastically at any moment.

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

## Help?

My time is limited, so I will not provide extended support using this library. But you
can join the Discord I created for this project to find like-minded people.

See https://discord.gg/by2q2Tqjyr