package net.balintgergely.nbt;

import java.nio.CharBuffer;

public class NamedTag<E> extends Tag<E>{
	StringUTF8 name;
	NamedTag(StringUTF8 name,CharBuffer cbuf) {
		super(cbuf);
		this.name = name;
	}
	public NamedTag(NBTType type,StringUTF8 name0,E value) {
		super(type, value);
		name = StringUTF8.valueOf(name0);
	}
	public StringUTF8 getName(){
		return name;
	}
	public StringUTF8 setName(CharSequence name0){
		StringUTF8 pr = name;
		name0 = NBTType.STRING.approve(StringUTF8.valueOf(name0));
		if(!name0.equals(name)){
			name = (StringUTF8)name0;
		}
		return pr;
	}
	@Override
	public String toString(){
		return NBTType.STRING.toString(name)+" : "+super.toString();
	}
}
