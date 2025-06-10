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

	public static String getHexDump(byte[] data, int bytesPerLine) {
		StringBuilder output = new StringBuilder();
		int offset = 0;

		while (offset < data.length) {
			// Offset/address
			output.append(String.format("%08X  ", offset));

			StringBuilder hexPart = new StringBuilder();
			StringBuilder asciiPart = new StringBuilder();

			for (int i = 0; i < bytesPerLine; i++) {
				if (offset + i < data.length) {
					byte b = data[offset + i];
					hexPart.append(String.format("%02X ", b));
					if (i % 8 == 7) hexPart.append(" ");
					asciiPart.append((b >= 32 && b <= 126) ? (char) b : '.');
				} else {
					hexPart.append("   ");
					if (i % 8 == 7) hexPart.append(" ");
					asciiPart.append(" ");
				}
			}

			output.append(hexPart);
			output.append(" |").append(asciiPart).append("|\n");

			offset += bytesPerLine;
		}

		return output.toString();
	}

}
