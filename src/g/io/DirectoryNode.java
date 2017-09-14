package g.io;

import java.io.File;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;

import javax.swing.tree.TreeNode;

import g.util.ArrayIterator;

public class DirectoryNode implements BaseFileNode{
	public final File file;
	private BaseFileNode[] children;
	private SoftReference<BaseFileNode[]> childrenWk;
	private WeakReference<DirectoryNode> parent;
	private boolean significant;
	public DirectoryNode(File file0){
		file = Objects.requireNonNull(file0);
	}
	DirectoryNode(File file0,DirectoryNode parent0){
		this(file0);
		parent = new WeakReference<>(parent0);
	}
	private synchronized BaseFileNode[] restoreChildren(){
		if(childrenWk != null){
			BaseFileNode[] ch = childrenWk.get();
			if(ch != null){
				children = ch;
			}
		}
		return children;
	}
	public synchronized boolean ensureChildren(){
		if(restoreChildren() == null){
			File[] files = file.listFiles();
			if(files == null){
				children = new BaseFileNode[0];
			}else{
				Arrays.sort(files);
				children = new BaseFileNode[files.length];
				int i = 0;
				for(File f : files){
					children[i] = f.isDirectory() ? new DirectoryNode(f,this) : new FileNode(f,this);
					i++;
				}
			}
			childrenWk = new SoftReference<>(children);
			return true;
		}
		return false;
	}
	public synchronized void weakChildren(){
		if(!significant){
			children = null;
		}
	}
	@Override
	public synchronized boolean saveWave(){
		if(!significant){
			return true;
		}
		boolean sign = true;
		for(BaseFileNode node : children){
			if(!node.saveWave()){
				sign = false;
			}
		}
		if(sign){
			significant = false;
		}
		return sign;
	}
	@Override
	public TreeNode getChildAt(int childIndex) {
		ensureChildren();
		return children[childIndex];
	}
	@Override
	public int getChildCount() {
		ensureChildren();
		return children.length;
	}
	@Override
	public DirectoryNode getParent() {
		if(parent != null){
			return parent.get();
		}
		return null;
	}
	@Override
	public int getIndex(TreeNode node) {
		ensureChildren();
		int i = 0;
		while(i < children.length){
			if(children[i] == node){
				return i;
			}
			i++;
		}
		return -1;
	}
	@Override
	public boolean getAllowsChildren() {
		return file.isDirectory();
	}
	@Override
	public boolean isLeaf() {
		return false;
	}
	@Override
	public Enumeration<BaseFileNode> children() {
		ensureChildren();
		return new ArrayIterator<>(children);
	}
	@Override
	public void setSignificant() {
		significant = true;
		ensureChildren();
		DirectoryNode parent0 = getParent();
		if(parent0 != null){
			parent0.setSignificant();
		}
	}
	@Override
	public boolean isSignificant() {
		return significant;
	}
	@Override
	public File getFile() {
		return file;
	}
	@Override
	public String toString(){
		return file.getName();
	}
}
