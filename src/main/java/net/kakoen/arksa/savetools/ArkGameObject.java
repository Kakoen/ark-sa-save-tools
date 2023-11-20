package net.kakoen.arksa.savetools;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.struct.ArkVector;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ArkGameObject extends ArkPropertyContainer {

	private UUID uuid;
	private String blueprint;
	private String name;
	private String className;
	private ArkVector location;
	private boolean item;

	public ArkGameObject(UUID uuid, String blueprint, ArkBinaryData byteBuffer) {
		this.uuid = uuid;
		this.location = byteBuffer.getSaveContext().getActorLocation(uuid).orElse(null);
		this.blueprint = blueprint;

		byteBuffer.skipBytes(8);
		this.name = byteBuffer.readSingleName();
		this.item = byteBuffer.readBoolean();
		this.className = byteBuffer.readSingleName();
		byteBuffer.skipBytes(1);

		readProperties(byteBuffer);
	}
}
