package net.kakoen.arksa.savetools;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class ArkGameObject {

	private UUID uuid;
	private String blueprint;
	private String className;
	private List<ArkProperty<?>> properties = new ArrayList<>();

	public ArkGameObject(UUID uuid, String blueprint, ArkBinaryData byteBuffer) {
		this.uuid = uuid;
		this.blueprint = blueprint;
		byteBuffer.skipBytes(12);
		this.className = byteBuffer.readName();

		byteBuffer.setPosition(29);

		int lastPropertyPosition = 29;
		try {
			ArkProperty<?> arkProperty = ArkProperty.readProperty(byteBuffer);
			while (byteBuffer.hasMore()) {
				properties.add(arkProperty);
				ArkSaveUtils.debugLog("Position: " + byteBuffer.byteBuffer.position());
				lastPropertyPosition = byteBuffer.byteBuffer.position();
				arkProperty = ArkProperty.readProperty(byteBuffer);
				if(arkProperty == null || arkProperty.getName().equals("None")) {
					return;
				}
				ArkSaveUtils.debugLog("Property {}", arkProperty);
			}
		} catch(Exception e) {
			ArkSaveUtils.debugLog("Could not parse {}", uuid, e);
			byteBuffer.setPosition(lastPropertyPosition);
			byteBuffer.debugBinaryData(byteBuffer.readBytes(byteBuffer.size() - byteBuffer.getPosition()));
			throw e;
		}
	}
}
