package io.siggi.economy.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class RafInputStream extends InputStream {

	private final RandomAccessFile raf;
	private int limit = -1;

	public RafInputStream(RandomAccessFile raf) {
		this.raf = raf;
	}

	@Override
	public int read() throws IOException {
		if (limit >= 0) {
			if (limit == 0) {
				return -1;
			}
			limit -= 1;
		}
		return raf.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		if (limit < 0) {
			return raf.read(b);
		}
		if (limit >= 0) {
			if (limit == 0) {
				return -1;
			}
		}
		int maxRead = Math.min(limit, b.length);
		int amountRead = raf.read(b, 0, limit);
		if (amountRead >= 0)
			limit -= amountRead;
		return amountRead;
	}

	@Override
	public int read(byte[] b, int offset, int length) throws IOException {
		if (limit >= 0) {
			if (limit == 0) {
				return -1;
			}
			limit -= 1;
		}
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

	public void setLimit(int limit) {
		if (limit < 0) limit = -1;
		this.limit = limit;
	}

	public void skipLine() throws IOException {
		while (true) {
			int i = read();
			if (i == -1 || i == 0x0A) break;
		}
	}

	private ByteArrayOutputStream baos = new ByteArrayOutputStream();

	public String readLine() throws IOException {
		baos.reset();
		while (true) {
			int i = read();
			if (i == -1 || i == 0x0A) {
				if (i == -1 && baos.size() == 0) return null;
				break;
			}
			baos.write(i);
		}
		return new String(baos.toByteArray(), StandardCharsets.UTF_8);
	}

}
