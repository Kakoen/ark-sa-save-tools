package net.kakoen.arksa.savetools.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ByteListInputStream extends InputStream {

	int index = 0;
	List<Byte> input;

	public ByteListInputStream(List<Byte> input) {
		this.input = input;
	}

	public ByteListInputStream(List<Byte> input, int initialIndex) {
		this.input = input;
		this.index = initialIndex;
	}

	@Override
	public int read() throws IOException {
		if (index >= input.size()) {
			return -1;
		}
		return input.get(index++) & 0xFF;
	}
}
