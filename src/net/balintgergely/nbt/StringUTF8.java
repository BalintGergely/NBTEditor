package net.balintgergely.nbt;

import java.nio.CharBuffer;
import java.util.Arrays;
/**
 * An alternative to the string class, this will hold the original (possibly corrupt) array of bytes
 * of an UTF8 string to safely re-write it back into the file if the user does not modify it.
 * 
 * Decoding the string itself is also unnecessary if the user does not see it.
 * @author balintgergely
 */
public final class StringUTF8 implements CharSequence, Comparable<String>{
	public static final StringUTF8 EMPTY = new StringUTF8(new byte[0],new char[0],"");
	byte[] bytes;
	char[] chars;//It's a shame we can't have this point to the same array as string.
	String string;
	private StringUTF8(byte[] bytes0,char[] chars0,String str){
		bytes = bytes0;
		chars = chars0;
		string = str;
	}
	StringUTF8(byte[] bytes0) {
		bytes = bytes0;
	}
	StringUTF8(char[] chars0){
		chars = chars0;
	}
	public StringUTF8(String str){
		string = String.valueOf(str);
	}
	public StringUTF8(StringUTF8 value){
		bytes = value.getBytes();
		chars = value.getChars();
		string = value.toString();
	}
	@Override
	public int length() {
		return getChars().length;
	}
	private char[] getChars(){
		if(chars == null){
			if(bytes == null){
				chars = string.toCharArray();
			}else{//Either string or bytes is non-null
				chars = decode(bytes);
			}
		}
		return chars;
	}
	byte[] getBytes(){
		if(bytes == null){
			bytes = encode(getChars());
		}
		return bytes;
	}
	@Override
	public char charAt(int index) {
		return getChars()[index];
	}
	@Override
	public CharSequence subSequence(int start, int end) {
		return CharBuffer.wrap(getChars()).asReadOnlyBuffer().subSequence(start, end);
	}
	/**
	 * Converts this object into a String, caching the conversion results.
	 */
	@Override
	public String toString(){
		if(string == null){
			string = new String(getChars());
		}
		return string;
	}
	@Override
	public int hashCode(){
		return toString().hashCode();
	}
	@Override
	public boolean equals(Object that){
		if(that instanceof String){
			return toString().equals(that);
		}
		if(that instanceof StringUTF8){
			StringUTF8 other = (StringUTF8) that;
			if(chars != null && other.chars != null){
				return Arrays.equals(chars, other.chars);
			}else if(bytes != null && other.bytes != null){
				return Arrays.equals(bytes, other.bytes);
			}else{
				return Arrays.equals(getChars(), other.getChars());
			}
		}
		return false;
	}
	/**
	 * Performs a very light check to see if this StringUTF8 can be, or is encoded into a sequence of bytes shorter than 0xffff
	 */
	public boolean isValid(){
		if(bytes != null){
			return true;
		}else if(chars != null){
			return isValid(chars);
		}else{
			return isValid(string);
		}
	}
	public static boolean isValid(char[] chars){
		int c = chars.length*3,k = 0;
		while(c > 0xffff){
			if(k >= chars.length){
				return false;//This string cannot be encoded!
			}
			char chr = chars[k++];
			if((chr & 0xff80) == 0){
				c -= 2;
			}else if((chr & 0xf800) == 0){
				c -= 1;
			}
		}
		return true;
	}
	public static boolean isValid(CharSequence string){
		if(string instanceof StringUTF8){
			return ((StringUTF8)string).isValid();
		}
		int len = string.length(),c = len*3,k = 0;
		while(c > 0xffff){
			if(k >= len){
				return false;//This string cannot be encoded!
			}
			char chr = string.charAt(k++);
			if((chr & 0xff80) == 0){
				c -= 2;
			}else if((chr & 0xf800) == 0){
				c -= 1;
			}
		}
		return true;
	}
	public static char[] decode(byte[] bytes){
		char[] tmp = new char[bytes.length];
		int a = 0,b = 0;
		while(a < bytes.length){
			byte h1 = bytes[a++];
			if(h1 >= 0){//0
				tmp[b++] = (char)h1;
				continue;
			}
			if(h1 < -64){//10
				tmp[b++] = 0xFFFD;
				continue;
			}
			if(a >= bytes.length){
				tmp[b++] = 0xFFFD;
				break;
			}
			byte h2 = bytes[a++];
			if(h2 >= -64){//!
				tmp[b++] = 0xFFFD;
				a--;
				continue;
			}
			if(h1 < -32){//110
				tmp[b++] = (char)(
						((h1 & 0x1F) << 6) |
						 (h2 & 0x3F));
				continue;
			}
			if(a >= bytes.length){
				tmp[b++] = 0xFFFD;
				break;
			}
			byte h3 = bytes[a++];
			if(h3 >= -64){//!
				tmp[b++] = 0xFFFD;
				a--;
				continue;
			}
			if(h1 < -16){//1110
				tmp[b++] = (char)(
						((h1 & 0x0F) << 12) |
						((h2 & 0x3F) << 6) |
						 (h3 & 0x3F));
			}
			tmp[b++] = 0xFFFD;
		}
		if(b < tmp.length){
			return Arrays.copyOf(tmp, b);
		}
		return tmp;
	}
	public static byte[] encode(char[] chars){
		byte[] bytes = new byte[Math.min(chars.length*3, 0xffff)];
		int b = 0;
		try{
			for(char c : chars){
				if((c & 0xff80) == 0){
					bytes[b++] = (byte)c;
				}else if((c & 0xf800) == 0){
					bytes[b++] = (byte) (((c >> 6) & 0x1F) | 0xC0);
					bytes[b++] = (byte) ((c & 0x3F) | 0x80);
				}else{
					bytes[b++] = (byte) (((c >> 12) & 0xF) | 0xE0);
					bytes[b++] = (byte) (((c >> 6) & 0x3F) | 0x80);
					bytes[b++] = (byte) ((c & 0x3F) | 0x80);
				}
			}
		}catch(ArrayIndexOutOfBoundsException e){
			throw new IllegalStateException(e);
		}
		if(bytes.length == b){
			return bytes;
		}
		return Arrays.copyOf(bytes, b);
	}
	@Override
	public int compareTo(String o) {
		return toString().compareTo(o);
	}
	public static StringUTF8 valueOf(Object obj){
		if(obj instanceof StringUTF8){
			return (StringUTF8)obj;
		}
		String str = String.valueOf(obj);
		if(str.length() == 0){
			return EMPTY;
		}
		return new StringUTF8(str);
	}
	public static StringUTF8 valueOf(boolean bol){
		return new StringUTF8(Boolean.toString(bol));
	}
	public static StringUTF8 valueOf(byte byt){
		return new StringUTF8(Byte.toString(byt));
	}
	public static StringUTF8 valueOf(short sht){
		return new StringUTF8(Short.toString(sht));
	}
	public static StringUTF8 valueOf(int in){
		return new StringUTF8(Integer.toString(in));
	}
	public static StringUTF8 valueOf(long lon){
		return new StringUTF8(Long.toString(lon));
	}
	public static StringUTF8 valueOf(char ch){
		char[] c = new char[]{ch};
		return new StringUTF8(null,c,new String(c));
	}
	public static StringUTF8 valueOf(float fl){
		return new StringUTF8(Float.toString(fl));
	}
	public static StringUTF8 valueOf(double db){
		return new StringUTF8(Double.toString(db));
	}
}
