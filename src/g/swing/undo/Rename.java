package g.swing.undo;

import g.nbt.NamedTag;
import g.nbt.Tag;

public class Rename extends AbstractTagEdit{
	private String a,b;
	public Rename(NamedTag node1,String a1,String b1) {
		super(node1);
		a = a1;
		b = b1;
	}
	@Override
	public void undo(){
		super.undo();
		((NamedTag)node).setName(a);
	}
	@Override
	public void redo(){
		super.redo();
		((NamedTag)node).setName(b);
	}
	@Override
	public void die(){
		super.die();
		a = null;
		b = null;
	}
	@Override
	public boolean isSignificant(){
		return !a.equals(b);
	}
	@Override
	@SuppressWarnings("nls")
	public String getPresentationName() {
		return "rename "+Tag.getTypeName(node.getTypeCode());
	}
}
