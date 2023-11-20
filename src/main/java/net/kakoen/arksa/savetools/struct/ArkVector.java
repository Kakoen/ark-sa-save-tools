package net.kakoen.arksa.savetools.struct;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.kakoen.arksa.savetools.ArkBinaryData;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArkVector {

	private double x;
	private double y;
	private double z;

	public ArkVector(ArkBinaryData byteBuffer) {
		x = byteBuffer.readDouble();
		y = byteBuffer.readDouble();
		z = byteBuffer.readDouble();
	}
}
