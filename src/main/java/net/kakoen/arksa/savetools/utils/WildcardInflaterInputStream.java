package net.kakoen.arksa.savetools.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.LinkedList;

/**
 * Helper for reading cryopod data
 */
public class WildcardInflaterInputStream extends InputStream {

	private enum ReadState {
		None,
		Escape,
		Switch
	}

	private final ArrayDeque<Integer> fifoQueue = new ArrayDeque<>();
	private final InputStream is;
	private ReadState readState = ReadState.None;

	public WildcardInflaterInputStream(InputStream is) {
		this.is = is;
	}

	@Override
	public int read() throws IOException {
		if(!fifoQueue.isEmpty()) {
			return fifoQueue.pop();
		}

		int next = is.read();
		if(readState == ReadState.Switch) {
			int returnValue = 0xF0 | ((next & 0xF0) >> 4);
			fifoQueue.add(0xF0 | (next & 0x0F));
			readState = ReadState.None;
			return returnValue;
		}
		if(readState == ReadState.None) {
			if(next == 0xF0) {
				readState = ReadState.Escape;
				return read();
			}
			if(next == 0xF1) {
				readState = ReadState.Switch;
				return read();
			}
			if(next >= 0xF2) {
				int byteCount = next & 0x0F;
				for(int i = 0; i < byteCount; i++) {
					fifoQueue.add(0);
				}
				return read();
			}
		}

		readState = ReadState.None;
		return next;
	}

	@Override
	public void close() throws IOException {
		is.close();
	}
}
