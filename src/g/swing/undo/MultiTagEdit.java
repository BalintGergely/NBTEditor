package g.swing.undo;

import java.util.Iterator;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import g.util.ArrayIterator;

public class MultiTagEdit implements UndoableEdit, Iterable<AbstractTagEdit>{
	AbstractTagEdit[] edits;
	private boolean isDone = true;
	public static UndoableEdit combine(AbstractTagEdit...edits){
		if(edits.length == 1){
			return edits[0];
		}
		return new MultiTagEdit(edits);
	}
	public MultiTagEdit(AbstractTagEdit...edits0){
		edits = edits0;
	}
	@Override
	public void undo() throws CannotUndoException {
		if(!canUndo()){
			throw new CannotUndoException();
		}
		int i = edits.length;
		while(i > 0){
			i--;
			edits[i].undo();
		}
		isDone = false;
	}

	@Override
	public boolean canUndo() {
		return isDone && edits != null;
	}

	@Override
	public void redo() throws CannotRedoException {
		if(!canRedo()){
			throw new CannotRedoException();
		}
		for(AbstractTagEdit e : edits){
			e.redo();
		}
		isDone = true;
	}

	@Override
	public boolean canRedo() {
		return !isDone && edits != null;
	}

	@Override
	public void die() {
		if(edits != null){
			for(AbstractTagEdit e : edits){
				e.die();
			}
		}
		edits = null;
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
		if(edits != null){
			for(AbstractTagEdit e : edits){
				if(e.isSignificant()){
					return true;
				}
			}
		}
		return false;
	}
	@Override
	public String getPresentationName() {
		return "Edit Tag";
	}
	@Override
	public String getUndoPresentationName() {
		return "Undo edit Tag";
	}
	@Override
	public String getRedoPresentationName() {
		return "Redo edit Tag";
	}
	public int size(){
		return edits.length;
	}
	@Override
	public Iterator<AbstractTagEdit> iterator() {
		return new ArrayIterator<>(edits);
	}

}
