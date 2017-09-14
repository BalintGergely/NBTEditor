package g.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class LongHeapSequence{
	private long[] content;
	private long size;
	public LongHeapSequence(){
		content = new long[0];
	}
	public synchronized void ensureCapacity(long capacity){
		if(capacity > content.length*8){
			long n = (capacity+7)/8;
			if(n > Integer.MAX_VALUE){
				throw new OutOfMemoryError();
			}
			content = Arrays.copyOf(content, (int)n);
		}
	}
	public synchronized void trimToCapacity(){
		if(content.length*8 > size+7){
			content = Arrays.copyOf(content, (int)((size+7)/8));
		}
	}
	public long size(){
		return size;
	}
	public int read(long position1){
		if(position1 < 0){
			throw new IllegalArgumentException();
		}
		if(position1 >= size){
			return -1;
		}
		return read0(position1);
	}
	private static int offset(long position){
		return (int)(7-position%8);
	}
	private int read0(long position1){
		return (int) ((content[(int) (position1/8)] >> offset(position1)*8) & 0xff);
	}
	private void write0(int value,long position1){
		value &= 0xff;
		int pos = (int) (position1/8),off = offset(position1)*8;
		long val = content[pos];
		val &= ~(0xffL << off);
		val |= ((long)value) << off;
		content[pos] = val;
	}
	public synchronized void write(int value, long position1){
		if(position1 < 0){
			throw new IllegalArgumentException();
		}
		ensureCapacity(position1+1);
		write0(value,position1);
		if(size <= position1){
			size = position1+1;
		}
	}
	public int read(ByteBuffer dst, long position1){
		int pos = dst.position();
		ByteBuffer buf = dst.slice();
		while(buf.hasRemaining() && position1%8 != 0 && position1 < size){
			buf.put((byte)read0(position1++));
		}
		while(buf.remaining() > 7 && position1+7 < size){
			buf.putLong(content[(int)(position1/8)]);
			position1 += 8;
		}
		while(buf.hasRemaining() && position1 < size){
			buf.put((byte)read0(position1++));
		}
		try{
			dst.position(pos+buf.position());
		}catch(IllegalArgumentException e){
			//
		}
		return buf.position();
	}
	public synchronized int write(ByteBuffer src, long position1){
		int pos = src.position();
		ByteBuffer buf = src.slice();
		long nSize = buf.remaining()+position1;
		ensureCapacity(nSize);
		if(size < nSize){
			size = nSize;
		}
		while(buf.hasRemaining() && position1%8 != 0){
			write0(Byte.toUnsignedInt(buf.get()),position1++);
		}
		while(buf.remaining() > 7){
			content[(int)(position1/8)] = buf.getLong();
			position1 += 8;
		}
		while(buf.hasRemaining()){
			write0(Byte.toUnsignedInt(buf.get()),position1++);
		}
		try{
			src.position(pos+buf.position());
		}catch(IllegalArgumentException e){
			//
		}
		return buf.position();
	}
	public synchronized LongHeapSequence truncate(long size1){
		if(size > size1){
			size = size1;
		}
		return this;
	}
	public void saveTo(OutputStream out) throws IOException{
		ByteBuffer buf = ByteBuffer.allocate(1024);
		int i;
		long pos = 0;
		while((i = read(buf,pos)) > 0){
			out.write(buf.array(),0,i);
			pos += i;
			buf.clear();
		}
	}
	public void loadFrom(InputStream in) throws IOException{
		size = 0;
		ByteBuffer buf = ByteBuffer.allocate(1024);
		int i;
		while((i = in.read(buf.array())) > 0){
			buf.limit(i);
			write(buf,size);
		}
	}
}