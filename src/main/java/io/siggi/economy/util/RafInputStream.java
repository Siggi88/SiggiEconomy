package io.siggi.economy.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class RafInputStream extends InputStream {

	private final RandomAccessFile raf;

	public RafInputStream(RandomAccessFile raf) {
		this.raf = raf;
	}

	@Override
	public int read() throws IOException {
		return raf.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return raf.read(b);
	}

	@Override
	public int read(byte[] b, int offset, int length) throws IOException {
		return raf.read(b, offset, length);
	}

	@Override
	public int available() throws IOException {
		long a = raf.length() - raf.getFilePointer();
		if (a > (long) Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int) a;
	}

}
