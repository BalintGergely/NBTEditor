package g.swing;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * I just love how much of a mess I have created because I didn't realize that I could implement
 * my own TreeModel
 * @author Gergely Bálint
 *
 */
public class TagTreeModel extends DefaultTreeModel{
	private static final long serialVersionUID = 1L;
	public TagTreeModel() {
		super(null,true);
	}
	@Override
	public void valueForPathChanged(TreePath path,Object value){
		nodeChanged((TreeNode)path.getLastPathComponent());
	}
}
