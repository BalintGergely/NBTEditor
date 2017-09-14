package g.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends FilterInputStream{
	long counter;
	public LimitedInputStream(InputStream in1,long count) {
		super(in1);
		counter = count;
	}
	@Override
	public synchronized int read() throws IOException{
		if(counter == 0){
			return -1;
		}
		int i = super.read();
		if(i >= 0){
			counter--;
		}
		return i;
	}
	@Override
	public synchronized int read(byte[] bytes,int off,int len) throws IOException{
		if(off < 0 || len < 0 || off+len > bytes.length){
			throw new IllegalArgumentException();
		}
		if(counter == 0){
			return -1;
		}
		if(counter < len){
			len = (int)counter;
		}
		if(len > 0){
			len = super.read(bytes, off, len);
			if(len == -1){
				return -1;
			}
		}
		counter -= len;
		return len;
	}
	@Override
	public boolean markSupported(){
		return false;
	}
	@Override
	@SuppressWarnings("sync-override")
	public void reset() throws IOException{
		throw new IOException();
	}
	@Override
	public synchronized void mark(int readLimit){
		//
	}
}
