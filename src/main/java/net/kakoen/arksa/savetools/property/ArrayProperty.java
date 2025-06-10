package net.kakoen.arksa.savetools.property;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
@Data
public class ArrayProperty<T> extends ArkProperty<T> {
	private final String arrayType;

	public ArrayProperty(String key, String type, int index, String arrayType, T data) {
		super(key, type, index, data);
		this.arrayType = arrayType;
	}

	public ArrayProperty(ArkPropertyHeader header, String arrayType, T data) {
		super(header, data);
		this.arrayType = arrayType;
	}
}
