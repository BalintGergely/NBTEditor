package g.nbt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import g.io.ByteArrayOutputStream;
import g.io.LimitedInputStream;
import g.util.Link;

public class Chunk extends NamedTag{
	public static final byte ENCODING_VAL = 2;
	int chunkId;// (x*32)+y
	byte length,encoding;
	int timestamp;
	private Chunk(int chunkId0,byte length0,int timestamp0,byte encoding0,DataInputStream dataIn) throws IOException{
		chunkId = chunkId0;
		length = length0;
		timestamp = timestamp0;
		encoding = encoding0;
		super.readFrom(dataIn,dataIn.read());
	}
	public int getChunkId(){
		return chunkId;
	}
	public void setChunkId(int chunkId0){
		if(chunkId0 < 0 || chunkId0 >= 1024){
			throw new IllegalArgumentException();
		}
		Tag parent = (Tag)getParent();
		int pId = chunkId;
		if(parent != null){
			chunkId = -1;
			Object o = parent.getValue();
			if(o instanceof Region && ((Region)o).contains(this)){
				Chunk rival = ((Region)o).getbyId(chunkId0);
				if(rival != null){
					rival.chunkId = pId;
				}
				chunkId = chunkId0;
				((Region)o).sort();
			}
		}else{
			chunkId = chunkId0;
		}
	}
	public int timestamp(){
		return timestamp;
	}
	Chunk(int chunkId0,String name,int value){
		super(name,value);
		chunkId = chunkId0;
	}
	public Chunk(int chunkId0, String name) {
		super(name,null);
		chunkId = chunkId0;
	}
	/**
	 * 
	 * @param chunkId0
	 * @param input
	 * @param buffer Should be 8 bytes long.
	 * @throws IOException
	 */
	static Chunk read(int chunkId0,FileInputStream input,ByteBuffer buffer) throws IOException {
		int chunkIdm = chunkId0 << 2;
		FileChannel ch = input.getChannel();
		buffer.clear();
		ch.position(chunkIdm);
		buffer.limit(4);
		ch.read(buffer);
		ch.position(chunkIdm+4096);
		buffer.limit(8);
		ch.read(buffer);
		if(buffer.hasRemaining()){
			throw new IOException();
		}
		buffer.flip();//position is 0
		int position = buffer.getInt() >> 8;//+4
		byte length = buffer.get(3);
		if(position < 2 || length <= 0){
			return null;
		}
		buffer.flip();//position is 0, limit is 4
		ch.position(position << 12);//Set the channels position...
		ch.read(buffer);//position is 4
		buffer.position(0);
		LimitedInputStream lim = new LimitedInputStream(input,buffer.getInt());
		buffer.limit(8);//limit is 8
		InputStream dataIn;
		byte encoding = (byte)lim.read();
		switch(encoding){
		case 1:dataIn = new GZIPInputStream(lim);break;
		case 2:dataIn = new InflaterInputStream(lim);break;
		default:throw new IOException();
		}
		return new Chunk(chunkId0, length, buffer.getInt(),encoding,new DataInputStream(dataIn));
	}
	@Override
	@SuppressWarnings("nls")
	public String toString(){
		int x = chunkId%32,z = chunkId/32;
		return "Chunk x "+x+"  z "+z+" > "+super.toString();
	}
	ByteArrayOutputStream content;
	int initWrite() throws IOException{
		content = new ByteArrayOutputStream(5);
		try(DeflaterOutputStream out = new DeflaterOutputStream(content)){
			out.write(super.getTypeCode());
			super.writeTo(new DataOutputStream(out));
			out.finish();
		}
		byte[] bytes = content.bytes.element;
		int s = content.getSize()-4;
		bytes[4] = ENCODING_VAL;
		bytes[3] = (byte)s;
		bytes[2] = (byte)(s >> 8);
		bytes[1] = (byte)(s >> 16);
		bytes[0] = (byte)(s >> 32);
		if(s == 0){
			return 0;
		}
		return (content.getSize() + 0xfff) / 0x1000;
	}
	void write(DataOutputStream out) throws IOException{
		Link<byte[]> b = content.bytes;
		while(b != null){
			out.write(b.element);
			b = b.next;
		}
	}
	@Override
	public void setSignificant(){
		super.setSignificant();
		timestamp = (int)TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}
}
