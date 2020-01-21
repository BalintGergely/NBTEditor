package net.balintgergely.nbt;

import java.io.DataOutput;
import java.io.IOException;
import java.nio.CharBuffer;
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
		@Override
		public <E> String toSNBT(E o) {
			throw new UnsupportedOperationException();
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
			return o.toString()+"i";
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
		@Override
		public <E> String toSNBT(E o) {
			return o.toString();
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
		@Override
		public <E> String toSNBT(E o) {
			byte[] array = (byte[])o;
			StringBuilder builder = new StringBuilder("[B;");
			boolean notFirst = false;
			for(byte b : array) {
				if(notFirst) {
					builder.append(',');
				}
				builder.append(b);
				builder.append("B");
				notFirst = true;
			}
			return builder.append(']').toString();
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
		@Override
		public <E> String toSNBT(E o) {
			String str = o.toString();
			StringBuilder builder = new StringBuilder(str.length()+2).append('"');
			int l = str.length();
			for(int i = 0;i < l;i++){//a
				char c = str.charAt(i);
				switch(c) {
				case '\\':
				case '"':builder.append('\\');break;//goto b
				case '\b':builder.append("\\b");continue;//goto a
				case '\f':builder.append("\\f");continue;//goto a
				case '\n':builder.append("\\n");continue;//goto a
				case '\r':builder.append("\\r");continue;//goto a
				case '\t':builder.append("\\t");continue;//goto a
				}//b
				builder.append(c);
			}
			return builder.append('"').toString();
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
		@Override
		public <E> String toSNBT(E o) {
			TagList<?> list = (TagList<?>)o;
			NBTType subType = list.type();
			StringBuilder builder = new StringBuilder("{");
			boolean notFirst = false;
			for(Tag<?> t : list) {
				if(notFirst) {
					builder.append(',');
				}
				builder.append(subType.toSNBT(t.value));
				notFirst = true;
			}
			return builder.append(']').toString();
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
		@Override
		public <E> String toSNBT(E o) {
			Compound comp = (Compound)o;
			StringBuilder builder = new StringBuilder("{");
			boolean notFirst = false;
			for(NamedTag<?> t : comp) {
				if(notFirst) {
					builder.append(',');
				}
				builder.append(STRING.toSNBT(t.name));
				builder.append(":");
				builder.append(t.TYPE.toSNBT(t.value));
				notFirst = true;
			}
			return builder.append('}').toString();
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
		@Override
		public <E> String toSNBT(E o) {
			int[] array = (int[])o;
			StringBuilder builder = new StringBuilder("[I;");
			boolean notFirst = false;
			for(int b : array) {
				if(notFirst) {
					builder.append(',');
				}
				builder.append(b);
				notFirst = true;
			}
			return builder.append("]").toString();
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
		@Override
		public <E> String toSNBT(E o) {
			long[] array = (long[])o;
			StringBuilder builder = new StringBuilder("[L;");
			boolean notFirst = false;
			for(long b : array) {
				if(notFirst) {
					builder.append(',');
				}
				builder.append(b);
				builder.append("L");
				notFirst = true;
			}
			return builder.append("]").toString();
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
	public static Object parseSNBT(String str){
		return new Tag<>(truncate(str));
	}
	/**
	 * Removes all whitespace characters outside of string blocks from the given char sequence. Throws an exception if a string block isn't closed.
	 */
	public static CharBuffer truncate(CharSequence s){
		int len = s.length();
		CharBuffer buf = CharBuffer.allocate(len);
		int i = 0;
		while(i < len){
			char x = s.charAt(i++);
			if(!Character.isWhitespace(x)){
				buf.append(x);
				if(x == '"'){
					do{
						buf.append(x = s.charAt(i++));
						if(x == '\\'){
							buf.append(s.charAt(i++));
						}
					}while(x != '"');
				}
			}
		}
		return buf.flip();
	}
	private static StringUTF8 parseSNBT(CharBuffer cbuf) {
		cbuf.get();
		int end,start = end = cbuf.position();
		int countSub = 0;
		w: while(true){
			switch(cbuf.get(end)) {
			case '"':break w;
			case '/':countSub++;end++;
			}
			end++;
		}
		char[] array = new char[end-start-countSub];
		countSub = 0;
		w: while(true){
			char c = cbuf.get(start++);
			s: switch(c) {
			case '"':break w;
			case '\\':c = cbuf.get(start++);
			switch(c) {
				case 'b':array[countSub++] = '\b';break s;
				case 'f':array[countSub++] = '\f';break s;
				case 'n':array[countSub++] = '\n';break s;
				case 'r':array[countSub++] = '\r';break s;
				case 't':array[countSub++] = '\t';break s;
			}//$FALL-THROUGH$
			default:array[countSub++] = c;
			}
		}
		cbuf.position(end+1);
		return new StringUTF8(array);
	}
	static NBTType parseSNBT(Tag<Object> t,CharBuffer cbuf) {
		int start = cbuf.position();
		char c = cbuf.get(start);
		switch(c) {
		case '"':
			t.value = parseSNBT(cbuf);
			return STRING;
		case '[':
			cbuf.position(++start);
			c = cbuf.get(start);
			if(c == 'b' || c == 'B' || c == 'i' || c == 'I' || c == 'l' || c == 'L'){
				if(cbuf.get(++start) != ';'){
					throw new RuntimeException();
				}
				cbuf.position(++start);
				int count = 0;
				if(cbuf.get(start) != ']'){
					count++;
					int index = start;
					while(true){
						char x = cbuf.get(index);
						if(x == ','){
							count++;
						}else if(x == ']'){
							break;
						}
						index++;
					}
				}
				switch(c){
				case 'b':
				case 'B':
					byte[] bytes = new byte[count];
					for(int i = 0;i < count;i++){
						while(true){
							char x = cbuf.get();
							if(x == ',' || x == ']'){
								break;
							}
						}
						bytes[i] = Byte.parseByte(cbuf.slice(start, cbuf.position()-start-1).toString());
					}
					t.value = bytes;
					return BYTE_ARRAY;
				case 'i':
				case 'I':
					int[] ints = new int[count];
					for(int i = 0;i < count;i++){
						while(true){
							char x = cbuf.get();
							if(x == ',' || x == ']'){
								break;
							}
						}
						ints[i] = Integer.parseInt(cbuf.slice(start, cbuf.position()-start-1).toString());
					}
					t.value = ints;
					return INT_ARRAY;
				case 'l':
				case 'L':
					long[] longs = new long[count];
					for(int i = 0;i < count;i++){
						while(true){
							char x = cbuf.get();
							if(x == ',' || x == ']'){
								break;
							}
						}
						longs[i] = Long.parseLong(cbuf.slice(start, cbuf.position()-start-1).toString());
					}
					t.value = longs;
					return LONG_ARRAY;
				}
			}else{
				TagList<Object> list = new TagList<>();
				if(c == ']'){
					cbuf.get();
				}else{
					a: while(true){
						list.add(new Tag<>(cbuf));
						switch(cbuf.get()){
						case ',':
							if(cbuf.get(cbuf.position()) != ']'){
								continue;
							}
							cbuf.get();//$FALL-THROUGH$
						case ']':
							break a;
						}
						throw new RuntimeException();
					}
				}
				t.value = list;
				return LIST;
			}//$FALL-THROUGH$ Not possible.
		case '{':
			cbuf.position(start+1);
			Compound comp = new Compound();
			a: while(true){
				switch(cbuf.get()){
				case '}':break a;
				case '"':
					StringUTF8 name = parseSNBT(cbuf);
					if(cbuf.get() == ':'){
						comp.add(new NamedTag<>(name, cbuf));
						switch(cbuf.get()){
						case ',':continue a;
						case '}':break a;
						}
					}
				}
				throw new RuntimeException();
			}
			t.value = comp;
			return COMPOUND;
		default:
			int end = start;
			boolean containsDecimalPoint = false;
			while(true){
				c = cbuf.get(end);
				if(c == '-' || (c >= '0' && c <= '9')) {
					end++;
				}else if(c == '.' || c == 'e' || c == 'E') {
					end++;
					containsDecimalPoint = true;
				}else break;
			}
			if(end == start) {
				throw new RuntimeException();
			}
			NBTType type;
			switch(cbuf.get(end)){
			case 'b':
			case 'B':
				t.value = Byte.valueOf(cbuf.slice(start, end-start).toString());
				type = BYTE;
				end++;
				break;
			case 's':
			case 'S':
				t.value = Short.valueOf(cbuf.slice(start, end-start).toString());
				type = SHORT;
				end++;
				break;
			case 'l':
			case 'L':
				t.value = Long.valueOf(cbuf.slice(start, end-start).toString());
				type = LONG;
				end++;
				break;
			case 'f':
			case 'F':
				t.value = Float.valueOf(cbuf.slice(start, end-start).toString());
				type = FLOAT;
				end++;
				break;
			case 'd':
			case 'D':
				t.value = Double.valueOf(cbuf.slice(start, end-start).toString());
				type = DOUBLE;
				end++;
				break;
			default:
				if(containsDecimalPoint) {
					t.value = Double.valueOf(cbuf.slice(start, end-start).toString());
					type = DOUBLE;
				}else{
					t.value = Integer.valueOf(cbuf.slice(start, end-start).toString());
					type = INTEGER;
				}
			}
			cbuf.position(end);
			return type;
		}
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
	public <E> String toSNBT(E o) {
		return toString(o);
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