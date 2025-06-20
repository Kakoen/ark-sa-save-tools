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

### Reading wild dinos with status

See [GetWildDinos.java](examples/dinos/GetWildDinos.java) for a starting point.

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

### Reading Ark Tribes and Players from save file (when server uses -usestore)

```java
try (ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(TestConstants.TEST_SAVED_ARKS_FILE.toFile())) {
    TribeAndPlayerData tribeAndPlayerData = arkSaSaveDatabase.getTribeAndPlayerData();
    
    List<ArkProfile> arkProfiles = tribeAndPlayerData.getPlayerIdentifiers().stream()
            .map(tribeAndPlayerData::getArkProfile)
            .toList();
    
    List<ArkTribe> arkTribes = tribeAndPlayerData.getTribeIdentifiers().stream()
            .map(tribeAndPlayerData::getArkTribe)
            .toList();
}    

```

### Reading Cryopod data

Reading Cryopod data is a bit more complex, but can be done. See `src/test/java/TestCryopods.java` for a starting point.

## Using the library in your Java project

From version 0.1.0, the library is published as a Maven package to Github packages. See https://github.com/Kakoen/ark-sa-save-tools/packages

## Help?

My time is limited, so I might not be able to provide extended support for using this library. But you
can join the Discord I created for this project to find like-minded people.

See https://discord.gg/by2q2Tqjyr

## Projects based on and/or inspired by this library

* [miragedmuk/ASV/ASASavegameToolkit](https://github.com/miragedmuk/ASV/tree/master/AsaSavegameToolkit) - C# port, used in Ark Save Visualizer.
* [VincentHenauGithub/ark-save-parser](https://github.com/VincentHenauGithub/ark-save-parser) - Python port.

## Donations

If you find this library useful and would like to support me, you can [donate here](https://www.paypal.com/donate/?business=RHMFDY3A7H3VU&no_recurring=0&item_name=Ark+Sa+Save+Tools). Thank you :)
