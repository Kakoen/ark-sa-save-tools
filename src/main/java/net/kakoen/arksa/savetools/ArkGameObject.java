package net.kakoen.arksa.savetools;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kakoen.arksa.savetools.struct.ActorTransform;
import net.kakoen.arksa.savetools.struct.ArkRotator;
import net.kakoen.arksa.savetools.struct.ArkVector;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@NoArgsConstructor
public class ArkGameObject extends ArkPropertyContainer {

	private UUID uuid;
	private String blueprint;
	private List<String> names;
	private ActorTransform location;
	private String part;
	private byte unknown;
	private UUID uuid2;

	@JsonIgnore
	private int propertiesOffset;

	public ArkGameObject(UUID uuid, String blueprint, ArkBinaryData binaryReader) {
		this.uuid = uuid;
		this.location = binaryReader.getSaveContext().getActorTransform(uuid).orElse(null);
		this.blueprint = blueprint;
		binaryReader.expect(0, binaryReader.readInt());
		this.names = binaryReader.readNames(binaryReader.readInt());
		this.part = binaryReader.readPart();
		this.unknown = binaryReader.readByte();

		readProperties(binaryReader);
	}

	public static ArkGameObject readFromCustomBytes(ArkBinaryData reader) {
		ArkGameObject arkGameObject = new ArkGameObject();
		arkGameObject.setUuid(reader.readUUID());
		arkGameObject.setBlueprint(reader.readString());
		reader.expect(0, reader.readInt());
		arkGameObject.setNames(reader.readStringsArray());
		boolean fromDataFile = reader.readBoolean(); // ???
		int dataFileIndex = reader.readInt(); // ???
		if(reader.readBoolean()) {
			ArkRotator rotator = new ArkRotator(reader); // ???
			arkGameObject.setLocation(new ActorTransform(new ArkVector(0,0,0), rotator));
		}
		arkGameObject.setPropertiesOffset(reader.readInt());
		reader.expect(0, reader.readInt());

		return arkGameObject;
	}

	public void readExtraData(ArkBinaryData reader) {
		if(reader.hasMore() && reader.readBoolean()) { //Boolean? Or count?
			uuid2 = reader.readUUID();
		}
	}
}
