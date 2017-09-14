package g.io;

import java.io.IOException;
import java.io.OutputStream;

import g.util.Link;
/**
 * A byte output stream that writes bytes to 0x1000 byte arrays that are stored as links.
 * @author Gergely Bálint
 *
 */
public class ByteArrayOutputStream extends OutputStream{
	public final Link<byte[]> bytes;
	private Link<byte[]> current;
	private int size = 0;
	public ByteArrayOutputStream() {
		bytes = current = new Link<>(new byte[0x1000],null);
	}
	public ByteArrayOutputStream(int off) {
		if(off < 0 || off > 0x1000){
			throw new IllegalArgumentException();
		}
		bytes = current = new Link<>(new byte[0x1000],null);
		size = off;
	}
	@Override
	public void write(int b) throws IOException{
		if(size == Integer.MAX_VALUE){
			throw new IOException();
		}
		if(size % 0x1000 == 0 && size != 0){
			current = current.next = new Link<>(new byte[0x1000],null);
		}
		current.element[size++] = (byte)b;
	}
	@Override
	public void write(byte[] b,int off,int len) throws IOException{
		if(off < 0 || len < 0 || b.length < off+len){
			throw new IllegalArgumentException();
		}
		if(len + size < 0){
			throw new IOException();
		}
		len += off;
		while(off != len){
			if(size % 0x1000 == 0 && size != 0){
				current = current.next = new Link<>(new byte[0x1000],null);
			}
			int write = 0x1000 - size % 0x1000;
			if(write > len-off){
				write = len-off;
			}
			System.arraycopy(b, off, current.element, size % 0x1000, write);
			size += write;
			off += write;
		}
	}
	public int getSize(){
		return size;
	}
}
