package net.kakoen.arksa.savetools;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArkSaveUtils {

	public static boolean enableDebugLogging = false;

	public static void debugLog(String message, Object... args) {
		if(enableDebugLogging) {
			log.info(message, args);
		}
	}

}
