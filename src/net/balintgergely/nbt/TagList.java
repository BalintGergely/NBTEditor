package net.balintgergely.nbt;

public final class TagList<E> extends Container<Tag<E>>{
	private static final long serialVersionUID = 1L;
	@Override
	public boolean accept(Tag<E> val) {
		NBTType t = type();
		return	val.getClass() == Tag.class &&
				/*
				 * if  (t == null) then (val.TYPE cannot be t)
				 * if !(t == null) then (val.TYPE must be t)
				 * val.TYPE is guaranteed to be non-null
				 */
				( (t == null) != (t == val.TYPE) );
	}
	public TagList(){}
	public NBTType type(){
		if(super.isEmpty()){
			return null;
		}
		return super.get(0).TYPE;
	}
	@Override
	public String toString(){
		int s = size();
		NBTType type = type();
		if(type == null){
			return "Empty "+NBTType.LIST.name;
		}else{
			return NBTType.LIST.name+" of "+s+" "+type.name+(s == 1 ? "" : "s");
		}
	}
	@Override
	public TagList<E> clone(){
		return (TagList<E>)super.clone();
	}
}
