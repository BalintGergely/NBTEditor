package net.balintgergely.nbt;

import java.io.Closeable;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * This class is used to read/write the Region file format
 * @author balintgergely
 *
 */
public class Region implements Closeable,Iterable<net.balintgergely.nbt.Region.Chunk>{
	public static final byte	CHUNK_PLAIN = 0,//I made it up... sorry.
								CHUNK_GZIP = 1,
								CHUNK_DEFLATE = 2;
	/**
	 * Applies the default compression format to the output stream. This method
	 * writes the format marker into the output stream and constructs a new compressor for it.
	 * It is guaranteed that this method returns a FilterOutputStream with it's out field set to the specified output stream.
	 * @throws IOException If the specified output stream's write method does.
	 */
	public static FilterOutputStream defaultCompression(OutputStream out) throws IOException{
		out.write(CHUNK_DEFLATE);
		return new DeflaterOutputStream(out);
	}
	private ChunkInputStream lock;
	private void releaseLock(){
		if(lock != null){
			lock.close();
			lock = null;
		}
	}
	public final File file;
	RandomAccessFile channel;
	private Chunk[] chunks;
	private int chunkCount;
	private BitSet sectorSet;
	/**
	 * Opens a region file for read and possibly write access.
	 * @param file0 The file
	 * @param write true if we open for read&write access, false if we open for read only.
	 * @throws IOException
	 */
	public Region(File file0,boolean write) throws IOException {
		boolean flag = file0.exists();
		channel = new RandomAccessFile(file = file0, write ? "rwd" : "r");
		sectorSet = new BitSet((int)(channel.length()/0x1000));
		sectorSet.set(0);
		sectorSet.set(1);
		chunks = new Chunk[0x400];
		if(flag){
			int i = 0;
			while(i < 0x400){
				int val = channel.readInt();
				if((val & 0xff) != 0){
					chunks[i] = new Chunk(val,i);
				}
				i++;
			}
			i = 0;
			while(i < 0x400){
				Chunk ch = chunks[i];
				int val = channel.readInt();
				if(ch != null){
					ch.timestamp = val;
				}
				i++;
			}
			for(Chunk ch : chunks){
				if(ch != null){
					ch.load();
				}
			}
			if(!write){
				sectorSet = null;//SectorSet beyond this point is only used for allocation which we don't do if we don't write.
			}
		}
	}
	public void checkWriteAccess() throws IOException{
		if(sectorSet == null){
			throw new IOException(ros);
		}
	}
	public boolean isReadOnly(){
		return sectorSet == null;
	}
	static String ros = "Read only region";
	public int getChunkCount(){
		return chunkCount;
	}
	public Chunk getChunk(int i){
		Chunk ch = chunks[i];
		if(ch == null){
			synchronized(Region.this){
				ch = chunks[i];
				if(ch == null){
					ch = new Chunk(i);
				}
			}
		}
		return ch;
	}
	public Chunk getChunkAt(int x,int z){
		return getChunk(x + (z * 32));
	}
	public class Chunk extends Tag<Compound>{
		int index,sectorOffset,sectorCount,timestamp;
		private Chunk(int val,int index0) throws IOException{
			super(NBTType.COMPOUND);
			sectorCount = val & 0xff;
			sectorOffset = (val >>>= 8);
			index = index0;
			int max = val+sectorCount;
			while(val < max){
				if(sectorSet.get(val)){
					throw new IOException();//Chunks overlap
				}
				val++;
			}
			sectorSet.set(sectorOffset, max);
		}
		public Chunk(int identifier){
			super(NBTType.COMPOUND);
			if(sectorSet == null){
				throw new IllegalStateException(ros);
			}
			synchronized(Region.this){
				if(chunks[identifier] == null){
					chunks[identifier] = this;
				}else{
					throw new IllegalStateException();
				}
				index = identifier;
			}
		}
		public Region getRegion(){
			return Region.this;
		}
		public int getIndex(){
			return index;
		}
		public Chunk setLocation(int x,int z) throws IOException{
			if(x < 0 || z < 0 || x >= 32 || z >= 32){
				throw new IllegalArgumentException();
			}
			return setIndex(x + (z * 32));
		}
		public Chunk setIndex(int index0) throws IOException{
			checkWriteAccess();
			synchronized(Region.this){
				Chunk old = chunks[index0];
				if(old != this){
					releaseLock();
					setIndexInternal(index0);
					if(old != null){
						old.setIndexInternal(index);
						old.index = index;
					}else{
						channel.seek(index*4);
						channel.writeInt(0);
					}
					index = index0;
				}
				return old;
			}
		}
		public int swapIndex(int index0) throws IOException{
			checkWriteAccess();
			synchronized(Region.this){
				int oa = index;
				if(index0 != index){
					Chunk old = chunks[index0];
					releaseLock();
					setIndexInternal(index0);
					if(old != null){
						old.setIndexInternal(index);
						old.index = index;
					}else{
						channel.seek(index*4);
						channel.writeInt(0);
					}
					index = index0;
				}
				return oa;
			}
		}
		private void setIndexInternal(int index0) throws IOException{
			channel.seek(index0*4);
			channel.writeInt((sectorOffset << 8) | sectorCount);
		}
		public int getX(){
			return index%32;
		}
		public int getZ(){
			return index/32;
		}
		/**
		 * Returns an InputStream to read the content of this Chunk.
		 */
		public InputStream getInputStream(){
			try{
				synchronized(Region.this){
					if(sectorCount == 0){
						return EmptyInputStream.INSTANCE;
					}
					releaseLock();
					ChunkInputStream input = new ChunkInputStream(this);
					lock = input;
					switch(input.read()){
					case CHUNK_PLAIN:return input;
					case CHUNK_GZIP:return new GZIPInputStream(input);
					case CHUNK_DEFLATE:return new InflaterInputStream(input);
					default:throw new IOException("Unknown chunk format");
					}
				}
			}catch(IOException e){
				e.printStackTrace();
				return null;
			}
		}
		/**
		 * Returns an OutputStream to write to the Chunk.<br>
		 * The stream has the default compression and a capacity of 0xFEFFB bytes. It will discard all data
		 * if it's limit is broken. (All though it is possible for it to store more bytes depending on compression)<br>
		 * The returned OutputStream <b>must be closed</b> in order to write it's content into the file. It will not close when it's finalize method is invoked.
		 * @throws IOException If this Region is read-only.
		 */
		@SuppressWarnings("resource")
		public OutputStream getOutputStream() throws IOException{
			checkWriteAccess();
			return defaultCompression(new ChunkOutputStream(this));
		}
		private void writeData(Link<byte[]> link,int bc) throws IOException{
			synchronized(Region.this){
				releaseLock();
				int sc = (bc+0x1003)/0x1000;//Number of sectors
				if(sc != sectorCount){
					deAllocate(sectorOffset,sectorCount);
					sectorOffset = allocate(sectorCount = sc);
					channel.seek(index*4);
					channel.writeInt((sectorOffset << 8) | sc);
				}
				channel.seek(sectorOffset*0x1000);
				channel.writeInt(bc);
				while(link != null){
					channel.write(link.element);
					link = link.next;
				}
			}
		}
		/**
		 * Removes this chunk from the file.
		 */
		public void erase() throws IOException{
			checkWriteAccess();
			synchronized(Region.this){
				releaseLock();
				channel.seek(index*4);
				channel.writeInt(0);
				deAllocate(sectorOffset,sectorCount);
				sectorCount = sectorOffset = 0;
				truncate(2);
			}
		}
		///////////////////////////////////////////////////////////////////////////////////////
		//===================================================================================// Tag related stuff
		///////////////////////////////////////////////////////////////////////////////////////
		
