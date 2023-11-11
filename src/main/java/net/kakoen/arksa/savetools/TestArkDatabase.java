package net.kakoen.arksa.savetools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class TestArkDatabase {

	public static void main(String[] args) throws SQLException, IOException {
		ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(new File("c:\\tmp\\TheIsland_WP.ark"));
		Map<UUID, ArkGameObject> objects = arkSaSaveDatabase.getGameObjects(GameObjectReaderConfiguration.builder()
				.classNameFilter(name -> name.isPresent() && name.get().contains("Character_BP_C"))
				.binaryFilesOutputDirectory(Path.of("c:\\tmp\\out\\bin"))
				.jsonFilesOutputDirectory(Path.of("c:\\tmp\\out\\json"))
				.build());
		log.info("Found {} objects", objects.size());
	}
}
