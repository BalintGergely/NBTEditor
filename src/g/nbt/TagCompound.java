package g.nbt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import g.util.Link;

public class TagCompound extends TagCollection<NamedTag>{
	TagCompound(Tag parent0){
		super(parent0,NamedTag.class);
	}
	TagCompound(Tag parent0,DataInputStream dataIn) throws IOException{
		this(parent0);
		Link<NamedTag> first = null,link = null;
		int type,len = 0;
		while((type = dataIn.readUnsignedByte()) != 0){
			NamedTag node = new NamedTag(parent0,dataIn,type);
			if(link == null){
				first = link = new Link<>(node,null);
			}else{
				link = (link.next = new Link<>(node,null));
			}
			len++;
		}
		ensureCapacity(len);
		while(first != null){
			add(first.element);
			first = first.next;
		}
	}
	@Override
	@SuppressWarnings("nls")
	public String toString(){
		return size()+" tags";
	}
	@Override
	public void writeOut(DataOutputStream dataOut) throws IOException {
		for(NamedTag node : this){
			dataOut.writeByte(node.getTypeCode());
			node.writeTo(dataOut);
		}
		dataOut.writeByte(0);
	}
}
