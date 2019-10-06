package net.balintgergely.nbt.editor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

import javax.swing.tree.TreePath;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import net.balintgergely.nbt.NamedTag;
import net.balintgergely.nbt.Region.Chunk;
import net.balintgergely.nbt.StringUTF8;
import net.balintgergely.nbt.Tag;
import net.balintgergely.nbt.editor.FileTreeNode.RemappableFileTreeNode;
import net.balintgergely.nbt.Container;

abstract class TagEditState<T extends Tag<?>> implements UndoableEdit{
	T tag;
	private boolean undone;
	private TreePath cachePath;
	public TreePath getCachePath() {
		return cachePath;
	}
	public void setCachePath(TreePath cachePath) {
		this.cachePath = cachePath;
	}
	public T getTag(){
		return tag;
	}
	private TagEditState(T tag0) {
		tag = Objects.requireNonNull(tag0);
	}
	@SuppressWarnings("unchecked")
	public static <T extends Tag<?>> TagEditState<T> createEditState(T tag){
		Class<? extends T> clasz = (Class<? extends T>) tag.getClass();
		if(clasz == Tag.class){
			return new ListEntryEditState<>(tag);
		}else if(clasz == NamedTag.class){
			return (TagEditState<T>)new NamedTagEditState<NamedTag<?>>((NamedTag<?>)tag);
		}else if(clasz == FileTreeNode.class || clasz == RemappableFileTreeNode.class){
			return (TagEditState<T>)new FileTagEditState<FileTreeNode<?>>((FileTreeNode<?>)tag);
		}else if(clasz == Chunk.class){
			return (TagEditState<T>)new ChunkEditState<Chunk>((Chunk)tag);
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	public static <T extends Tag<?>> TagAREdit<T> createDeleteEdit(TreePath path){
		Object tag = path.getLastPathComponent();
		if(!(tag instanceof Tag)){
			return null;
		}
		TreePath parentPath = path.getParentPath();
		if(parentPath == null){
			return null;
		}
		Object parent = parentPath.getLastPathComponent();
		if(!(parent instanceof Tag)){
			return null;
		}
		Object cont = ((Tag<?>)parent).getValue();
		if(!(cont instanceof Container)){
			return null;
		}
		return new ListEntryAREdit<>((T)tag, (Container<T>)cont, path);
	}
	@SuppressWarnings("unchecked")
	public static <T extends Tag<?>> TagAREdit<T> createAddEdit(TreePath path,int index){
		T tag = (T)path.getLastPathComponent();
		TreePath parentPath = path.getParentPath();
		Tag<? extends Container<T>> parent = (Tag<? extends Container<T>>)parentPath.getLastPathComponent();
		Container<T> t = parent.getValue();
		return new ListEntryAREdit<>(tag, t, path, index);
	}
	@SuppressWarnings("unchecked")
	public static <T extends Tag<?>> TagIndexEdit<T> createMoveEdit(TreePath path,boolean up){
		T tag = (T)path.getLastPathComponent();
		TreePath parentPath = path.getParentPath();
		Tag<? extends Container<T>> parent = (Tag<? extends Container<T>>)parentPath.getLastPathComponent();
		Object o = parent.getValue();
		if(o instanceof Container){
			Container<T> t = (Container<T>)o;
			int from = t.indexOf(tag),to = up ? from-1 : from+1;
			if(to >= t.size() || to < 0 || from < 0){
				return null;
			}
			return new TagIndexEdit<T>(t, tag, path, from, to);
		}
		return null;
	}
	void setEx(Object ex0){
		throw new UnsupportedOperationException();
	}
	<E> E getValue(){
		throw new UnsupportedOperationException();
	}
	String getName(){
		throw new UnsupportedOperationException();
	}
	void setName(String name0){
		throw new UnsupportedOperationException();
	}
	void setFileType(int type){
		throw new UnsupportedOperationException();
	}
	int getFileType(){
		throw new UnsupportedOperationException();
	}
	boolean setX(int x0){
		throw new UnsupportedOperationException();
	}
	boolean setZ(int z0){
		throw new UnsupportedOperationException();
	}
	public boolean isAddition(){
		throw new UnsupportedOperationException();
	}
	public int getIndexA(){
		throw new UnsupportedOperationException();
	}
	public int getIndexB(){
		throw new UnsupportedOperationException();
	}
	public Container<T> getContainer(){
		throw new UnsupportedOperationException();
	}
	/**
	 * @return
	 * <li>0 if nothing has changed
	 * <li>1 if good to go
	 * <li>-1 if not good to go
	 */
	abstract byte sanitize();
	abstract void swap() throws Exception;
	@Override
	public void undo() throws CannotUndoException {
		if(canUndo()){
			try{
				swap();
			}catch(Exception e){
				throw new CannotRedoException();
			}
			undone = true;
		}else{
			throw new CannotUndoException();
		}
	}
	@Override
	public boolean canUndo() {
		return !(undone || tag == null);
	}
	@Override
	public void redo() throws CannotRedoException {
		if(canRedo()){
			try{
				swap();
			}catch(Exception e){
				throw new CannotRedoException();
			}
			undone = false;
		}else{
			throw new CannotRedoException();
		}
	}
	@Override
	public boolean canRedo() {
		return undone && tag != null;
	}
	public boolean undone(){
		return undone;
	}
	@Override
	public void die() {
		tag = null;
	}
	@Override
	public boolean addEdit(UndoableEdit anEdit) {
		return false;
	}
	@Override
	public boolean replaceEdit(UndoableEdit anEdit) {
		return false;
	}
	@Override
	public boolean isSignificant() {
		return true;
	}
	@Override
	public String getUndoPresentationName() {
		return "Undo "+getPresentationName();
	}
	@Override
	public String getRedoPresentationName() {
		return "Redo "+getPresentationName();
	}
	public static class ListEntryEditState<T extends Tag<?>> extends TagEditState<T>{
		Object ex;
		private ListEntryEditState(T tag0){
			super(tag0);
			ex = this;
		}
		@Override
		void setEx(Object ex0){
			ex = ex0;
		}
		@Override
		@SuppressWarnings("unchecked")
		<E> E getValue(){
			if(ex == this){
				return (E) tag.getValue();
			}
			return (E) ex;
		}
		@Override
		@SuppressWarnings("unchecked")
		byte sanitize(){
			if(ex == this){
				return 0;
			}
			if(ex != null){
				ex = tag.TYPE.approve(ex);
			}
			if(ex == null){
				return -1;
			}
			if(tag.TYPE.equals(ex,tag.getValue())){
				return 0;
			}
			ex = ((Tag<Object>)tag).setValue(ex);
			return 1;
		}
		@Override
		@SuppressWarnings("unchecked")
		void swap(){
			ex = ((Tag<Object>)tag).setValue(ex);
		}
		@Override
		public String getPresentationName() {
			return "edit tag value";
		}
	}
	public static class NamedTagEditState<T extends NamedTag<?>> extends ListEntryEditState<T>{
		private CharSequence name;
		private NamedTagEditState(T tag0) {
			super(tag0);
		}
		@Override
		void setName(String name0){
			name = name0;
		}
		@Override
		String getName(){
			if(name == null){
				return tag.getName().toString();
			}
			return name.toString();
		}
		@SuppressWarnings("unchecked")
		@Override
		void swap(){
			if(ex != null){
				ex = ((Tag<Object>)tag).setValue(ex);
			}
			if(name != null){
				name = tag.setName(name);
			}
		}
		@Override
		byte sanitize(){
			if(ex != this){
				if(ex != null){
					ex = tag.TYPE.approve(ex);
				}
				if(ex == null){
					return -1;
				}
			}
			CharSequence str = name;
			if(str != null){
				if(tag.getName().equals(str)){
					name = null;
				}else{
					if(!StringUTF8.isValid(str)){
						return -1;
					}
					name = StringUTF8.valueOf(str);
				}
			}
			if(ex == this || (ex != null && tag.TYPE.equals(ex,tag.getValue()))){
				ex = null;
			}
			if(ex == null && name == null){
				return 0;
			}
			swap();
			return 1;
		}
		@Override
		public String getPresentationName(){
			if(name == null){
				return super.getPresentationName();
			}
			if(ex != null){
				return "edit tag name and value";
			}else{
				return "edit tag name";
			}
		}
	}
	public static class TagIndexEdit<T extends Tag<?>> extends TagEditState<T>{
		Container<T> c;
		int a,b;
		private TagIndexEdit(Container<T> c,T tag,TreePath pt,int a,int b){
			super(tag);
			this.a = a;
			this.b = b;
			this.c = c;
			super.setCachePath(pt);
		}
		@Override
		public int getIndexA(){
			return a;
		}
		@Override
		public int getIndexB(){
			return b;
		}
		@Override
		public Container<T> getContainer(){
			return c;
		}
		@Override
		public byte sanitize(){
			if(c.indexOf(tag) != a){
				return -1;
			}
			if(a < 0 || b < 0 || a >= c.size() || b >= c.size()){
				return -1;
			}
			if(a == b){
				return 0;
			}
			c.set(a, c.set(b, tag));
			return 1;
		}
		@Override
		public void setCachePath(TreePath pt){}
		@Override
		public boolean isSignificant(){
			return false;
		}
		@Override
		void swap() throws Exception {
			c.set(a, c.set(b, c.get(a)));
		}
		@Override
		public String getPresentationName(){
			return a > b ? "move tag down" : "move tag up";
		}
	}
	public static abstract class TagAREdit<T extends Tag<?>> extends TagEditState<T>{
		int index;
		boolean addition;
		private TagAREdit(T tag0,TreePath path,boolean add) {
			super(tag0);
			super.setCachePath(path);
			addition = add;
		}
		@Override
		public int getIndexA(){
			return index;
		}
		@Override
		public boolean isAddition(){
			return addition;
		}
		@Override
		public void setCachePath(TreePath pt){}
		@Override
		public String getPresentationName() {
			return addition ? "add tag" : "delete tag";
		}
	}
	public static class ListEntryAREdit<T extends Tag<?>> extends TagAREdit<T>{
		Container<T> container;
		private ListEntryAREdit(T tag0,Container<T> con,TreePath p){
			super(tag0,p,false);
			container = con;
		}
		private ListEntryAREdit(T tag0,Container<T> con,TreePath p,int in){
			super(tag0,p,true);
			container = con;
			index = in;
		}
		@Override
		byte sanitize(){
			if(addition){
				container.add(index, tag);
				return 1;
			}else{
				index = container.indexOf(tag);
				if(index < 0){
					return -1;
				}
				container.remove(index);
				return 1;
			}
		}
		@Override
		void swap(){
			if(undone() == addition){
				container.add(index, tag);
			}else{
				container.remove(index);
			}
		}
	}
	public static class FileTagEditState<T extends FileTreeNode<?>> extends ListEntryEditState<T>{
		private static int[] STATE_LINK = {FileTreeNode.TYPE_NBT,FileTreeNode.TYPE_DEFLATE_NBT,FileTreeNode.TYPE_GZIP_NBT};
		private static int toStateLink(int fileType){
			int a = 0;
			while(a < STATE_LINK.length){
				if(STATE_LINK[a] == fileType){
					return a;
				}
				a++;
			}
			return -1;
		}
		private int state = -1;
		private FileTagEditState(T tag0) {
			super(tag0);
		}
		@Override
		void setFileType(int type){
			state = type;
		}
		@Override
		int getFileType(){
			if(state >= 0){
				return state;
			}
			return toStateLink(tag.getType());
		}
		@Override
		@SuppressWarnings("unchecked")
		void swap(){
			ex = ((Tag<Object>)tag).setValue(ex);
			if(state >= 0){
				state = tag.setType(state);
			}
		}
		@Override
		byte sanitize(){
			byte b = super.sanitize();
			if(b >= 0 && state >= 0){
				state = STATE_LINK[state];
				if(state != tag.getType()){
					state = tag.setType(state);
					return 1;
				}
				state = -1;
			}
			return b;
		}
	}
	public static class ChunkEditState<T extends Chunk> extends TagEditState<T>{
		int x = -1,z = -1;
		private ChunkEditState(T tag0) {
			super(tag0);
		}
		@Override
		void swap() throws IOException {
			x = tag.swapIndex(x);
		}
		@Override
		byte sanitize(){
			if(x == -2 || z == -2){
				return -1;
			}
			if(x == -1){
				if(z != -1){
					x = tag.getX();
				}else{
					return 0;
				}
			}else if(z == -1){
				z = tag.getZ();
			}else if(tag.getX() == x && tag.getZ() == z){
				return 0;
			}
			x += (z * 32);
			try{
				swap();
			}catch(IOException e){
				throw new UncheckedIOException(e);
			}
			return 1;
		}
		@Override
		boolean setX(int x0){
			if(x0 < 0 || x0 >= 32){
				x = -2;
				return false;
			}
			x = x0;
			return true;
		}
		@Override
		boolean setZ(int z0){
			if(z0 < 0 || z0 >= 32){
				z = -2;
				return false;
			}
			z = z0;
			return true;
		}
		@Override
		public String getPresentationName() {
			return "edit chunk location";
		}
	}
}
