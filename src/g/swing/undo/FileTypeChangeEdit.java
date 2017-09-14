package g.swing.undo;

import g.io.FileNode;

public class FileTypeChangeEdit extends ValueEdit{
	int a,b;
	public FileTypeChangeEdit(FileNode node0,Object a,Object b,int sa,int sb) {
		super(node0,a,b);
		a = sa;
		b = sb;
	}
	public void undo(){
		super.undo();
		((FileNode)node).setState(a);
	}
	public void redo(){
		super.redo();
		((FileNode)node).setState(b);
	}
	@Override
	public String getPresentationName() {
		return "edit file type";
	}
	public boolean isSignificant(){
		return a != b || super.isSignificant();
	}
	public String toString(){
		return (node == null ? "dead filetype edit " : "filetype edit "+node.toString()) + "a = "+a+" b = "+b;
	}
}
