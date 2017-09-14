package g.swing.undo;

import g.nbt.TagCollection;
import g.nbt.Tag;

public class TagSwapEdit extends AbstractTagEdit{
	int a,b;
	public TagSwapEdit(Tag node0,int a0,int b0) {
		super(node0);
		a = a0;
		b = b0;
	}
	public void undo(){
		super.undo();
		Object o = node.getValue();
		if(o instanceof TagCollection){
			((TagCollection<?>)o).swap(a,b);
		}
	}
	public void redo(){
		super.redo();
		Object o = node.getValue();
		if(o instanceof TagCollection){
			((TagCollection<?>)o).swap(a,b);
		}
	}
	@Override
	public String getPresentationName() {
		return "move tag "+(a < b ? "down" : "up"); 
	}
}
