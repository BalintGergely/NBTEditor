package net.balintgergely.nbt;

/**
 * All though regular compounds are not supposed to handle multiple tags with the same name, we have no problem
 * doing so because we can always treat the elements as a list.
 * @author balintgergely
 *
 */
public final class Compound extends Container<NamedTag<?>>{
	private static final long serialVersionUID = 1L;
	public Compound(){}
	@Override
	public boolean accept(NamedTag<?> tag){
		return tag.getClass() == NamedTag.class;
	}
	@Override
	public Compound clone(){
		return (Compound)super.clone();
	}
	@Override
	public String toString(){
		int s = size();
		if(s == 0){
			return "Empty "+NBTType.COMPOUND;
		}
		return NBTType.COMPOUND.name+" of "+s+(s == 1 ? " tag" : " tags");
	}
}
