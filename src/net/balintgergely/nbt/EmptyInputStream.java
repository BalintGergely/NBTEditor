package net.balintgergely.nbt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class EmptyInputStream extends InputStream{
	public static final EmptyInputStream INSTANCE = new EmptyInputStream();
	@Override
	public int read() throws IOException {
		return -1;
	}
	@Override
	public int read(byte[] b) throws IOException {
		return -1;
	}
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return -1;
	}
	@Override
	public byte[] readAllBytes() throws IOException {
		return new byte[0];
	}
	@Override
	public int readNBytes(byte[] b, int off, int len) throws IOException {
		return 0;
	}
	@Override
	public long skip(long n) throws IOException {
		return 0;
	}
	@Override
	public int available() throws IOException {
		return 0;
	}
	@Override
	public void close() throws IOException {}

	@Override
	public synchronized void mark(int readlimit) {}
	@Override
	public synchronized void reset() throws IOException {}
	@Override
	public boolean markSupported() {
		return true;
	}
	@Override
	public long transferTo(OutputStream out) throws IOException {
		return 0;
	}
}
