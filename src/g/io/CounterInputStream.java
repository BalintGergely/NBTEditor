package g.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CounterInputStream extends FilterInputStream {
	public int counter;
	public CounterInputStream(InputStream in) {
		super(in);
	}
	@Override
	public int read() throws IOException{
		int i = super.read();
		if(i >= 0){
			counter++;
		}
		return i;
	}
	@Override
	public int read(byte[] b,int off,int len) throws IOException{
		int i = super.read(b, off, len);
		if(i > 0){
			counter += i;
		}
		return i;
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
	@SuppressWarnings("sync-override")
	public void mark(int rlimit){
		//
	}
}
