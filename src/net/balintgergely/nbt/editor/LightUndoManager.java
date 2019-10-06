package net.balintgergely.nbt.editor;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

public class LightUndoManager{
	UndoableEdit[] array;
	int off,len,pos;//len: index of next insert pos: position of the edit to be redone
	private UndoableEdit get(int index){
		return array[(off+index)%array.length];
	}
	private void set(int index,UndoableEdit edt){
		array[(off+index)%array.length] = edt;
	}
	private void clear(int index){
		index = (off+index)%array.length;
		UndoableEdit edt = array[index];
		array[index] = null;
		edt.die();
	}
	private void rotate(int offset){
		off = (off+offset)%array.length;
	}
	public LightUndoManager(int capacity) {
		array = new UndoableEdit[capacity];
	}
	public boolean addEdit(UndoableEdit edit){
		removeAllUndoneEdits();
		UndoableEdit catalyst = get(len);
		if(catalyst == null || catalyst.isSignificant() || !catalyst.addEdit(edit)){
			if(len == array.length){
				clear(0);
				set(0,edit);
				rotate(1);
			}else{
				set(len,edit);
				pos = ++len;
			}
		}
		return true;
	}
	public void removeAllUndoneEdits(){
		while(len > pos){
			--len;
			clear(len);
		}
	}
	public void removeAllDoneEdits(){
		while(pos != 0){
			clear(0);
			rotate(1);
			--pos;
			--len;
		}
	}
	public void removeAllEdits(){
		while(len > 0){
			--len;
			clear(len);
		}
		pos = 0;
		off = 0;
	}
	public UndoableEdit undo(){
		if(pos <= 0){
			throw new CannotUndoException();
		}
		UndoableEdit ed = get(pos-1);
		ed.undo();
		--pos;
		if(!ed.canRedo()){
			removeAllUndoneEdits();
		}
		return ed;
	}
	public UndoableEdit redo(){
		if(pos >= len){
			throw new CannotRedoException();
		}
		UndoableEdit ed = get(pos);
		ed.redo();
		++pos;
		if(!ed.canUndo()){
			removeAllDoneEdits();
		}
		return ed;
	}
	public boolean canUndo(){
		return pos > 0 && get(pos-1).canUndo();
	}
	public boolean canRedo(){
		return pos < len && get(pos).canRedo();
	}
	public String getUndoPresentationName() {
		return pos > 0 ? get(pos-1).getUndoPresentationName() : "Undo";
	}
	public String getRedoPresentationName() {
		return pos < len ? get(pos).getRedoPresentationName() : "Redo";
	}
}
