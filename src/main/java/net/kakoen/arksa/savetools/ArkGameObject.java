package net.kakoen.arksa.savetools;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.struct.ActorTransform;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ArkGameObject extends ArkPropertyContainer {

	private UUID uuid;
	private String blueprint;
	private List<String> names;
	private ActorTransform location;
	private String part;
	private byte unknown;

	public ArkGameObject(UUID uuid, String blueprint, ArkBinaryData byteBuffer) {
		this.uuid = uuid;
		this.location = byteBuffer.getSaveContext().getActorTransform(uuid).orElse(null);
		this.blueprint = blueprint;
		byteBuffer.expect(0, byteBuffer.readInt());
		this.names = byteBuffer.readNames(byteBuffer.readInt());
		this.part = byteBuffer.readPart();
		this.unknown = byteBuffer.readByte();

		readProperties(byteBuffer);
	}
}
