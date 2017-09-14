package g.io;

import java.io.File;

import javax.swing.tree.TreeNode;
/**
 * RemappableFileNode serves as the root in single file editing mode.
 * Only loads during constructor call, saves as usual.
 * @author Gergely Bálint
 */
public class RemappableFileNode extends FileNode{
	public RemappableFileNode(File file0) {
		super(file0);
		super.scan();
		super.load();
	}
	public RemappableFileNode(String name,int typeCode0){
		super(name,typeCode0);
	}
	public RemappableFileNode(String name,Object value0){
		super(name,value0);
	}
	public synchronized void setFile(File file0){
		file = file0;
	}
	@Override
	@SuppressWarnings("sync-override")
	public int scan(){
		//Do nothing
		return getTypeCode();
	}
	@Override
	@SuppressWarnings("sync-override")
	public boolean load(){
		return true;
	}
	@Override
	public void setParent(TreeNode parent){
		throw new UnsupportedOperationException();
	}
	@Override
	public String toString(){
		return super.toTagString();
	}
}
