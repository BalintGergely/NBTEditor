package g.nbt;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import g.io.FileNode;

public class Region extends TagCollection<Chunk>{
	public Region(FileNode fileNode) {
		super(fileNode, Chunk.class);
		super.ensureCapacity(1024);
	}
	public Region(FileNode parent0,FileInputStream fileIn) throws IOException{
		this(parent0);
		FileChannel ch = fileIn.getChannel();
		long s = ch.size();
		if(s % 0x1000 != 0 || s < 0x2000){
			throw new IOException();
		}
		int i = 0;
		ByteBuffer buf = ByteBuffer.allocate(8);
		while(i < 1024){
			Chunk node = Chunk.read(i, fileIn, buf);
			if(node != null){
				super.add(node);
			}
			i++;
		}
	}
	@Override
	@SuppressWarnings("nls")
	public String toString(){
		return super.size()+" chunks";
	}
	public Chunk getbyId(int chunkId){
		for(Chunk node : this){
			if(node.chunkId >= chunkId){
				return node.chunkId == chunkId ? node : null;
			}
		}
		return null;
	}
	public void swap(int a,int b){
		
	}
	public void sort(){
		super.sort((Chunk a, Chunk b) -> Integer.compare(a.chunkId,b.chunkId));
	}
	public void remove(Chunk node){
		super.remove(node);
		sort();
	}
	public Chunk remove(int index){
		Chunk node = super.remove(index);
		sort();
		return node;
	}
	@Override
	public void safeCheck(){
		for(Chunk node : this){
			node.safeCheck();
		}
	}
	public Chunk createChunk(String name,int index){
		int chunkId = index > 0 ? get(index-1).chunkId : 0;
		for(Chunk node : this){
			if(chunkId == node.chunkId){
				chunkId++;
			}else if(node.chunkId > chunkId){
				break;
			}
		}
		if(chunkId >= 0x1000){
			if(index > 0){
				return createChunk(name,index-1);
			}
			return null;
		}
		Chunk node = new Chunk(chunkId,name);
		return node;
	}
	public Chunk createChunk(String name,int type,int index){
		int chunkId = index > 0 ? get(index-1).chunkId : 0;
		for(Chunk node : this){
			if(chunkId == node.chunkId){
				chunkId++;
			}else if(node.chunkId > chunkId){
				break;
			}
		}
		if(chunkId >= 0x1000){
			if(index > 0){
				return createChunk(name,type,index-1);
			}
			return null;
		}
		Chunk node = new Chunk(chunkId,name,type);
		add(node);
		return node;
	}
	public boolean add(Chunk element){
		int index = indexOf((Chunk nod) -> nod.chunkId > element.chunkId);
		return super.add(index < 0 ? size() : index, element);
	}
	public boolean add(int index,Chunk element){
		return add(element);
	}
	@Override
	public void writeOut(DataOutputStream dataOut) throws IOException {
		sort();
		int openChunks = 0,c = 0,pos = 2;
		try{
			while(openChunks < size()){
				Chunk node = get(openChunks++);
				while(c < node.chunkId){
					dataOut.writeInt(0);
					c++;
				}
				int len = node.initWrite();
				if(len == 0){
					continue;
				}
				dataOut.writeInt((pos << 8) | len);
				pos += len;
				c++;
			}
			while(c < 1024){
				dataOut.writeInt(0);
				c++;
			}
			c = 0;
			for(Chunk node : this){
				if(node.content.getSize() == 0){
					continue;
				}
				while(c < node.chunkId){
					dataOut.writeInt(0);
					c++;
				}
				dataOut.writeInt(node.timestamp);
				c++;
			}
			while(c < 1024){
				dataOut.writeInt(0);
				c++;
			}
			for(Chunk node : this){
				node.write(dataOut);
			}
		}finally{
			while(openChunks > 0){
				Chunk node = get(--openChunks);
				node.content = null;
			}
		}
	}
	public TagCompound convertToCompound(){
		sort();
		TagCompound comp = new TagCompound(parent);
		comp.ensureCapacity(size());
		for(Chunk node : this){
			comp.add((NamedTag)super.cloneTag(node));
		}
		return comp;
	}
}