		public void load(){
			synchronized(Region.this){
				boolean pr = value != null;
				if(sectorCount == 0){
					if(pr){
						value = null;
						--chunkCount;
					}
				}else{
					value = NbtIO.read(getInputStream(), -1).readResult;
					if(!pr){
						++chunkCount;
					}
				}
			}
		}
		public void save() throws IOException{
			checkWriteAccess();
			synchronized(Region.this){
				if(value == null){
					erase();
				}else{
					OutputStream out = getOutputStream();
					NbtIO.write(out, value);
					out.close();
				}
			}
		}
		@Override
		public Compound setValue(Compound value) {
			throw new UnsupportedOperationException();
		}
		public void activate(){
			if(sectorSet == null){
				throw new IllegalStateException(ros);
			}
			synchronized(Region.this){
				if(value == null){
					value = new Compound();
					++chunkCount;
				}
			}
		}
		public void deactivate(){
			if(sectorSet == null){
				throw new IllegalStateException(ros);
			}
			synchronized(Region.this){
				if(value != null){
					value = null;
					--chunkCount;
				}
			}
		}
		@Override
		public String toString() {
			Compound c = value;
			if(c != null){
				int s = c.size();
				return "Chunk at ["+getX()+","+getZ()+"] : "+(s == 0 ? "no tags" : (s == 1 ? "1 tag" : s+" tags"));
			}
			return "Chunk at ["+getX()+","+getZ()+"] : no data";
		}
	}
	private class ChunkInputStream extends InputStream{
		private ChunkInputStream(Chunk ch) throws IOException{
			channel.seek(ch.sectorOffset*0x1000);
			rem = channel.readInt();
		}
		private int rem;
		@Override
		public synchronized int read() throws IOException {
			if(rem == 0){
				return -1;
			}
			int i = channel.read();
			if(i >= 0){
				--rem;
			}
			return i;
		}
		@Override
		public synchronized byte[] readAllBytes() throws IOException{
			try{
				byte[] a = new byte[rem];//If we didn't do this, the underlying system would keep re-allocating arrays. Something we want to avoid.
				super.readNBytes(a, 0, a.length);
				return a;
			}catch(NegativeArraySizeException | OutOfMemoryError e){
				throw new IOException(e);//It happens.
			}
		}
		@Override
		public synchronized int read(byte[] b, int off, int len) throws IOException {
			if(off < 0 || len < 0 || off+len > b.length){
				throw new IllegalArgumentException();
			}
			if(rem == 0){
				return -1;
			}
			if(rem > 0 && rem < len){
				len = rem;
			}
			int r = channel.read(b, off, len);
			if(r > 0){
				rem -= r;
			}
			return r;
		}
		@Override
		public synchronized int readNBytes(byte[] b, int off, int len) throws IOException {
			return super.readNBytes(b, off, len);
		}
		@Override
		public synchronized long skip(long n) throws IOException {
			if(n < 0){
				throw new IllegalArgumentException();
			}
			if(n > rem){
				n = rem;
			}
			if(n == 0){
				return 0;
			}
			channel.seek(channel.getFilePointer()+n);
			rem -= n;
			return n;
		}

