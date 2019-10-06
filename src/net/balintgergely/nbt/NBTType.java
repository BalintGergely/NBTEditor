package net.balintgergely.nbt;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

@SuppressWarnings("unchecked")
public enum NBTType {
	END(0,Void.class,null,"TAG_End") {//Void cannot be instantiated
		@Override
		public <E> E clone(E val) {
			throw val == null ? new NullPointerException() : new ClassCastException();
		}
		@Override
		public <E> String toString(E o) {
			return void.class.getName();
		}
	},
	BYTE(1,Byte.class,Byte.valueOf((byte)0),"TAG_Byte") {
		@Override
		public <E> E clone(E val) {
			return val;
		}
		@Override
		public <E> String toString(E o) {
			return o.toString()+'b';
		}
		@Override
		public <E> E parse(String str) {
			try{
				String hex = thx(str);
				int i;
				if(hex != null){
					i = Integer.parseInt(hex, 0x10);
				}else{
					i = Integer.parseInt(str);
				}
				if(i < Byte.MIN_VALUE || i >= 0x100){
					return null;
				}else{
					return (E)Byte.valueOf((byte)i);
				}
			}catch(NumberFormatException e){
				return null;
			}
		}
		@Override
		public boolean equals(Object a, Object b) {
			return a.equals(b);
		}
	},
	SHORT(2,Short.class,Short.valueOf((short)0x0),"TAG_Short") {
		@Override
		public <E> E clone(E val) {
			return val;
		}
		@Override
		public <E> String toString(E o) {
			return o.toString()+'s';
		}
		@Override
		public <E> E parse(String str) {
			try{
				String hex = thx(str);
				int i;
				if(hex != null){
					i = Integer.parseInt(hex, 0x10);
				}else{
					i = Integer.parseInt(str);
				}
				if(i < Short.MIN_VALUE || i >= 0x10000){
					return null;
				}else{
					return (E)Short.valueOf((short)i);
				}
			}catch(NumberFormatException e){
				return null;
			}
		}
		@Override
		public boolean equals(Object a, Object b) {
			return a.equals(b);
		}
	},
	INTEGER(3,Integer.class,Integer.valueOf(0),"TAG_Int") {
		@Override
		public <E> E clone(E val) {
			return val;
		}
		@Override
		public <E> String toString(E o) {
			return o.toString()+'i';
		}
		@Override
		public <E> E parse(String str) {
			try{
				String hex = thx(str);
				int i;
				if(hex != null){
					i = Integer.parseUnsignedInt(hex, 0x10);
				}else if(str.charAt(0) == '-'){
					i = Integer.parseInt(str);
				}else{
					i = Integer.parseUnsignedInt(str);
				}
				return (E)Integer.valueOf(i);
			}catch(NumberFormatException e){
				return null;
			}
		}
		@Override
		public boolean equals(Object a, Object b) {
			return a.equals(b);
		}
	},
	LONG(4,Long.class,Long.valueOf(0),"TAG_Long") {
		@Override
		public <E> E clone(E val) {
			return val;
		}
		@Override
		public <E> String toString(E o) {
			return o.toString()+'L';
		}
		@Override
		public <E> E parse(String str) {
			try{
				String hex = thx(str);
				long i;
				if(hex != null){
					i = Long.parseUnsignedLong(hex, 0x10);
				}else if(str.charAt(0) == '-'){
					i = Long.parseLong(str);
				}else{
					i = Long.parseUnsignedLong(str);
				}
				return (E)Long.valueOf(i);
			}catch(NumberFormatException e){
				return null;
			}
		}
		@Override
		public boolean equals(Object a, Object b) {
			return a.equals(b);
		}
	},
	FLOAT(5,Float.class,Float.valueOf(0.0f),"TAG_Float") {
		@Override
		public <E> E clone(E val) {
			return val;
		}
		@Override
		public <E> String toString(E o) {
			return o.toString()+"f";
		}
		@Override
		public <E> E parse(String str) {
			try{
				String hex = thx(str);
				if(hex != null){
					return (E)Float.valueOf(Float.intBitsToFloat(Integer.parseUnsignedInt(hex, 0x10)));
				}
				return (E)Float.valueOf(str);
			}catch(NumberFormatException e){
				return null;
			}
		}
		@Override
		public boolean equals(Object a, Object b) {
			return a.equals(b);
		}
	},
	DOUBLE(6,Double.class,Double.valueOf(0.0),"TAG_Double") {
		@Override
		public <E> E clone(E val) {
			return val;
		}
		@Override
		public <E> String toString(E o) {
			return o.toString()+"D";
		}
		@Override
		public <E> E parse(String str) {
			try{
				String hex = thx(str);
				if(hex != null){
					return (E)Double.valueOf(Double.longBitsToDouble(Long.parseUnsignedLong(hex, 0x10)));
				}
				return (E)Double.valueOf(str);
			}catch(NumberFormatException e){
				return null;
			}
		}
		@Override
		public boolean equals(Object a, Object b) {
			return a.equals(b);
		}
	},
	BYTE_ARRAY(7,byte[].class,new byte[0x0],"TAG_Byte_Array") {
		@Override
		public <E> E clone(E val) {
			return (E)clone((byte[])val);
		}
		public byte[] clone(byte[] val){
			return Arrays.copyOf(val, val.length);
		}
		@Override
		public <E> String toString(E o) {
			return Integer.toString(((byte[])o).length)+" bytes";
		}
		@Override
		public boolean equals(Object a, Object b) {
			return Arrays.equals((byte[])a, (byte[])b);
		}
	},
	STRING(8,StringUTF8.class,StringUTF8.EMPTY,"TAG_String") {
		@Override
		public <E> E clone(E val) {
			return val;
		}
		@Override
		public <E> String toString(E o) {
			return '\"'+o.toString()+'\"';
		}
		@Override
		public <E> E approve(Object value){
			value = StringUTF8.valueOf(value);
			if(!((StringUTF8)value).isValid()){
				return null;
			}
			return (E)value;
		}
		@Override
		public <E> E parse(String str) {
			StringUTF8 value = StringUTF8.valueOf(str);
			if(!value.isValid()){
				return null;
			}
			return (E)value;
		}
		@Override
		public boolean equals(Object a, Object b) {
			return a.equals(b);
		}
	},
	LIST(9,TagList.class,null,"TAG_List") {
		@Override
		public <E> E clone(E val) {
			return (E)clone((TagList<Object>)val);
		}
		public <E> TagList<E> clone(TagList<E> val){
			return val.clone();
		}
		@Override
		public <E> String toString(E o) {
			return o.toString();
		}
		@Override
		public <E> E defaultValue(){
			return (E)new TagList<>();
		}
	},
	COMPOUND(10,Compound.class,null,"TAG_Compound") {
		@Override
		public <E> E clone(E val) {
			return (E)clone((Compound)val);
		}
		public Compound clone(Compound val){
			return val.clone();
		}
		@Override
		public <E> String toString(E o) {
			return o.toString();
		}
		@Override
		public <E> E defaultValue(){
			return (E)new Compound();
		}
	},
	INT_ARRAY(11,int[].class,new int[0],"TAG_Int_Array") {
		@Override
		public <E> E clone(E val) {
			return (E)clone((int[])val);
		}
		public int[] clone(int[] val){
			return Arrays.copyOf(val, val.length);
		}
		@Override
		public <E> String toString(E o) {
			return ((int[])o).length+" ints";
		}
		@Override
		public boolean equals(Object a, Object b) {
			return Arrays.equals((int[])a, (int[])b);
		}
	},
	LONG_ARRAY(12,long[].class,new long[0],"TAG_Long_Array") {
		@Override
		public <E> E clone(E val) {
			return (E)clone((long[])val);
		}
		public long[] clone(long[] val){
			return Arrays.copyOf(val, val.length);
		}
		@Override
		public <E> String toString(E o) {
			return ((long[])o).length+" longs";
		}
		@Override
		public boolean equals(Object a, Object b) {
			return Arrays.equals((long[])a, (long[])b);
		}
	};
	public static final Charset UTF_8 = StandardCharsets.UTF_8;
	private static final NBTType[] TYPE_LIST = values();
	public static final int TYPE_COUNT = TYPE_LIST.length;
	public static NBTType get(int type_code){
		return TYPE_LIST[type_code];
	}
	public void writeOrdinal(DataOutput output) throws IOException{
		output.writeByte(ordinal());
	}
	public final Class<?> clasz;
	public final String name;
	public final byte typeCode;
	private final Object defaultValue;
	private <E> NBTType(int typeCode0,Class<E> type0,E defaultValue0,String name0){
		clasz = type0;
		name = name0;
		defaultValue = defaultValue0;
		typeCode = (byte)typeCode0;
	}
	public <E> E defaultValue(){
		return (E)defaultValue;
	}
	public abstract <E> E clone(E val);
	/**
	 * Parses the String into an object of this type. Throws UOE if not applicable.
	 * @return An object if the parsing was success full. null if the parsing failed.
	 */
	public <E> E parse(String str) {
		throw new UnsupportedOperationException();
	}
	public boolean equals(Object a,Object b){
		throw new UnsupportedOperationException();
	}
	public static String thx(String str){
		int l = str.length();
		if(l < 1){
			throw new NumberFormatException();
		}
		if(l < 3){
			return null;
		}
		char c = str.charAt(1);
		if((c == 'x' || c == 'X') && str.charAt(0) == '0'){
			return str.substring(2);
		}
		return null;
	}
	public <E> E approve(Object obj){
		return (E)clasz.cast(Objects.requireNonNull(obj));
	}
	public <E> String toString(E o){
		return String.valueOf(o);
	}
	@Override
	public String toString(){
		return name;
	}
	public static NBTType forClass(Class<?> clasz){
		for(NBTType t : TYPE_LIST){
			if(t.clasz.equals(clasz)){
				return t;
			}
		}
		return null;
	}
	public static Object convert(Object o){
		try{
			if(o instanceof NBTConvertible){
				o = Objects.requireNonNull(((NBTConvertible)o).formatNBT());
			}else if(o instanceof CharSequence){
				o = StringUTF8.valueOf(o);
			}else{
				o = forClass(o.getClass()).approve(o);
			}
			return o;
		}catch(NullPointerException e){
			throw new IllegalArgumentException(e);
		}
	}
}