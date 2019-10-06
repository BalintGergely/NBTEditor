package net.balintgergely.nbt;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

import static net.balintgergely.nbt.NBTType.*;
/**
 * Responsible for IO operations regarding the NBT format. This class also implements some functionality to
 * restore corrupted data.
 * @author balintgergely
 *
 */
public final class NbtIO implements Comparable<NbtIO>{
	public static final int DEFAULT_BUFFER_SIZE = 8192;
	/**
	 * Holds an IOException if there was any caught during the reading procedure.
	 */
	public IOException exception;
	/**
	 * Holds the total number of tags read.
	 */
	public int tagCount;
	/**
	 * Holds the number of tags contained in the first entry in the result compound.
	 * Will hold -1 if the first entry could not be fully read.
	 */
	public int integrity;
	/**
	 * Holds the result of the reading procedure.
	 */
	public Compound readResult;
	/**
	 * Compares this instance with another one in an attempt to determine which one has better results assuming they both used the same source.
	 */
	@Override
	public int compareTo(NbtIO o) {
		if(o == null){
			return 1;
		}else if(o.integrity < integrity){
			return 1;
		}else if(o.integrity > integrity){
			return -1;
		}else if(readResult == null){
			return o.readResult == null ? 0 : -1;
		}else if(o.readResult == null){
			return 1;
		}else if(o.tagCount < tagCount){
			return 1;
		}else if(o.tagCount > tagCount){
			return -1;
		}else{
			return 0;
		}
	}
	private NbtIO(DataInputStream input,int maxTags) {
		integrity = -1;
		tagCount = 0;
		if(maxTags == 0){
			return;
		}
		int value,size = 0;
		LinkedList<NamedTag<?>> tlk = new LinkedList<>();
		try{
			while((value = input.read()) > 0){
				NBTType tp = toType(value);
				StringUTF8 name = (StringUTF8)readValue(STRING,input);
				tlk.add(new NamedTag<Object>(tp,name,readValue(tp,input)));
				if(exception == null && size == 0){
					integrity = tagCount;
				}
				size++;
				if(exception != null || size == maxTags){
					break;
				}
			}
		}catch(IOException e){
			exception = e;
			if(size == 0){
				readResult = null;
				return;
			}
		}
		readResult = new Compound();
		readResult.addAll(tlk);
	}
	/**
	 * Reads as much data as it can from the input stream. Returns an instance of this class holding the result of the reading procedure.
	 */
	@SuppressWarnings("resource")
	public static NbtIO read(InputStream in,int maxTags){
		DataInputStream input;
		if(in.getClass() == DataInputStream.class){
			input = (DataInputStream)in;
		}else{
			input = new DataInputStream(in);
		}
		return new NbtIO(input,maxTags);
	}
	/**
	 * This method will only throw an IOException if there was absolutely no data it could recover.
	 */
	private Object readValue(NBTType type,DataInputStream input) throws IOException{
		if(exception != null){
			throw exception;
		}
		try{
			switch(type){
			case END:throw new IOException();
			
			case BYTE:return Byte.valueOf(input.readByte());
			case SHORT:return Short.valueOf(input.readShort());
			case INTEGER:return Integer.valueOf(input.readInt());
			case LONG:return Long.valueOf(input.readLong());
			case FLOAT:return Float.valueOf(input.readFloat());
			case DOUBLE:return Double.valueOf(input.readDouble());
			
			case BYTE_ARRAY:
						byte[] bytes = new byte[input.readInt()];
						try{
							input.readFully(bytes);
						}catch(IOException e){
							exception = e;
						}
						return bytes;
			case STRING:bytes = new byte[input.readUnsignedShort()];
						try{
							input.readFully(bytes);
						}catch(IOException e){
							exception = e;
						}
						return new StringUTF8(bytes);
			case LIST:	NBTType tp = toType(input.readUnsignedByte());
						int size =  input.readInt(),count = 0;
						LinkedList<Tag<Object>> tl = new LinkedList<>();
						while(count < size){
							try{
								tl.add(new Tag<>(tp,readValue(tp,input)));
							}catch(IOException e){
								exception = e;
								break;
							}
							tagCount++;
							count++;
						}
						TagList<Object> lst = new TagList<>();
						lst.addAll(tl);
						return lst;
			case COMPOUND:
						int value;
						size = 0;
						LinkedList<NamedTag<?>> tlk = new LinkedList<>();
						while((value = input.readUnsignedByte()) != 0){
							try{
								tp = toType(value);
								StringUTF8 name = (StringUTF8)readValue(STRING,input);
								tlk.add(new NamedTag<Object>(tp,name,readValue(tp,input)));
							}catch(IOException e){
								exception = e;
								break;
							}
							tagCount++;
							size++;
						}
						Compound cmp = new Compound();
						cmp.addAll(tlk);
						return cmp;
			case INT_ARRAY:
						int[] ints = new int[input.readInt()];
						count = 0;
						try{
							while(count < ints.length){
								ints[count] = input.readInt();
								count++;
							}
						}catch(IOException e){
							exception = e;
						}
						return ints;
			case LONG_ARRAY:
						long[] longs = new long[input.readInt()];
						count = 0;
						try{
							while(count < longs.length){
								longs[count] = input.readLong();
								count++;
							}
						}catch(IOException e){
							exception = e;
						}
						return longs;
			default:throw new IncompatibleClassChangeError();
			}
		}catch(RuntimeException | OutOfMemoryError e){
			//OutOfMemoryError is handled because it can be thrown if we try to create an array too big.
			//RuntimeException can come from negative array size.
			throw new IOException(e);
		}
	}
	private static NBTType toType(int i) throws IOException{
		try{
			return NBTType.get(i);
		}catch(Exception e){
			throw new IOException(e);
		}
	}
	public static void write(OutputStream out,Compound comp) throws IOException{
		DataOutputStream output = new DataOutputStream(new BufferedOutputStream(out, DEFAULT_BUFFER_SIZE));
		for(NamedTag<?> tag : comp){
			output.write(tag.TYPE.typeCode);
			write(output, STRING, tag.name);
			write(output, tag.TYPE, tag.value);
		}
		//The compound we get here is the file space itself, so EOF is good enough even for MineCraft.
		//(Mostly because MineCraft does not attempt to read a second tag at all.)
		output.flush();
	}
	private static void write(DataOutputStream output,NBTType type,Object value) throws IOException{
		switch(type){
		case END:throw new IOException();
		
		case BYTE:output.writeByte(Byte.toUnsignedInt(  ((Number)value).byteValue()  ));break;
		case SHORT:output.writeShort(Short.toUnsignedInt(  ((Number)value).shortValue()  ));break;
		case INTEGER:output.writeInt(  ((Number)value).intValue()  );break;
		case LONG:output.writeLong(  ((Number)value).longValue()  );break;
		case FLOAT:output.writeFloat(  ((Number)value).floatValue()  );break;
		case DOUBLE:output.writeDouble(  ((Number)value).doubleValue()  );break;
		
		case BYTE_ARRAY:byte[] bytes = (byte[])value;
						output.writeInt(bytes.length);
						output.write(bytes);break;
		case STRING:bytes = ((StringUTF8)value).getBytes();
					if(bytes.length > 0xffff){
						throw new IOException();
					}
					output.writeShort(bytes.length);
					int index = 0;
					while(index < bytes.length){
						int len = Math.min(DEFAULT_BUFFER_SIZE-1, bytes.length-index);
						output.write(bytes,index,len);
						index += len;
					}break;
		case LIST:TagList<?> lst = (TagList<?>)value;
					NBTType ltp = lst.type();
					if(ltp == null){
						output.write(0);
						output.writeInt(0);
					}else{
						output.write(ltp.ordinal());
						int s = lst.size();
						output.writeInt(s);
						for(int c = 0;c != s;c++){
							Object obj;
							try{
								obj = ltp.approve(lst.get(c).getValue());
							}catch(Exception e){
								throw new IOException(e);
							}
							write(output,ltp,obj);
						}
					}break;
		case COMPOUND:
				Compound comp = (Compound)value;
				for(NamedTag<?> tag : comp){
					output.write(tag.TYPE.typeCode);
					write(output, STRING, tag.name);
					write(output, tag.TYPE, tag.value);
				}
				output.write(0);break;
		case INT_ARRAY:
				int[] ints = (int[])value;
				output.writeInt(ints.length);
				for(int i : ints){
					output.writeInt(i);
				}break;
		case LONG_ARRAY:
				long[] longs = (long[])value;
				output.writeInt(longs.length);
				for(long l : longs){
					output.writeLong(l);
				}break;
		default:throw new IncompatibleClassChangeError();
		}
	}
}
