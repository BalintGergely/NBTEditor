package g.swing.undo;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import g.nbt.Tag;

public abstract class AbstractTagEdit implements UndoableEdit{
	boolean hasBeenDone = true;
	protected Tag node;
	public AbstractTagEdit(Tag node0) {
		node = node0;
	}
	@Override
	public void die(){
		node = null;
	}
	public Tag getNode(){
		return node;
	}
	@Override
	public void undo() throws CannotUndoException {
		if(node == null || !hasBeenDone){
			throw new CannotUndoException();
		}
		hasBeenDone = false;
	}
	@Override
	public boolean canUndo() {
		return node != null && hasBeenDone;
	}
	@Override
	public void redo() throws CannotRedoException {
		if(node == null || hasBeenDone){
			throw new CannotUndoException();
		}
		hasBeenDone = true;
	}
	@Override
	public boolean canRedo() {
		return node != null && !hasBeenDone;
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
	@SuppressWarnings("nls")
	public String getUndoPresentationName(){
		return "Undo "+getPresentationName();
	}
	@Override
	@SuppressWarnings("nls")
	public String getRedoPresentationName(){
		return "Redo "+getPresentationName();
	}
}
