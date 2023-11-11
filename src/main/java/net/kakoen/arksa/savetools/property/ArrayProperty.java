package net.kakoen.arksa.savetools.property;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.kakoen.arksa.savetools.property.ArkProperty;

@EqualsAndHashCode(callSuper = false)
@Data
public class ArrayProperty<T> extends ArkProperty<T> {
	private final String arrayType;
	private int arrayLength;
	private String rest;

	public ArrayProperty(String key, String type, int index, byte endOfStruct, String arrayType, int arrayLength, T data, String rest) {
		super(key, type, index, endOfStruct, data);
		this.arrayType = arrayType;
		this.rest = rest;
		this.arrayLength = arrayLength;
	}
}
