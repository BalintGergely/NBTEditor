package g.io;

import java.io.File;

import javax.swing.tree.TreeNode;
/**
 * I wonder where I would be without this interface...
 * @author Gergely B�lint
 *
 */
public interface BaseFileNode extends TreeNode{
	public void setSignificant();
	public boolean isSignificant();
	public File getFile();
	public boolean saveWave();
}
