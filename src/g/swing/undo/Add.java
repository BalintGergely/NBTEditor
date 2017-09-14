package g.swing.undo;

import javax.swing.tree.MutableTreeNode;

import g.nbt.Tag;

public class Add extends AbstractTagEdit{
	private int index;
	public Add(Tag node1,int index1) {
		super(node1);
		index = index1;
	}
	@Override
	public void undo(){
		super.undo();
		node.removeFromParent();
	}
	@Override
	public void redo(){
		super.redo();
		((MutableTreeNode)node.getParent()).insert(node, index);
	}
	@Override
	@SuppressWarnings("nls")
	public String getPresentationName(){
		return "add "+Tag.getTypeName(node.getTypeCode());
	}
	public String toString(){
		return node == null ? "dead add edit" : "add "+node.toString();
	}
}
