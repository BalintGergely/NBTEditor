package g.swing.undo;

import g.nbt.Chunk;

public class ChunkIdEdit extends AbstractTagEdit{
	int a,b;
	public ChunkIdEdit(Chunk node0,int a0,int b0) {
		super(node0);
		a = a0;
		b = b0;
	}
	@Override
	public String getPresentationName() {
		return "edit chunk position";
	}
	public void undo(){
		super.undo();
		((Chunk)node).setChunkId(a);
	}
	public void redo(){
		super.redo();
		((Chunk)node).setChunkId(b);
	}
	public void die(){
		super.die();
	}
	public boolean isSignificant(){
		return a != b;
	}
}
