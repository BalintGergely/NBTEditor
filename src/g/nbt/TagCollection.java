package g.nbt;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

import javax.swing.tree.TreeNode;

public abstract class TagCollection<E extends Tag> implements Collection<E>{
	private E[] elementData;
	private int size;
	public final Tag parent;
	@SuppressWarnings("unchecked")
	protected TagCollection(Tag parent0,Class<? super E> type){
		elementData = (E[])Array.newInstance(type, 0);
		parent = parent0;
	}
	public void ensureCapacity(int capacity){
		if(elementData.length < capacity){
			@SuppressWarnings("unchecked")
			E[] array = (E[])Array.newInstance(elementData.getClass().getComponentType(), capacity);
			System.arraycopy(elementData, 0, array, 0, size);
			elementData = array;
		}
	}
	public void trim(){
		if(elementData.length > size){
			elementData = Arrays.copyOf(elementData, size);
		}
	}
	@Override
	public int size() {
		return size;
	}
	protected void sort(Comparator<? super E> comp){
		Arrays.sort(elementData, 0, size, comp);
	}
	@Override
	public boolean isEmpty() {
		return size == 0;
	}
	public void swap(int a,int b){
		if(a >= size || b >= size){
			throw new IllegalArgumentException();
		}
		E ea = elementData[a],eb = elementData[b];
		elementData[a] = eb;
		elementData[b] = ea;
	}
	@Override
	public boolean contains(Object o) {
		int i = 0;
		while(i < size){
			if(o.equals(elementData[i])){
				return true;
			}
			i++;
		}
		return false;
	}
	@Override
	public Itr iterator() {
		return new Itr();
	}
	public class Itr implements Iterator<E>,Enumeration<E>{
		private Itr(){}
		private int index;
		private int latest;
		@Override
		public boolean hasNext() {
			return index < size;
		}
		@Override
		public E next() {
			if(index >= size){
				index = Integer.MAX_VALUE;
				throw new NoSuchElementException();
			}
			return elementData[latest = index++];
		}
		@Override
		public void remove(){
			if(latest < 0){
				throw new IllegalStateException();
			}
			TagCollection.this.remove(latest);
			index--;
			latest = -1;
		}
		@Override
		public boolean hasMoreElements() {
			return hasNext();
		}
		@Override
		public E nextElement() {
			return next();
		}

	}
	@Override
	public Object[] toArray() {
		Object[] o = new Object[size];
		System.arraycopy(elementData, 0, o, 0, size);
		return o;
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> T[] toArray(T[] a) {
		if(a.length < size){
			a = (T[])Array.newInstance(a.getClass().getComponentType(), size);
		}else if(a.length > size){
			a[size] = null;
		}
		System.arraycopy(elementData, 0, a, 0, size);
		return a;
	}
	@Override
	public boolean remove(Object o) {
		int i = 0;
		while(i < size){
			if(o.equals(elementData[i])){
				size--;
				while(i < size){
					i++;
					elementData[i-1] = elementData[i];
				}
				elementData[size] = null;
				return true;
			}
			i++;
		}
		return false;
	}
	@Override
	public boolean containsAll(Collection<?> c) {
		for(Object o : c){
			if(!contains(o)){
				return false;
			}
		}
		return true;
	}
	@Override
	public boolean addAll(Collection<? extends E> c) {
		ensureCapacity(c.size());
		boolean bol = false;
		for(E node : c){
			if(add(node)){
				bol = true;
			}
		}
		return bol;
	}
	@Override
	public boolean retainAll(Collection<?> c) {
		return removeIf((Object o) -> !c.contains(o));
	}
	@Override
	public boolean removeAll(Collection<?> c) {
		return removeIf((Object o) -> c.contains(o));
	}
	@Override
	public boolean removeIf(Predicate<? super E> pred){
		int a = 0,b = 0;
		a: while(b < size){
			while(pred.test(elementData[b])){
				b++;
				if(b == size){
					break a;
				}
			}
			elementData[a] = elementData[b];
			a++;
			b++;
		}
		size -= b-a;
		return b != a;
	}
	@Override
	public void clear() {
		int i = 0,s = size;
		size = 0;
		while(i < s){
			elementData[i] = null;
			i++;
		}
	}
	public E get(int index) {
		if(index < 0 || index >= size){
			throw new NoSuchElementException();
		}
		return elementData[index];
	}
	public E remove(int index) {
		if(index < 0 || index >= size){
			throw new IllegalArgumentException();
		}
		size--;
		E e = elementData[index];
		while(index < size){
			index++;
			elementData[index-1] = elementData[index];
		}
		elementData[size] = null;
		return e;
	}
	public int indexOf(Object o){
		return indexOf((Object a) -> o.equals(a));
	}
	public int indexOf(Predicate<? super E> pred){
		int i = 0;
		while(i < size){
			if(pred.test(elementData[i])){
				return i;
			}
			i++;
		}
		return -1;
	}
	@Override
	public boolean add(E e) {
		if(e.getClass() != elementData.getClass().getComponentType()){
			return false;
		}
		TreeNode pp = e.getParent();
		if(pp != parent && pp != null){
			return false;
		}
		e.setParent(parent);
		ensureCapacity(size+1);
		elementData[size++] = e;
		return true;
	}
	public boolean add(int index, E element) {
		if(element.getClass() != elementData.getClass().getComponentType()){
			return false;
		}
		if(index < 0 || index > size){
			throw new IllegalArgumentException();
		}
		int i = size;
		ensureCapacity(size+1);
		size++;
		while(i > index){
			elementData[i] = elementData[i-1];
			i--;
		}
		elementData[index] = element;
		element.setParent(parent);
		return true;
	}
	public void safeCheck(){
		int i = 0;
		while(i < size){
			if(elementData[i].getClass() != elementData.getClass().getComponentType()){
				throw new ArrayStoreException();
			}
			elementData[i].safeCheck();
			i++;
		}
	}
	public abstract void writeOut(DataOutputStream dataOut) throws IOException;
	/**
	 * WARNING!!! This method will return NamedTagNode always when an instance of NamedTagNode is received.
	 * Ignores known subclasses.
	 * @param node
	 * @return
	 */
	public static Tag cloneTag(Tag node){
		Tag newNode;
		if(node instanceof NamedTag){
			newNode = new NamedTag(((NamedTag)node).getName(), null);
		}else{
			newNode = new Tag(null);
		}
		newNode.setValue(cloneValue(node.getValue(),newNode));
		return newNode;
	}
	@SuppressWarnings("unchecked")
	public static <E> E cloneValue(E value,Tag newParent){
		switch(Tag.typeCode(value.getClass())){
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
		case 6:
		case 8:return value;
		case 7:return (E)((byte[])value).clone();
		case 9:TagList list = (TagList)value,newList = new TagList(newParent);
			newList.ensureCapacity(list.size());
			for(Tag node : list){
				newList.add(cloneTag(node));
			}
			return (E)newList;
		case 10:TagCompound comp = (TagCompound)value,newComp = new TagCompound(newParent);
			newComp.ensureCapacity(comp.size());
			for(NamedTag node : comp){
				newComp.add((NamedTag)cloneTag(node));
			}
			return (E)newComp;
		case 11:return (E)((int[])value).clone();
		case 12:return (E)((long[])value).clone();
		default:throw new IllegalArgumentException();
		}
	}
}
