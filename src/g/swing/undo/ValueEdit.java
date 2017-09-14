package g.swing.undo;

import g.nbt.Tag;

public class ValueEdit extends AbstractTagEdit{
	private Object a,b;
	public ValueEdit(Tag node1,Object a1,Object b1) {
		super(node1);
		a = a1;
		b = b1;
	}
	@Override
	public void undo(){
		super.undo();
		node.setValue(a);
	}
	@Override
	public void redo(){
		super.redo();
		node.setValue(b);
	}
	@Override
	public void die(){
		super.die();
		a = null;
		b = null;
	}
	@Override
	@SuppressWarnings("nls")
	public String getPresentationName(){
		return "edit "+Tag.getTypeName(node.getTypeCode());
	}
	/**
	 * Yes, if it actually DOES change something.
	 */
	@Override
	public boolean isSignificant(){
		return !a.equals(b);
	}
}
