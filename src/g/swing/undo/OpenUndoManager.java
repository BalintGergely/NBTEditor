package g.swing.undo;

import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

public class OpenUndoManager extends UndoManager{
	private static final long serialVersionUID = 1L;
	public UndoableEdit editToBeUndone(){
		return super.editToBeUndone();
	}
	public UndoableEdit editToBeRedone(){
		return super.editToBeRedone();
	}
	public int numberOfEdits(){
		return super.edits.size();
	}
}
