package g.nbt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TagList extends TagCollection<Tag>{
	TagList(Tag parent0) {
		super(parent0, Tag.class);
	}
	TagList(Tag parent0,DataInputStream dataIn) throws IOException{
		this(parent0);
		int type = dataIn.readUnsignedByte(),len = dataIn.readInt();
		super.ensureCapacity(len);
		while(size() < len){
			add(new Tag(parent0,dataIn,type));
		}
	}
	@Override
	@SuppressWarnings("nls")
	public String toString(){
		if(size() > 0){
			return "List of "+Tag.getTypeName(getTypeCode())+" "+size()+" entries.";
		}
		return "Empty list";
	}
	@Override
	public boolean add(Tag node){
		int typeCode = getTypeCode();
		if(typeCode == 0 || node.getTypeCode() == typeCode){
			return super.add(node);
		}
		return false;
	}
	@Override
	public boolean add(int index,Tag node){
		int typeCode = getTypeCode();
		if(typeCode == 0 || node.getTypeCode() == typeCode){
			return super.add(index,node);
		}
		return false;
	}
	public int getTypeCode(){
		if(size() <= 0){
			return 0;
		}
		return get(0).getTypeCode();
	}
	@Override
	public void writeOut(DataOutputStream dataOut) throws IOException {
		int i = getTypeCode();
		dataOut.writeByte(i);
		dataOut.writeInt(size());
		for(Tag o : this){
			o.writeTo(dataOut);
		}
	}
	@Override
	public void safeCheck(){
		if(size() != 0){
			int i = 1,t = get(0).getTypeCode();
			get(0).safeCheck();
			while(i < size()){
				if(get(i).getTypeCode() != t){
					throw new ArrayStoreException();
				}
				get(i).safeCheck();
				i++;
			}
		}
	}
}
