package g.nbt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NamedTag extends Tag implements Comparable<NamedTag>{
	private String name;
	public NamedTag(String name0,int typeCode){
		super(typeCode);
		setName(name0);
	}
	public NamedTag(String name0,Object value0){
		super(value0);
		setName(name0);
	}
	protected NamedTag(){}
	NamedTag(Tag parent0,DataInputStream dataIn,int type) throws IOException{
		super(parent0);
		readFrom(dataIn,type);
	}
	@Override
	protected void readFrom(DataInputStream dataIn,int type) throws IOException{
		name = readString(dataIn);
		super.readFrom(dataIn, type);
	}
	@Override
	protected void writeTo(DataOutputStream dataOut) throws IOException{
		writeString(dataOut,name);
		super.writeTo(dataOut);
	}
	public String getName(){
		return name;
	}
	public String setName(String nN){
		nN = nN == null ? new String() : nN;
		byte[] bytes = nN.getBytes();
		if(bytes.length > 0xffff){
			nN = new String(bytes, 0, 0xffff, charset);
		}
		return name = nN;
	}
	@Override
	public String toString(){
		return '"'+name+"\" : "+super.toString(); //$NON-NLS-1$
	}
	@Override
	public int compareTo(NamedTag o) {
		int i = name.compareTo(o.name);
		if(i == 0){
			return Integer.compare(getTypeCode(), o.getTypeCode());
		}
		return i;
	}
	@Override
	public void safeCheck(){
		safeCheckString(name);
		super.safeCheck();
	}
}
