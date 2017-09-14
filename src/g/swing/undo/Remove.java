package g.swing.undo;

import javax.swing.tree.MutableTreeNode;

import g.nbt.Tag;

public class Remove extends AbstractTagEdit{
	private int index;
	public Remove(Tag node1,int index1) {
		super(node1);
		index = index1;
	}
	@Override
	public void undo(){
		super.undo();
		((MutableTreeNode)node.getParent()).insert(node, index);
	}
	@Override
	public void redo(){
		super.redo();
		node.removeFromParent();
	}
	@Override
	@SuppressWarnings("nls")
	public String getPresentationName(){
		return "remove "+Tag.getTypeName(node.getTypeCode());
	}
}
