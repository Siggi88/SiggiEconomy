package io.siggi.economy.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class IO {

	public static int readSingleByte(InputStream in) throws IOException {
		int read = in.read();
		if (read < 0) throw new EOFException();
		return read;
	}

	public static long readLong(InputStream in) throws IOException {
		return (((long) readSingleByte(in)) << 56L)
				| (((long) readSingleByte(in)) << 48L)
				| (((long) readSingleByte(in)) << 40L)
				| (((long) readSingleByte(in)) << 32L)
				| (((long) readSingleByte(in)) << 24L)
				| (((long) readSingleByte(in)) << 16L)
				| (((long) readSingleByte(in)) << 8L)
				| (((long) readSingleByte(in)));
	}
	public static void writeLong(OutputStream out, long value) throws IOException {
		out.write((int) ((value >> 56L) & 0xffL));
		out.write((int) ((value >> 48L) & 0xffL));
		out.write((int) ((value >> 40L) & 0xffL));
		out.write((int) ((value >> 32L) & 0xffL));
		out.write((int) ((value >> 24L) & 0xffL));
		out.write((int) ((value >> 16L) & 0xffL));
		out.write((int) ((value >> 8L) & 0xffL));
		out.write((int) ((value) & 0xffL));
	}

	public static byte[] readBytes(InputStream in, int count) throws IOException {
		byte[] data = new byte[count];
		int read = 0;
		int c;
		while (read < count) {
			c = in.read(data, read, count - read);
			if (c == -1) {
				throw new EOFException();
			}
			read += c;
		}
		return data;
	}

	public static String readString(InputStream in) throws IOException {
		int length = in.read();
		if (length >= 128) {
			length &= 0x7f;
			length = length << 8;
			length |= in.read();
		}
		return new String(readBytes(in, length), StandardCharsets.UTF_8);
	}

	public static void writeString(OutputStream out, String string) throws IOException {
		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		if (bytes.length >= 128) {
			if (bytes.length >= 32768) {
				throw new IllegalArgumentException("String too long!");
			}
			out.write(0x80 | ((bytes.length >> 8) & 0xff));
			out.write((bytes.length) & 0xff);
		}
		out.write(bytes);
	}
}
