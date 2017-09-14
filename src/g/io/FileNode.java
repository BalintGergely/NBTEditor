package g.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

import g.nbt.NamedTag;
import g.nbt.Region;

public class FileNode extends NamedTag implements BaseFileNode{
	File file;
	/**
	 * 0 No data,-1 Scanned,1 Plain,2 GZIP,3 ZLIB,4 MCRegion
	 */
	private int state;
	/**
	 * phantomType: Stores the type of the file's root. Cached while scanning.
	 */
	int phantomType;
	/**
	 * True if the file was modified.
	 */
	boolean significant;
	public int getState(){
		return state;
	}
	public boolean scheduleInfo;
	public FileNode(File file0) {
		file = Objects.requireNonNull(file0);
	}
	FileNode(String name,int typeCode0){
		super(name,typeCode0);
		state = 1;
	}
	FileNode(String name,Object value0){
		super(name,value0);
		state = 1;
	}
	FileNode(File file0,DirectoryNode parent0){
		file = file0;
		super.setParent(parent0);
	}
	public synchronized int scan(){
		try(FileInputStream fileIn = new FileInputStream(file)){
			if(state == -1){
				state++;
			}
			while(state < 4){
				fileIn.getChannel().position(0);
				state++;
				try{
					phantomType = scan(fileIn,state);
					return state;
				}catch(IOException e){
					//
				}
			}
			state = -1;
		} catch (IOException e1) {
			//
		} finally {
			System.out.println("Scanned "+file); //$NON-NLS-1$
		}
		return state;
	}
	/**
	 * WARNING this does not resets the value
	 * @param state0
	 */
	public void setState(int state0){
		if(state0 < -1 || state0 > 4){
			throw new IllegalArgumentException();
		}
		state = state0;
	}
	private static void regionScan(FileInputStream fileIn) throws IOException{
		try{
			int i = 0;
			FileChannel ch = fileIn.getChannel();
			long len = ch.size();
			if(len % 0x1000 != 0 || len < 0x2000){
				throw new IOException(Long.toString(len));
			}
			ByteBuffer buffer = ByteBuffer.allocate(4);
			while(i < 1024){
				int chunkIdm = (i++) << 2;
				buffer.clear();
				if(chunkIdm >= len){
					throw new IOException();
				}
				ch.position(chunkIdm);
				buffer.limit(4);
				ch.read(buffer);
				buffer.flip();
				int position = buffer.getInt() >> 8;//+4
				if(position << 12 >= len){
					throw new IOException();
				}
				byte length = buffer.get(3);
				if(position < 2 || length <= 0){
					continue;
				}
				buffer.clear();
				ch.position(position << 12);//Set the channel's position...
				ch.read(buffer);//position is 4
				buffer.flip();
				int size = buffer.getInt();
				CounterInputStream cIn = new CounterInputStream(fileIn);
				InputStream dataIn;
				switch(cIn.read()){
				case 1:dataIn = new GZIPInputStream(cIn);break;
				case 2:dataIn = new InflaterInputStream(cIn);break;
				default:throw new IOException();
				}
				baseScan(new DataInputStream(dataIn));
			}
		}catch(BufferOverflowException | BufferUnderflowException e){
			throw new IOException(e);
		}
	}
	@Override
	public boolean isLeaf(){
		return state <= 0;
	}
	@Override
	public boolean getAllowsChildren(){
		return value != null && super.getAllowsChildren();
	}
	public boolean isLoaded(){
		return super.value != null;
	}
	@Override
	public int getTypeCode(){
		if(super.value != null){
			return super.getTypeCode();
		}
		return phantomType;
	}
	private static int baseScan(DataInputStream dataIn) throws IOException{
		int i = dataIn.readUnsignedByte();
		recursiveScan(dataIn,8);
		recursiveScan(dataIn,i);
		return i;
	}
	private static void recursiveScan(DataInputStream dataIn,int type) throws IOException{
		switch(type){
		case 1:dataIn.readByte();break;
		case 2:dataIn.readShort();break;
		case 3:dataIn.readInt();break;
		case 4:dataIn.readLong();break;
		case 5:dataIn.readFloat();break;
		case 6:dataIn.readDouble();break;
		case 7:forceSkip(dataIn,dataIn.readInt());break;
		case 8:forceSkip(dataIn,dataIn.readUnsignedShort());break;
		case 9:int i = dataIn.readUnsignedByte(),b = dataIn.readInt();
				if(b == 0){
					break;
				}
				if(i == 0 || i > 11){
					throw new IOException();
				}
				switch(i){
				case 1:forceSkip(dataIn,b);return;
				case 2:forceSkip(dataIn,b*2);return;
				case 3:
				case 5:forceSkip(dataIn,b*4);return;
				case 4:
				case 6:forceSkip(dataIn,b*8);return;
				}
				int a = 0;
				while(a < b){
					recursiveScan(dataIn,i);
					a++;
				}break;
		case 10:
			while((i = dataIn.readUnsignedByte()) != 0){
				recursiveScan(dataIn,8);
				recursiveScan(dataIn,i);
			}break;
		case 11:
			forceSkip(dataIn,((long)dataIn.readInt())*4);break;
		case 12:
			forceSkip(dataIn,((long)dataIn.readInt())*8);break;
		default:throw new IOException(Integer.toString(type));
		}
	}
	private static void forceSkip(DataInputStream dataIn,long skip) throws IOException{
		if(dataIn.skip(skip) < skip){
			throw new EOFException();
		}
	}
	@Override
	public void safeCheck(){
		if(value instanceof Region){
			((Region) value).safeCheck();
		}else if(value != null){
			super.safeCheck();
		}
	}
	@Override
	public boolean saveWave(){
		return write();
	}
	/*
	 * Load,scan,save
	 * @return
	 */
	public synchronized boolean load(){
		if(state <= 0){
			return false;
		}
		try(FileInputStream fileIn = new FileInputStream(file)){
			DataInputStream dataIn;
			switch(state){
			case 1:dataIn = new DataInputStream(fileIn);
				super.readFrom(dataIn, dataIn.readUnsignedByte());break;
			case 2:dataIn = new DataInputStream(new GZIPInputStream(fileIn));
				super.readFrom(dataIn, dataIn.readUnsignedByte());break;
			case 3:dataIn = new DataInputStream(new InflaterInputStream(fileIn));
				super.readFrom(dataIn, dataIn.readUnsignedByte());break;
			case 4:super.setValue(new Region(this,fileIn));
			}
			System.out.println("Loaded "+file); //$NON-NLS-1$
			phantomType = 0;
			return true;
		} catch (IOException e) {
			// 
		}
		return false;
	}
	public static int scan(FileInputStream fileIn,int type) throws IOException{
		switch(type){
		case 1:return baseScan(new DataInputStream(fileIn));
		case 2:return baseScan(new DataInputStream(new GZIPInputStream(fileIn)));
		case 3:return baseScan(new DataInputStream(new InflaterInputStream(fileIn)));
		case 4:regionScan(fileIn);return 128;
		default:throw new IllegalArgumentException();
		}
	}
	/**
	 * The writer comes equipped with three layers of anti corruption defense, so that our users rather don't
	 * overwrite good files with corrupt ones.
	 * @return
	 */
	public synchronized boolean write(){
		if(state < 1 || state > 4 || value == null){
			return true;
		}
		try{
			safeCheck();
			File temp = File.createTempFile(file.getName(),".tmp",file.getParentFile()); //$NON-NLS-1$
			try(FileOutputStream fileOut = new FileOutputStream(temp)){
				DataOutputStream main;
				switch(state){
				case 1:writeTo(main = new DataOutputStream(fileOut));break;
				case 2:writeTo(main = new DataOutputStream(new GZIPOutputStream(fileOut,true)));break;
				case 3:writeTo(main = new DataOutputStream(new DeflaterOutputStream(fileOut,true)));break;
				case 4:((Region)value).writeOut(main = new DataOutputStream(fileOut));break;
				default:throw new IOException();
				}
				main.flush();
			}
			try(FileInputStream fileIn = new FileInputStream(temp)){
				scan(fileIn,state);
			}
			Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}catch(Exception e){
			System.err.println(file+" : "+state); //$NON-NLS-1$
			e.printStackTrace();
			return false;
		}
		significant = false;
		return true;
	}
	@Override
	protected void writeTo(DataOutputStream out) throws IOException{
		out.write(super.getTypeCode());
		super.writeTo(out);
	}
	@Override
	public String toString(){
		if(getValue() != null){
			Object v = getValue();
			if(v instanceof Region){
				return file.getName()+" -> "+v.toString(); //$NON-NLS-1$
			}
			return file.getName()+" -> "+super.toString(); //$NON-NLS-1$
		}
		return file.getName();
	}
	/**
	 * Pass through method for RemappableFileNode
	 * @return
	 */
	public String toTagString(){
		if(value instanceof Region){
			return value.toString();
		}
		return super.toString();
	}
	@Override
	public void setSignificant() {
		significant = true;
		DirectoryNode node = (DirectoryNode)super.getParent();
		if(node != null){
			node.setSignificant();
		}
	}
	@Override
	public boolean isSignificant() {
		return significant;
	}
	@Override
	public File getFile() {
		return file;
	}
}