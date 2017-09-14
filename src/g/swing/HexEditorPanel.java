package g.swing;

import static java.awt.Color.*;
import static java.awt.event.KeyEvent.*;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.nio.ByteBuffer;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;

import g.io.LongHeapSequence;
/**
 * Built in favor of the NBT editor, this panel is capable of basic HEX editing even a channel
 * backed by a max length long array.
 * @author Gergely Bálint
 *
 */
public class HexEditorPanel extends JPanel {
	public static void main(String[] atgs) throws Throwable{
		JFrame frame = new JFrame("HEXEDITORTEST"); //$NON-NLS-1$
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		LongHeapSequence hbs = new LongHeapSequence();
		int i = 0;
		while(i < 0x100){
			hbs.write(i,i);
			i++;
		}
		HexEditorPanel panel = new HexEditorPanel(hbs);
		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
	}
	private static final long serialVersionUID = 1L;
	private static final Font font = new Font(Font.MONOSPACED, Font.BOLD, 20);
	private Toolkit tk;
	private JScrollBar scrollBar;
	private LongHeapSequence sequence;
	private int cap = 1;
	private long caret = -1,length,limit = 		0x1000000000L;
	public static final long maximumLength =	0x7ffffffffL;// Division by 16 must make a positive integer.
	@SuppressWarnings("synthetic-access")
	private Listener ls = new Listener();
	public HexEditorPanel(LongHeapSequence seq) {
		super(null);
		super.setFocusable(true);
		tk = Toolkit.getDefaultToolkit();
		this.sequence = seq;
		scrollBar = new JScrollBar();
		super.add(scrollBar);
		scrollBar.setLocation(40*12+1,0);
		scrollBar.addAdjustmentListener(ls);
		scrollBar.addMouseWheelListener(ls);
		addMouseListener(ls);
		addMouseWheelListener(ls);
		addComponentListener(ls);
		addKeyListener(ls);
		setSize(40*12+scrollBar.getWidth(),32);
		repaint();
	}
	@Override
	public void setSize(int w,int h){
		super.setSize(w, h);
		scrollBar.setSize(scrollBar.getPreferredSize().width,h+1);
	}
	public void setUserLimit(long limit0){
		if(limit0 > maximumLength){
			limit0 = 0x100000000L;
		}
		if(limit0 < 0){
			throw new IllegalArgumentException();
		}
		limit = limit0*2;
	}
	public long getUserLimit(){
		return limit/2;
	}
	private long limit(){
		return Math.max(limit, length*2);
	}
	@SuppressWarnings({"synthetic-access"})
	private class Listener extends MouseAdapter implements ComponentListener,AdjustmentListener,KeyListener{
		@Override
		public void adjustmentValueChanged(AdjustmentEvent e) {
			update();
		}
		@Override
		public void componentResized(ComponentEvent e) {
			update();
		}
		@Override
		public void componentMoved(ComponentEvent e) {
			//
		}
		@Override
		public void componentShown(ComponentEvent e) {
			//
		}
		@Override
		public void componentHidden(ComponentEvent e) {
			//
		}
		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if(scrollBar.isEnabled()){
				int amout = e.getWheelRotation();
				scrollBar.setValue(scrollBar.getValue()+amout);
			}
		}
		@Override
		public void mousePressed(MouseEvent e) {
			HexEditorPanel.this.requestFocusInWindow();
			int i = revarie(e);
			if(i < limit()){
				caret = i;
			}
			repaint();
		}
		private int revarie(MouseEvent e){
			int x = e.getX(),y = e.getY();
			if(x%12 == 0 || y%16 == 0){
				return -1;
			}
			x /= 12;
			y /= 16;
			x -= 8;
			y -= 1;
			if(x < 0 || y < 0 || x >= 32*cap){
				return -1;
			}
			return x+((y+scrollBar.getValue())*32*cap);
		}
		@Override
		public void keyTyped(KeyEvent e) {
			//
		}
		@Override
		public void keyPressed(KeyEvent e) {
			a: if(caret >= 0){
				byte rec;
				switch(e.getKeyCode()){
				case VK_BACK_SPACE:
				case VK_DELETE:rec = rDel;break;
				case VK_NUMPAD0:
				case VK_0:rec = 0x0;break;
				case VK_NUMPAD1:
				case VK_1:rec = 0x01;break;
				case VK_NUMPAD2:
				case VK_2:rec = 0x02;break;
				case VK_NUMPAD3:
				case VK_3:rec = 0x03;break;
				case VK_NUMPAD4:
				case VK_4:rec = 0x04;break;
				case VK_NUMPAD5:
				case VK_5:rec = 0x05;break;
				case VK_NUMPAD6:
				case VK_6:rec = 0x06;break;
				case VK_NUMPAD7:
				case VK_7:rec = 0x07;break;
				case VK_NUMPAD8:
				case VK_8:rec = 0x08;break;
				case VK_NUMPAD9:
				case VK_9:rec = 0x09;break;
				case VK_A:rec = 0x0A;break;
				case VK_B:rec = 0x0B;break;
				case VK_C:rec = 0x0C;break;
				case VK_D:rec = 0x0D;break;
				case VK_E:rec = 0x0E;break;
				case VK_F:rec = 0x0F;break;
				case VK_KP_UP:
				case VK_UP:caret -= cap*32;
				if(caret < 0){
					caret += cap*32;
				}
				repaint();return;
				case VK_KP_DOWN:
				case VK_DOWN:int ic = cap*32;
						if(caret+ic < limit()){
							caret += ic;repaint();
						}return;
				case VK_KP_LEFT:
				case VK_LEFT:caret--;if(caret < 0){
					caret = 0;
				}
				repaint();return;
				case VK_KP_RIGHT:
				case VK_RIGHT:if(caret+1 < limit()){
									caret++;repaint();
								}return;
				default:break a;
				}
				recover(rec);
				update();
				return;
			}
			tk.beep();
		}
		@Override
		public void keyReleased(KeyEvent e) {
			//
		}
	}
	private static final byte rDel = (byte)0xF0;
	private void recover(byte rData){
		if(caret < 0){
			tk.beep();
			return;
		}
		if(rData == rDel){
			long pos = caret;
			pos /= 2;
			if(pos+1 >= length){
				caret = pos*2-1;
				if(caret < 0){
					caret = 0;
				}
				sequence.truncate(pos);
				return;
			}
		}
		long pos = caret,off = (pos%2 == 0) ? 4 : 0;
		pos /= 2;
		int id = sequence.read(pos);
		byte d = id < 0 ? 0 : (byte)(id & (0xF0 >>> off));
		id = Byte.toUnsignedInt((byte)(d | (((rData == rDel ? 0 : rData) & 0x0F) << off)));
		sequence.write(id, pos);
		if(rData == rDel){
			caret--;
		}else{
			caret++;
			if(caret >= limit()){
				caret = limit()-1;
			}
		}
	}
	public LongHeapSequence getSequence(){
		return sequence;
	}
	public void setSequence(LongHeapSequence sec){
		sequence = sec;
		repaint();
	}
	public void update(){
		if(EventQueue.isDispatchThread()){
			update0();
		}else{
			EventQueue.invokeLater(this::update0);
		}
	}
	private void update0(){
		length = sequence.size();
		if(length > maximumLength){
			length = maximumLength;
		}else if(length < 0){
			length = 0;
		}
		Dimension scpref = scrollBar.getPreferredSize();
		int locap = (getWidth()-(8*12+scpref.width))/12/32;
		if(locap < 1){
			locap = 1;
		}
		int capnum = locap*16,height = getHeight();
		scrollBar.setSize(scpref.width, height);
		scrollBar.setVisibleAmount(height/16-1);
		scrollBar.setMaximum((int) (length/capnum));
		if(cap != locap){
			scrollBar.setLocation((locap*32+8)*12+1, 0);
			cap = locap;
		}
		repaint();
	}
	@Override
	public Dimension getPreferredSize(){
		if(super.isPreferredSizeSet()){
			return super.getPreferredSize();
		}
		return new Dimension(40*12+1+scrollBar.getPreferredSize().width,0x110);
	}
	@Override
	public void paintComponent(Graphics g){
		Dimension dm = getSize();
		g.setColor(WHITE);
		g.fillRect(0,0,dm.width,dm.height);
		int capnum = cap*16,i = 8*12;
		if(caret >= 0){
			int h = (int) (caret/2/capnum+1),w = (int) ((caret%(capnum*2)+8)*12+caret%2);
			h -= scrollBar.getValue();
			g.setColor(CYAN);
			g.fillRect(w, 0, 12, dm.height);
			if(h > 0){
				h *= 16;
				g.fillRect(0, h, (capnum+4)*24, 16);
				g.setColor(BLUE);
				g.fillRect(w, h, 12, 16);
			}
		}
		g.setColor(GRAY);
		g.fillRect(0, 0, 8*12, 16);
		g.setFont(font);
		while(i <= dm.getWidth()){
			g.drawLine(i, 0, i, dm.height);
			i += 24;
		}
		i = 16;
		while(i < dm.height+15){
			g.drawLine(0, i, dm.width, i);
			i += 16;
		}
		i = 1;
		g.setColor(isEnabled() ? BLACK : GRAY);
		ByteBuffer buf = ByteBuffer.allocate(capnum);
		while(i <= (dm.height+15)/16){
			g.drawString(rowString(scrollBar.getValue(),i-1,buf), 1, (i*16)-1);
			i++;
		}
	}
	private String rowString(long offset,int row,ByteBuffer buf){
		StringBuilder str = new StringBuilder(8+cap*32);
		int i = 0;
		if(row == 0){
			str.append("        "); //$NON-NLS-1$
			while(i < cap*16){
				String hex = Integer.toHexString(i);
				if(hex.length() < 2){
					str.append('0');
				}
				str.append(hex);
				i++;
			}
		}else{
			offset += row-1;
			String hex = Long.toHexString(offset*cap);
			while(str.length()+hex.length() < 8){
				str.append('0');
				i++;
			}
			str.append(hex);
			sequence.read(buf,offset*cap*16);
			buf.flip();
			while(buf.hasRemaining()){
				hex = Integer.toHexString(Byte.toUnsignedInt(buf.get()));
				if(hex.length() < 2){
					str.append('0');
				}
				str.append(hex);
			}
			buf.clear();
		}
		return str.toString().toUpperCase();
	}
}