		@Override
		public synchronized int available() throws IOException {
			return (int)Math.min(rem,channel.length()-channel.getFilePointer());
		}
		@Override
		public synchronized void close(){
			rem = 0;
		}
	}
	private static class ChunkOutputStream extends OutputStream{
		/**
		 * Links of arrays of sectors. The first array is of length 0xFFC. After that, 
		 * each array's length is 0x1000*x (where x is the number of sectors the array contains.)
		 */
		Link<byte[]> head,tail;
		int count,offset;
		Chunk chunk;
		private ChunkOutputStream(Chunk ch){
			chunk = ch;
			head = tail = new Link<>(new byte[0xFFC],null);
			//The first four bytes are needed for the header( 0xFFC = 0x1000 - 4 )
			//Therefore the first sector gets a smaller payload.
		}
		@Override
		public synchronized void write(int b) throws IOException {
			if(tail == null){
				throw new IOException("Closed");
			}
			count++;
			if(offset == tail.element.length){
				dieIfOverflow();
				tail = tail.next = new Link<>(new byte[0x1000],null);
				offset = 0;
			}
			tail.element[offset++] = (byte)(b & 0xff);
		}
		/**
		 * Warning: No bound checking here
		 */
		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			if(tail == null){
				throw new IOException("Closed");
			}
			if(len != 0){
				count += len;
				dieIfOverflow();
				byte[] target = tail.element;
				if(offset+len <= target.length){
					System.arraycopy(b, off, target, offset, len);
					offset += len;
				}else{
					int diff = target.length-offset;
					len -= diff;
					int ni = (len+0xFFF) & 0xFFFFF000;//Number of new sectors to be allocated
					System.arraycopy(b, off, target, offset, diff);//Write first part into previous array
					tail = tail.next = new Link<>(target = new byte[ni],null);//The next array can span multiple sectors if needed.
					System.arraycopy(b, off+diff, target, 0, offset = len);//Write remaining data into new array
				}
			}
		}
		/**
		 * Kills this stream if the limit is breached.
		 * @throws IOException if that happens
		 */
		private void dieIfOverflow() throws IOException{
			if(count > 0xFEFFC){
				tail = head = null;//No point in advancing if that's the case.
				throw new IOException();
			}
		}
		/**
		 * Overwrites the chunk data associated with this OutputStream to the content recorded within the output stream.
		 * Throws IOException if the stream limit is breached.
		 */
		@Override
		public synchronized void close() throws IOException {
			if(count > 0xFEFFC){
				throw new IOException("Stream overflow!");
			}
			if(tail != null){
				if(count == 0){
					chunk.erase();
				}else{
					chunk.writeData(head,count);//Since first array length is...
				}
				tail = head = null;
			}
		}
		@Override
		protected void finalize() throws Throwable{}
	}
	/**
	 * Locates and claims unused space in the File that is at least n sectors long.
	 * This method also overwrites the channel's size.
	 */
	private int allocate(int capacity) throws IOException{
		int index = 0,loc;
		do{
			loc = sectorSet.nextClearBit(index);
			index = sectorSet.nextSetBit(loc);
			if(index < 0){
				channel.setLength((loc+capacity)*0x1000);
				//Unless the File is externally modified, there is no way this will truncate it. Not like *that* would be a problem.
				break;
			}
		}while((index-loc) < capacity);
		if(index >= 0){
			truncate(loc+capacity);
		}
		sectorSet.set(loc, loc+capacity);
		return loc;
	}
	private void deAllocate(int off,int len){
		if(len != 0){
			sectorSet.set(off, off+len, false);
		}
	}
	@Override
	public Iterator<Chunk> iterator(){
		return new Iterator<Chunk>() {
			Chunk next;
			int index;
			@Override
			public boolean hasNext(){
				while(next == null){
					if(index >= chunks.length){
						return false;
					}
					Chunk ch = chunks[index];
					index++;
					if(ch != null && ch.getValue() != null){
						next = ch;
					}
				}
				return true;
			}
			@Override
			public Chunk next() {
				if(!hasNext()){
					throw new NoSuchElementException();
				}
				Chunk ch = next;
				next = null;
				return ch;
			}
			
		};
	}
	/**
	 * Closes this Region. This method cuts all interaction with the File.
	 * Calling any method on this Region or it's Chunks after closing have undefined results.
	 */
	@Override
	public synchronized void close() throws IOException {
		releaseLock();
		channel.close();
	}
	public synchronized void truncate(int minLength) throws IOException{
		checkWriteAccess();
		channel.setLength(Math.max(minLength,sectorSet.length())*0x1000);
	}
	/**
	 * Compresses the region file removing all unused sectors between chunks and decreasing the file length.
	 * After this method returns, the file is guaranteed to be the smallest possible size.
	 * This method may take seconds to run.
	 */
	public synchronized void defrag() throws IOException{
		checkWriteAccess();
		releaseLock();
		int i = 0;
		while(true){
			i = sectorSet.nextClearBit(i);
			int len = sectorSet.nextSetBit(i);
			if(len < 0){
				break;
			}
			len -= i;//Len is now the number of empty sectors beginning with i;
			Chunk c = null,n = null;
			//c: Find a chunk that we can move into this empty space. We want to find the largest one that fits.
			//n: In case we can't find a chunk that fits, we also look for the chunk directly next to us.
			//Don't worry, if c == n != null we still do the best operation we can.
			for(Chunk ch : chunks){
				if(ch != null && ch.sectorCount > 0 && ch.sectorOffset > i){
					if(ch.sectorCount <= len && (c == null || ch.sectorCount > c.sectorCount)){
						c = ch;//Largest chunk that fits
					}else if(n == null || ch.sectorOffset < n.sectorOffset){
						n = ch;//Next chunk will definitely fit.
					}
				}
			}
			if(c == null){
				c = n;//And the copy source and target sectors will overlap.
			}
			if(c == null){
				break;//We are done.
			}
			InputStream in = new ChunkInputStream(c);//Input to read from the chunk
			OutputStream out = new ChunkOutputStream(c);//This is the output buffer
			in.transferTo(out);//Read data to buffer
			in.close();//Close input
			c.erase();//Erase the chunk so we free up it's location
			out.close();//This seeks out a new place for our chunk right where we want it
		}
		truncate(2);
	}
	@Override
	public void finalize() throws Throwable{
		close();
	}
	@Override
	public String toString(){
		int count = getChunkCount();
		if(count == 0){
			return "Empty region";
		}else{
			return "Region of "+count+(count == 1 ? " chunk" : " chunks");
		}
	}
}
