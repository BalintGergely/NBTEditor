package net.balintgergely.nbt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class LongHeapSequence{
	private long[] content;
	private long position,size;
	public long size(){
		return size;
	}
	public LongHeapSequence(){
		content = new long[0];
	}
	public synchronized void ensureCapacity(long capacity){
		capacity = (capacity+7)/8;
		if(capacity > content.length){
			if(capacity > Integer.MAX_VALUE){
				throw new IllegalArgumentException();
			}
			long[] ar = new long[(int)capacity];
			System.arraycopy(content, 0, ar, 0, (int)((size+7)/8));
			content = ar;
		}
	}
	public synchronized void trimToCapacity(){
		int cap = (int) ((size+7)/8);
		if(content.length > cap){
			content = Arrays.copyOf(content, cap);
		}
	}
	public synchronized int read(ByteBuffer dst){
		int res = read(dst,position);
		if(res > 0){
			position += res;
		}
		return res;
	}
	public synchronized int write(ByteBuffer src){
		int res = write(src,position);
		if(res > 0){
			position += res;
		}
		return res;
	}
	public long position(){
		return position;
	}
	public boolean isOpen(){
		return true;
	}
	public void close(){
		//
	}
	public synchronized int read(){
		int res = read(position);
		if(res >= 0){
			position++;
		}
		return res;
	}
	public synchronized void write(int value){
		write(value,position);
		position++;
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
	private int read0(long position1){
		return getByte(content[(int)(position1/8)], (int)(position1%8));
	}
	private void write0(int value,long position1){
		int index = (int)(position1/8);
		content[index] = putByte(content[index], (byte)(position1%8), value);
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
		int len = read0(dst.slice(), position1);
		try{
			dst.position(dst.position()+len);
		}catch(IllegalArgumentException e){
			//Concurrent modification
		}
		return len;
	}
	private synchronized int read0(ByteBuffer buf, long position1){
		long len = size;
		while(buf.hasRemaining() && position1%8 != 0 && position1 < len){
			buf.put((byte)read0(position1++));
		}
		buf.order(ByteOrder.BIG_ENDIAN);
		while(buf.remaining() > 7 && position1+7 < len){
			buf.putLong(content[(int)(position1/8)]);
			position1 += 8;
		}
		while(buf.hasRemaining() && position1 < len){
			buf.put((byte)read0(position1++));
		}
		return buf.position();
	}
	public int write(ByteBuffer src, long position1){
		int len = write0(src.slice(),position1);
		try{
			src.position(src.position()+len);
		}catch(IllegalArgumentException e){
			//
		}
		return len;
	}
	private synchronized int write0(ByteBuffer buf, long position1){
		long nSize = buf.remaining()+position1+1;
		ensureCapacity(nSize);
		if(size < nSize){
			size = nSize;
		}
		buf.order(ByteOrder.BIG_ENDIAN);
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
		return buf.position();
	}
	public void force(boolean metaData){
		//
	}
	public void discard(){
		//
	}
	public void tempRegion(long position1, long length){
		//
	}
	public synchronized LongHeapSequence truncate(long size1){
		if(size > size1){
			size = size1;
			if(position > size1){
				position = size1;
			}
		}
		return this;
	}
	public synchronized LongHeapSequence position(long position1){
		if(position1 < 0){
			throw new IllegalArgumentException();
		}
		position = position1;
		return this;
	}
	public synchronized void export(File file) throws IOException{
		file.createNewFile();
		try(OutputStream out = new FileOutputStream(file)){
			ByteBuffer buf = ByteBuffer.allocate(8192);
			long pos = 0;
			int i;
			while((i = read0(buf,pos)) > 0){
				out.write(buf.array(), 0, i);
				pos += i;
				buf.clear();
			}
		}
	}
	public synchronized void inport(File file) throws IOException{
		try(FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.READ)){
			long l = fc.size(),pos = 0;
			ensureCapacity(l);
			ByteBuffer buf = ByteBuffer.allocate(8192);
			while(fc.read(buf) > 0){
				buf.flip();
				pos += write0(buf,pos);
				buf.clear();
			}
			truncate(pos);
		}
	}
	
	/*
	 * ==============================BIG_ENDIAN
	 * 
	 * [a+x] = val >> (7-x)*8
	 * val = [a+x] << (7-x)*8
	 * 
	 *        00 00 00 00 00 00 00 00
	 *        |  |  |  |  |  |  |  |
	 * [a  ]<-+  |  |  |  |  |  |  |
	 * [a+1]<----+  |  |  |  |  |  |
	 * [a+2]<-------+  |  |  |  |  |
	 * [a+3]<----------+  |  |  |  |
	 * [a+4]<-------------+  |  |  |
	 * [a+5]<----------------+  |  |
	 * [a+6]<-------------------+  |
	 * [a+7]<----------------------+
	 * 
	 * ==============================LITTLE_ENDIAN
	 * 
	 * [a+x] = val >> x*8
	 * val = [a+x] >> x*8
	 * 
	 * return content[a/8] >> (a%8)*8;
	 * content[a/8] &= ~(0xff << (a%8))
	 * 
	 * 00 00 00 00 00 00 00 00
	 * |  |  |  |  |  |  |  |
	 * |  |  |  |  |  |  |  +-->[a  ]
	 * |  |  |  |  |  |  +----->[a+1]
	 * |  |  |  |  |  +-------->[a+2]
	 * |  |  |  |  +----------->[a+3]
	 * |  |  |  +-------------->[a+4]
	 * |  |  +----------------->[a+5]
	 * |  +-------------------->[a+6]
	 * +----------------------->[a+7]
	 * 
	 * WHY DO BOTH EXIST?!
	 */
	public synchronized void setContent(byte[] buf){
		int pos = 0,len = buf.length,lin = len-len%8;
		ensureCapacity(len);
		while(pos < lin){//Offset is kept if BIG_ENDIAN
			content[pos/8] = 
						(buf[pos+7] & 0xffl)
					|	((buf[pos+6] & 0xffl) << 8)
					|	((buf[pos+5] & 0xffl) << 16)
					|	((buf[pos+4] & 0xffl) << 24)
					|	((buf[pos+3] & 0xffl) << 32)
					|	((buf[pos+2] & 0xffl) << 40)
					|	((buf[pos+1] & 0xffl) << 48)
					|	((buf[pos] & 0xffl) << 56);
			pos += 8;
		}
		long val = 0;
		switch(len%8){
		case 7:	val |= ((buf[pos+6] & 0xffl) << 8);//$FALL-THROUGH$
		case 6:	val |= ((buf[pos+5] & 0xffl) << 16);//$FALL-THROUGH$
		case 5:	val |= ((buf[pos+4] & 0xffl) << 24);//$FALL-THROUGH$
		case 4:	val |= ((buf[pos+3] & 0xffl) << 32);//$FALL-THROUGH$
		case 3:	val |= ((buf[pos+2] & 0xffl) << 40);//$FALL-THROUGH$
		case 2:	val |= ((buf[pos+1] & 0xffl) << 48);//$FALL-THROUGH$
		case 1:	val |= ((buf[pos] & 0xffl) << 56);
				content[pos/8] = val;
		}
		size = len;
	}
	public synchronized byte[] getContentBytes(){
		long len = size;
		if(len > Integer.MAX_VALUE){
			throw new IllegalStateException();
		}
		byte[] buf;
		int pos = 0;
		try{
			buf = new byte[(int)len];
		}catch(OutOfMemoryError e){
			throw new IllegalStateException(e);
		}
		int lin = buf.length-buf.length%8;
		while(pos < lin){
			long val = content[pos/8];
			buf[pos+7] = (byte)(val);
			buf[pos+6] = (byte)(val >>> 8);
			buf[pos+5] = (byte)(val >>> 16);
			buf[pos+4] = (byte)(val >>> 24);
			buf[pos+3] = (byte)(val >>> 32);
			buf[pos+2] = (byte)(val >>> 40);
			buf[pos+1] = (byte)(val >>> 48);
			buf[pos] = (byte)(val >>> 56);
			pos += 8;
		}
		if(pos < buf.length){
			long val = content[pos/8];
			switch(buf.length%8){
			case 7:buf[pos+6] = (byte)(val >>> 8);//$FALL-THROUGH$
			case 6:buf[pos+5] = (byte)(val >>> 16);//$FALL-THROUGH$
			case 5:buf[pos+4] = (byte)(val >>> 24);//$FALL-THROUGH$
			case 4:buf[pos+3] = (byte)(val >>> 32);//$FALL-THROUGH$
			case 3:buf[pos+2] = (byte)(val >>> 40);//$FALL-THROUGH$
			case 2:buf[pos+1] = (byte)(val >>> 48);//$FALL-THROUGH$
			case 1:buf[pos] = (byte)(val >>> 56);
			}
		}
		return buf;
	}
	public synchronized void setContent(int[] buf){
		int pos = 0,len = buf.length,lin = len-len%2;
		long s = ((long)buf.length)*4;
		ensureCapacity(s);
		while(pos < lin){
			content[pos/2] =
					(buf[pos+1]) |
					((long)buf[pos]) << 32;
			pos += 2;
		}
		if(pos < len){
			content[pos/2] = (((long)buf[pos]) << 32);
		}
		size = s;
	}
	public synchronized int[] getContentIntegers(){
		long ls = (size+3)/4;
		if(ls > Integer.MAX_VALUE){
			throw new IllegalStateException();
		}
		int[] buf = new int[(int)ls];
		int pos = 0,lin = buf.length-buf.length%2;
		while(pos < lin){
			long val = content[pos/2];
			buf[pos] = (int)(val >> 32);
			buf[pos+1] = (int)(val);
			pos += 2;
		}
		if(pos < buf.length){
			buf[pos] = (int)(content[pos/2] >> 32);
		}
		return buf;
	}
	public synchronized void setContent(long[] buf){
		long cap = ((long)buf.length)*8;
		ensureCapacity(cap);
		System.arraycopy(buf, 0, content, 0, buf.length);
		size = cap;
	}
	public synchronized long[] getContentLongs(){
		return Arrays.copyOf(content, (int)((size+7)/8));
	}
	@SuppressWarnings("unused")
	private static long makeLong(byte a,byte b,byte c,byte d,byte e,byte f,byte g,byte h){
		return	((a & 0xffl) << 56)
			|	((b & 0xffl) << 48)
			|	((c & 0xffl) << 40)
			|	((d & 0xffl) << 32)
			|	((e & 0xffl) << 24)
			|	((f & 0xffl) << 16)
			|	((g & 0xffl) << 8)
			|	(h & 0xffl);
	}
	@SuppressWarnings("unused")
	private static long makeLong(int a,int b){
		return	((a & 0xffffffffl) << 32)
			|	(a & 0xffffffffl);
	}
	private static int getByte(long val,int off){
		return (int)( (val >> (56 - (off*8) ) ) & 0xff);
	}
	private static long putByte(long val,int off,int b){
		off = 56-(off*8);
		val &= ~(0xffl << off);
		return val | ((b & 0xffl) << off);
	}
}
