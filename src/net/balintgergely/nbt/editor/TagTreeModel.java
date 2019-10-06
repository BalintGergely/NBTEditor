package net.balintgergely.nbt.editor;

import java.awt.EventQueue;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import net.balintgergely.nbt.Region;
import net.balintgergely.nbt.Region.Chunk;
import net.balintgergely.nbt.Tag;

class TagTreeModel implements TreeModel{
	private Vector<TreeModelListener> listenerList = new Vector<>();
	private FileTreeNode<?> root;
	TagTreeModel() {}
	public void setRoot(FileTreeNode<?> root0) {
		root = root0;
		treeStructureChanged(new TreeModelEvent(this, root0.path));
	}
	@Override
	public FileTreeNode<?> getRoot() {
		return root;
	}
	@Override
	public Object getChild(Object parent, int index) {
		if(parent instanceof Tag<?>){
			parent = ((Tag<?>)parent).getValue();
		}
		if(parent instanceof List){
			return ((List<?>)parent).get(index);
		}
		if(parent instanceof Region){
			Iterator<Chunk> chunk = ((Region)parent).iterator();
			while(index > 0){
				chunk.next();
				index--;
			}
			return chunk.next();
		}
		if(parent.getClass().isArray()){
			return Array.get(parent, index);
		}
		return null;
	}

	@Override
	public int getChildCount(Object parent) {
		if(parent instanceof FileTreeNode){
			FileTreeNode<?> pr = (FileTreeNode<?>)parent;
			scanMaybe(pr);
			parent = pr.getValue();
		}else if(parent instanceof Tag<?>){
			parent = ((Tag<?>)parent).getValue();
		}
		if(parent instanceof List){
			return ((List<?>)parent).size();
		}
		if(parent instanceof Region){
			return ((Region)parent).getChunkCount();
		}
		if(parent instanceof Object[]){
			return Array.getLength(parent);
		}
		return 0;
	}
	@Override
	public boolean isLeaf(Object node) {
		if(node instanceof FileTreeNode){
			FileTreeNode<?> pr = (FileTreeNode<?>)node;
			scanMaybe(pr);
			node = pr.getValue();
		}else if(node instanceof Tag<?>){
			node = ((Tag<?>)node).getValue();
		}
		return node == null || !(node instanceof List || node instanceof Region || node instanceof Object[]);
	}
	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		if(Boolean.TRUE.equals(newValue)){
			treeStructureChanged(new TreeModelEvent(this, path.getParentPath() == null ? path : path.getParentPath()));
		}else{
			treeNodesChanged(new TreeModelEvent(this, path));
		}
	}
	public void valueForPathRemoved(TreePath path,int index){
		treeNodesRemoved(new TreeModelEvent(this, path.getParentPath(), new int[]{index}, new Object[]{path.getLastPathComponent()}));
	}
	public void valueForPathAdded(TreePath path,int index){
		treeNodesInserted(new TreeModelEvent(this, path.getParentPath(), new int[]{index}, new Object[]{path.getLastPathComponent()}));
	}
	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if(parent instanceof FileTreeNode){
			parent = ((FileTreeNode<?>)parent).getValue();
		}else if(parent instanceof Tag<?>){
			parent = ((Tag<?>)parent).getValue();
		}
		if(parent instanceof List){
			return ((List<?>)parent).indexOf(child);
		}
		if(parent instanceof Region){
			Iterator<Chunk> itr = ((Region)parent).iterator();
			int index = 0;
			while(itr.hasNext()){
				Chunk c = itr.next();
				if(child.equals(c)){
					return index;
				}
				index++;
			}
		}
		if(parent instanceof Object[]){
			int l = Array.getLength(parent);
			for(int c = 0;c < l;c++){
				if(child.equals(Array.get(parent, c))){
					return c;//That was hopelessly inefficient.
				}
			}
		}
		return -1;
	}
	public boolean containsChild(Object parent, Object child){
		if(parent instanceof FileTreeNode){
			parent = ((FileTreeNode<?>)parent).getValue();
		}else if(parent instanceof Tag<?>){
			parent = ((Tag<?>)parent).getValue();
		}
		if(parent instanceof List){
			return ((List<?>)parent).contains(child);
		}
		if(parent instanceof Region){
			return child instanceof Chunk && ((Chunk)child).getRegion() == parent;
		}
		if(parent instanceof Object[]){
			int l = Array.getLength(parent);
			for(int c = 0;c < l;c++){
				if(child.equals(Array.get(parent, c))){
					return true;
				}
			}
		}
		return false;
	}
	private void scanMaybe(FileTreeNode<?> node){
		if(!node.isProcessed()){
			if(node.scan() != null){
				EventQueue.invokeLater(() -> {
					treeNodesChanged(new TreeModelEvent(this, node.path));
				});
			}
		}
	}
	@Override
	public void addTreeModelListener(TreeModelListener l) {
		if(l != null){
			listenerList.add(l);
		}
	}
	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		listenerList.remove(l);
	}
	private void treeNodesChanged(TreeModelEvent e) {
		for(TreeModelListener ls : listenerList){
			ls.treeNodesChanged(e);
		}
	}
	private void treeNodesInserted(TreeModelEvent e) {
		for(TreeModelListener ls : listenerList){
			ls.treeNodesInserted(e);
		}
	}
	private void treeNodesRemoved(TreeModelEvent e) {
		for(TreeModelListener ls : listenerList){
			ls.treeNodesRemoved(e);
		}
	}
	void treeStructureChanged(TreeModelEvent e) {
		for(TreeModelListener ls : listenerList){
			ls.treeStructureChanged(e);
		}
	}
}
