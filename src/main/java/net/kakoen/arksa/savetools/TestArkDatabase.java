package net.kakoen.arksa.savetools;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class TestArkDatabase {

	public static void main(String[] args) throws SQLException, IOException {
		ArkSaSaveDatabase arkSaSaveDatabase = new ArkSaSaveDatabase(new File("c:\\tmp\\TheIsland_WP.ark"));
		//Map<UUID, ArkGameObject> objects = arkSaSaveDatabase.getGameObjects((name, uuid) -> name.contains("Rex") && uuid.toString().contains("0327"));
		Map<UUID, ArkGameObject> objects = arkSaSaveDatabase.getGameObjects((name, uuid) -> name.contains("Character_BP_C"));
		// do not write null
		ObjectWriter writer = new ObjectMapper()
				.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
				.writer(new DefaultPrettyPrinter());

		writer.writeValue(new File("c:\\tmp\\TheIsland_WP.json"), objects);
		//log.info(writer.writeValueAsString(objects));
	}
}
