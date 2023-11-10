package net.kakoen.arksa.savetools.struct;

import lombok.Data;
import net.kakoen.arksa.savetools.ArkBinaryData;

@Data
public class ArkQuat {
	private double x;
	private double y;
	private double z;
	private double w;

	public ArkQuat(ArkBinaryData byteBuffer) {
		x = byteBuffer.readDouble();
		y = byteBuffer.readDouble();
		z = byteBuffer.readDouble();
		w = byteBuffer.readDouble();
	}
}
