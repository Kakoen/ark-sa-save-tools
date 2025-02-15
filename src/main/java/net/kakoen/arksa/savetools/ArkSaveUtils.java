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

	public static String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02X ", b));
		}
		return sb.toString();
	}
}
