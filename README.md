# Ark: Survival Ascended Save Tools

This library parses Ark: Survival Ascended world files (*.ark).

## Usage

As the code is in an incomplete and unstable state, usage is only recommended for experienced developers.
The implementation can change drastically at any moment.

See `ArkSaSaveDatabase` for a starting point.

Example usage:

```java
ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(new File("c:\\tmp\\TheIsland_WP.ark"));
Map<UUID, ArkGameObject> objects = arkSaSaveDatabase
        .getGameObjects((className, uuid) -> className.contains("Character_BP_C"));

ObjectWriter writer = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
        .writer(new DefaultPrettyPrinter());

writer.writeValue(new File("c:\\tmp\\TheIsland_WP.json"), objects);
```

## Help?

Join my Discord to find like-minded people. https://discord.gg/by2q2Tqjyr