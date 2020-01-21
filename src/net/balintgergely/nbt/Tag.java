package net.balintgergely.nbt;

import java.nio.CharBuffer;

public class Tag<E>{
	protected E value;
	/**
	 * Can only be null if getClass() is from something other than g.nbt
	 */
	public final NBTType TYPE;
	@SuppressWarnings("unchecked")
	Tag(CharBuffer cbuf) {
		TYPE = NBTType.parseSNBT((Tag<Object>)this, cbuf);
	}
	protected Tag(){
		TYPE = null;
	}
	public Tag(NBTType type,E value0) {
		TYPE = type;
		value = type.approve(value0);
	}
	Tag(NBTType type){
		TYPE = type;
	}
	public E getValue() {
		return value;
	}
	public E setValue(E value0) {
		E v = value;
		value = (TYPE == null ? value0 : TYPE.approve(value0));
		return v;
	}
	@Override
	public final int hashCode(){
		return super.hashCode();
	}
	@Override
	public final boolean equals(Object that){
		return this == that;
	}
	@Override
	public String toString(){
		return value == null ? "" : TYPE.toString(value);
	}
}
