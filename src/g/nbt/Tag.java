package g.nbt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.BufferOverflowException;
import java.nio.charset.Charset;
import java.util.Enumeration;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import g.io.FileNode;

public class Tag implements MutableTreeNode{
	protected Object value;
	WeakReference<TreeNode> parent;
	public static final Charset charset = Charset.forName("UTF-8"); //$NON-NLS-1$
	public void setParent(TreeNode parent0){
		if(getParent() == null){
			parent = new WeakReference<>(parent0);
		}
	}
	@Override
	public void setParent(MutableTreeNode parent0){
		setParent((TreeNode)parent0);
	}
	protected Tag(){}
	public Tag(Object value0){
		value = value0;
	}
	public Tag(int type){
		switch(type){
		case 1:value = new Byte((byte) 0x00);break;
		case 2:value = new Short((short) 0x00);break;
		case 3:value = new Integer(0);break;
		case 4:value = new Long(0);break;
		case 5:value = new Float(0);break;
		case 6:value = new Double(0);break;
		case 7:value = new byte[0];break;
		case 8:value = new String();break;
		case 9:value = new TagList(this);break;
		case 10:value = new TagCompound(this);break;
		case 11:value = new int[0];break;
		case 12:value = new long[0];break;
		case 128:value = new Region((FileNode)this);break;
		default:throw new IllegalArgumentException();
		}
	}
	Tag(Tag parent0){
		parent = new WeakReference<>(parent0);
	}
	Tag(Tag parent0,DataInputStream dataIn,int type) throws IOException{
		this(parent0);
		readFrom(dataIn,type);
	}
	public Object getValue(){
		return value;
	}
	@Override
	public void setUserObject(Object o){
		//Do nothing
	}
	public void setValue(Object value0){
		if(value0 instanceof String){
			String nN = (String)value0;
			byte[] bytes = nN.getBytes();
			if(bytes.length > 0xffff){
				value0 = new String(bytes, 0, 0xffff, charset);
			}
		}
		value = value0;
		return;
	}
	@Override
	public TreeNode getChildAt(int childIndex) {
		if(value instanceof TagCollection){
			return ((TagCollection<?>)value).get(childIndex);
		}
		return null;
	}
	@Override
	public int getChildCount() {
		if(value instanceof TagCollection){
			return ((TagCollection<?>)value).size();
		}
		return 0;
	}
	@Override
	public TreeNode getParent() {
		if(parent == null){
			return null;
		}
		return parent.get();
	}
	@Override
	public int getIndex(TreeNode node) {
		if(value instanceof TagCollection){
			return ((TagCollection<?>)value).indexOf(node);
		}
		return -1;
	}
	@Override
	public boolean getAllowsChildren() {
		return getValue() instanceof TagCollection;
	}
	@Override
	public boolean isLeaf() {
		if(value instanceof TagCollection){
			return ((TagCollection<?>)value).isEmpty();
		}
		return true;
	}
	public static void safeCheckString(String str){
		byte[] bytes = str.getBytes(charset);
		if(bytes.length > 0xffff){
			throw new BufferOverflowException();
		}
	}
	@Override
	public Enumeration<? extends TreeNode> children() {
		if(value instanceof TagCollection){
			return ((TagCollection<?>)value).iterator();
		}
		return null;
	}
	static String readString(DataInputStream dataIn) throws IOException{
		byte[] bytes = new byte[dataIn.readUnsignedShort()];
		dataIn.readFully(bytes);
		return new String(bytes,charset);
	}
	static void writeString(DataOutputStream dataOut,String str) throws IOException{
		byte[] bytes = str.getBytes(charset);
		dataOut.writeShort(bytes.length);
		dataOut.write(bytes);
	}
	void readFrom(DataInputStream dataIn,int type) throws IOException{
		switch(type){
		case 1:value = new Byte(dataIn.readByte());break;
		case 2:value = new Short(dataIn.readShort());break;
		case 3:value = new Integer(dataIn.readInt());break;
		case 4:value = new Long(dataIn.readLong());break;
		case 5:value = new Float(dataIn.readFloat());break;
		case 6:value = new Double(dataIn.readDouble());break;
		case 7:byte[] bytes = new byte[dataIn.readInt()];
				dataIn.readFully(bytes);
				value = bytes;break;
		case 8:value = readString(dataIn);break;
		case 9:value = new TagList(this,dataIn);break;
		case 10:value = new TagCompound(this, dataIn);break;
		case 11:int[] ints = new int[dataIn.readInt()];
				int i = 0;
				while(i < ints.length){
					ints[i] = dataIn.readInt();
					i++;
				}
				value = ints;break;
		case 12:long[] longs = new long[dataIn.readInt()];
				i = 0;
				while(i < longs.length){
					longs[i] = dataIn.readLong();
					i++;
				}
				value = longs;break;
		default:throw new IOException();
		}
	}
	void writeTo(DataOutputStream dataOut) throws IOException{
		writeTo(dataOut,value);
	}
	static void writeTo(DataOutputStream dataOut,Object value) throws IOException{
		switch(typeCode(value.getClass())){
		case 1:dataOut.writeByte(((Number)value).byteValue());break;
		case 2:dataOut.writeShort(((Number)value).shortValue());break;
		case 3:dataOut.writeInt(((Number)value).intValue());break;
		case 4:dataOut.writeLong(((Number)value).longValue());break;
		case 5:dataOut.writeFloat(((Number)value).floatValue());break;
		case 6:dataOut.writeDouble(((Number)value).doubleValue());break;
		case 7:byte[] bytes = (byte[])value;
			dataOut.writeInt(bytes.length);
			dataOut.write(bytes);break;
		case 8:writeString(dataOut,value.toString());break;
		case 9:
		case 10:
		case 128:((TagCollection<?>)value).writeOut(dataOut);break;
		case 11:int[] ints = (int[])value;
			dataOut.writeInt(ints.length);
			for(int i : ints){
				dataOut.writeInt(i);
			}break;
		case 12:long[] longs = (long[])value;
			dataOut.writeInt(longs.length);
			for(long i : longs){
				dataOut.writeLong(i);
			}break;
		default:throw new IOException();
		}
	}
	public static int typeCode(Class<?> clasz){
		if(clasz == Byte.class){
			return 1;
		}
		if(clasz == Short.class){
			return 2;
		}
		if(clasz == Integer.class){
			return 3;
		}
		if(clasz == Long.class){
			return 4;
		}
		if(clasz == Float.class){
			return 5;
		}
		if(clasz == Double.class){
			return 6;
		}
		if(clasz == byte[].class){
			return 7;
		}
		if(clasz == String.class){
			return 8;
		}
		if(TagList.class.isAssignableFrom(clasz)){
			return 9;
		}
		if(TagCompound.class.isAssignableFrom(clasz)){
			return 10;
		}
		if(clasz == int[].class){
			return 11;
		}
		if(clasz == long[].class){
			return 12;
		}
		if(Region.class.isAssignableFrom(clasz)){
			return 128;
		}
		throw new IllegalArgumentException(clasz.getName());
	}
	public static String getTypeName(int typeCode){
		switch(typeCode){
		case 1:return "BYTE"; //$NON-NLS-1$
		case 2:return "SHORT"; //$NON-NLS-1$
		case 3:return "INTEGER"; //$NON-NLS-1$
		case 4:return "LONG"; //$NON-NLS-1$
		case 5:return "FLOAT"; //$NON-NLS-1$
		case 6:return "DOUBLE"; //$NON-NLS-1$
		case 7:return "BYTE_ARRAY"; //$NON-NLS-1$
		case 8:return "STRING"; //$NON-NLS-1$
		case 9:return "LIST"; //$NON-NLS-1$
		case 10:return "COMPOUND"; //$NON-NLS-1$
		case 11:return "INT_ARRAY"; //$NON-NLS-1$
		case 12:return "LONG_ARRAY";
		default:return null;
		}
	}
	public static int getTypeCode(String typeName){
		switch(typeName){
		case "BYTE":return 1; //$NON-NLS-1$
		case "SHORT":return 2; //$NON-NLS-1$
		case "INTEGER":return 3; //$NON-NLS-1$
		case "LONG":return 4; //$NON-NLS-1$
		case "FLOAT":return 5; //$NON-NLS-1$
		case "DOUBLE":return 6; //$NON-NLS-1$
		case "BYTE_ARRAY":return 7; //$NON-NLS-1$
		case "STRING":return 8; //$NON-NLS-1$
		case "LIST":return 9; //$NON-NLS-1$
		case "COMPOUND":return 10; //$NON-NLS-1$
		case "INT_ARRAY":return 11; //$NON-NLS-1$
		case "LONG_ARRAY":return 12;
		default:return 0;
		}
	}
	@Override
	@SuppressWarnings("nls")
	public String toString(){
		return valueString(value);
	}
	public static String valueString(Object value){
		if(value == null){
			return null;
		}
		switch(typeCode(value.getClass())){
		case 1:return value.toString()+'b';
		case 2:return value.toString()+'s';
		case 3:return value.toString()+'i';
		case 4:return value.toString()+'L';
		case 5:return value.toString()+'f';
		case 6:return value.toString()+'D';
		case 7:return ((byte[])value).length+" bytes";
		case 8:return '"'+value.toString()+'"';
		case 9:
		case 10:return value.toString();
		case 11:int[] i = (int[])value;
				return i.length+" ints ("+((long)i.length)*4+" bytes)";
		case 12:long[] l = (long[])value;
				return l.length+" longs("+((long)l.length)*8+" bytes)";
		default:return "";
		}
	}
	public int getTypeCode(){
		if(value == null){
			return 0;
		}
		return typeCode(value.getClass());
	}
	public void safeCheck(){
		switch(getTypeCode()){
		case 9:
		case 10:((TagCollection<?>)value).safeCheck();//$FALL-THROUGH$
		case 1:
		case 2:
		case 5:
		case 3:
		case 6:
		case 4:
		case 7:
		case 11:
		case 12:return;
		case 8:safeCheckString(value.toString());return;
		default:throw new IllegalStateException();
		}
	}
	/*
	 * Very rough implementations.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void insert(MutableTreeNode child, int index) {
		((TagCollection<Tag>)value).add(index, (Tag)child);
	}
	@Override
	@SuppressWarnings("unchecked")
	public void remove(int index) {
		((TagCollection<Tag>)value).remove(index);
	}
	@Override
	@SuppressWarnings("unchecked")
	public void remove(MutableTreeNode node) {
		((TagCollection<Tag>)value).remove(node);
	}
	@Override
	public void removeFromParent() {
		((MutableTreeNode) getParent()).remove(this);
	}
	public void setSignificant(){
		Tag node = (Tag)getParent();
		if(node != null){
			node.setSignificant();
		}
	}
}
