package net.kakoen.arksa.savetools.property;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
public class ArrayProperty<T> extends ArkProperty<T> {
	private final String arrayType;
	private int arrayLength;

	public ArrayProperty(String key, String type, int index, byte endOfStruct, String arrayType, int arrayLength, T data) {
		super(key, type, index, endOfStruct, data);
		this.arrayType = arrayType;
		this.arrayLength = arrayLength;
	}
}
