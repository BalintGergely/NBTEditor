package net.balintgergely.nbt.editor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Objects;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

import javax.swing.Icon;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.TreePath;

import net.balintgergely.nbt.Compound;
import net.balintgergely.nbt.NbtIO;
import net.balintgergely.nbt.Region;
import net.balintgergely.nbt.Region.Chunk;
import net.balintgergely.nbt.Tag;

public class FileTreeNode<E> extends Tag<E>{
	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_DIRECTORY = 1;
	public static final int TYPE_UNRESOLVED = 2;
	public static final int TYPE_REGION = 3;
	public static final int TYPE_NBT = 4;
	public static final int TYPE_DEFLATE_NBT = 5;
	public static final int TYPE_GZIP_NBT = 6;
	public final TreePath path;
	private File file;
	private Icon icon;
	private boolean modified = false;
	public void setModFlag(){
		if(recruit() != null){
			modified = true;
		}
	}
	public boolean getModFlag(){
		return modified;
	}
	@SuppressWarnings("hiding")
	private int TYPE = TYPE_UNKNOWN;
	public File getFile(){
		return file;
	}
	public FileTreeNode(File file0){
		if(!(this instanceof RemappableFileTreeNode)){
			Objects.requireNonNull(file0);
		}
		file = file0;
		setIcon(null);
		path = new TreePath(this);
	}
	private FileTreeNode(File file0,TreePath parentPath){
		file = Objects.requireNonNull(file0);
		setIcon(null);
		path = parentPath.pathByAddingChild(this);
	}
	public void setIcon(Icon ic){
		if(ic == null){
			if(file != null){
				icon = FileSystemView.getFileSystemView().getSystemIcon(file);
			}
		}else{
			icon = ic;
		}
	}
	public Icon getIcon(){
		return icon;
	}
	public boolean isProcessed(){
		return TYPE != TYPE_UNKNOWN;
	}
	public int getType(){
		return TYPE;
	}
	public int setType(int type){
		int ot = TYPE;
		if( (type == TYPE_NBT || type == TYPE_DEFLATE_NBT || type == TYPE_GZIP_NBT) &&
			(TYPE == TYPE_NBT || TYPE == TYPE_DEFLATE_NBT || TYPE == TYPE_GZIP_NBT)){
			TYPE = type;
			return ot;
		}else{
			throw new IllegalStateException();
		}
	}
	public boolean hasLoadedNBTData(){
		return getValue() instanceof Compound;
	}
	@Override
	@SuppressWarnings("unchecked")
	public E getValue(){
		E val = value;
		return val instanceof Reference ? ((Reference<E>)val).get() : val;
	}
	@Override
	@SuppressWarnings("unchecked")
	public synchronized E setValue(E val){
		E old = value;
		value = val;
		return old instanceof Reference ? ((Reference<E>)old).get() : old;
	}
	public synchronized void discard(){
		modified = false;
		E val = setValue(null);
		setIcon(null);
		TYPE = TYPE_UNKNOWN;
		if(val instanceof Region){
			try {
				((Region)val).close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else if(val instanceof FileTreeNode[]){
			for(FileTreeNode<?> n : ((FileTreeNode[])val)){
				n.discard();
			}
		}
	}
	@SuppressWarnings("unchecked")
	public synchronized void dismiss(){
		if(!modified && value != null && !(value instanceof Reference)){
			value = (E)new SoftReference<E>(value);
		}
	}
	@SuppressWarnings("unchecked")
	public synchronized E recruit(){
		if(value instanceof Reference){
			value = ((Reference<E>)value).get();
		}
		return value;
	}
	/**
	 * A quick attempt at pinning down what this file has.
	 * This method will only scan directories.
	 */
	@SuppressWarnings("unchecked")
	public synchronized E scan(){
		if(recruit() != null){
			return value;
		}
		if(file.isDirectory()){
			TYPE = TYPE_DIRECTORY;
			File[] files = file.listFiles();
			FileTreeNode<?>[] ch = new FileTreeNode<?>[files.length];
			int i = 0;
			while(i < ch.length){
				ch[i] = new FileTreeNode<>(files[i],path);
				i++;
			}
			value = (E)ch;
		}
		return value;
	}
	/**
	 * @return True if this FileTreeNode changed as a result of this invocation
	 */
	public synchronized boolean load(){
		E v = scan();
		if(v != null){
			return false;
		}
		if(file.isFile()){
			return tryLoad();
		}else{//Unlikely... but just to be sure, I place this here.
			throw new UncheckedIOException(new FileNotFoundException(file.toString()));
		}
	}
	public synchronized boolean save(){
		if(file.isDirectory()){
			if(value != null){
				for(FileTreeNode<?> node : (FileTreeNode<?>[])value){
					node.save();
				}
			}
			return true;
		}
		E val = recruit();
		if(val == null){
			modified = false;
			return true;
		}
		try{
			if(TYPE == TYPE_NBT || TYPE == TYPE_GZIP_NBT || TYPE == TYPE_DEFLATE_NBT){
				Compound com = (Compound)val;
				OutputStream out = new FileOutputStream(file);
				switch(TYPE){
				case TYPE_GZIP_NBT:out = new GZIPOutputStream(out);break;
				case TYPE_DEFLATE_NBT:out = new DeflaterOutputStream(out);break;
				}
				NbtIO.write(out, com);
				out.close();
				modified = false;
				return true;
			}
			if(TYPE == TYPE_REGION){
				for(Chunk ch : (Region)val){
					ch.save();
				}
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		return false;
	}
	@SuppressWarnings("unchecked")
	private boolean tryLoad(){
		/* First, we have to determine which format we are dealing with.
		 * but some formats are easier to figure out.
		 * Order of formats to check: (Note, check region first, if filesize is at least 0x2000 and is divisible by 0x1000)
		 * GZIP
		 * NBT
		 * DEFLATE
		 * REGION
		 */
		long size = file.length();
		boolean dr;
		if(dr = (size%0x1000 == 0 && size >= 0x2000)){//Has chance to be a region.
			if(tryLoadRegion()){
				return true;
			}
		}
		try(FileInputStream mfs = new FileInputStream(file)){
			mfs.getChannel();
			NbtIO nio = null,ngzip = null,nbt = null,nInf = null;
			a:{
				ngzip = NbtIO.read(new GZIPInputStream(mfs),-1);
				if(ngzip != null && ngzip.integrity >= 0){
					TYPE = TYPE_GZIP_NBT;
					nio = ngzip;
					break a;
				}
				mfs.getChannel().position(0);
				nbt = NbtIO.read(mfs,-1);
				if(nbt.readResult != null && nbt.compareTo(ngzip) > 0){
					TYPE = TYPE_NBT;
					nio = nbt;
					break a;
				}
				mfs.getChannel().position(0);
				nInf = NbtIO.read(new InflaterInputStream(mfs),-1);
				if(nInf.readResult != null && nInf.compareTo(ngzip) > 0){
					TYPE = TYPE_DEFLATE_NBT;
					nio = nInf;
					break a;
				}
			}
			if(nio != null && nio.readResult != null){
				value = (E)nio.readResult;
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(!dr){
			return tryLoadRegion();
		}
		TYPE = TYPE_UNRESOLVED;
		return false;
	}
	@SuppressWarnings({ "unchecked" })
	private boolean tryLoadRegion(){
		try {
			value = (E)new Region(file,true);
			TYPE = TYPE_REGION;
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	@Override
	public String toString(){
		E element = getValue();
		String str = file != null ? file.getName() : "";
		if(element != null){
			if(element instanceof Compound){
				int s = ((Compound)element).size();
				str += ": "+(s == 0 ? "no" : s) + (s == 1 ? " tag" : " tags");
				switch(TYPE){
				case TYPE_NBT:str += " (uncompressed)";break;
				case TYPE_DEFLATE_NBT:str += " (zlib)";break;
				case TYPE_GZIP_NBT:str += " (gzip)";break;
				}
			}else if(element instanceof Region){
				str += ": "+element.toString();
			}
		}
		return str;
	}
	public static class RemappableFileTreeNode<E> extends FileTreeNode<E>{
		public RemappableFileTreeNode(File file0) {
			super(file0);
			super.TYPE = TYPE_NBT;
		}
		public synchronized void setFile(File f){
			super.file = f;
		}
		/**
		 * The dismiss method is overridden by this class, because it is for single file editing mode.
		 */
		@Override
		public void dismiss(){}
	}
}
