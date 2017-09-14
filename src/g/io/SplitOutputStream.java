package g.io;

import java.io.IOException;
import java.io.OutputStream;

public class SplitOutputStream extends OutputStream{
	protected OutputStream a, b;
	public SplitOutputStream(OutputStream a1,OutputStream b1){
		a = a1;
		b = b1;
	}
	@Override
	public void write(int b1) throws IOException {
		try{
			a.write(b1);
		}finally{
			b.write(b1);
		}
	}
	@Override
	public void write(byte[] bytes,int off,int len) throws IOException{
		try{
			a.write(bytes,off,len);
		}finally{
			b.write(bytes,off,len);
		}
	}
	@Override
	public void flush() throws IOException{
		try{
			a.flush();
		}finally{
			b.flush();
		}
	}
	@Override
	public void close() throws IOException{
		try{
			a.close();
		}finally{
			b.close();
		}
	}
}
