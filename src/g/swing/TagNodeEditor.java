package g.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.EventObject;
import java.util.Objects;
import java.util.function.Function;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CellEditorListener;
import javax.swing.tree.TreeCellEditor;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import g.NBTEditor;
import g.io.BaseFileNode;
import g.io.FileNode;
import g.io.LongHeapSequence;
import g.nbt.Chunk;
import g.nbt.NamedTag;
import g.nbt.Tag;
import g.swing.undo.AbstractTagEdit;
import g.swing.undo.ChunkIdEdit;
import g.swing.undo.MultiTagEdit;
import g.swing.undo.Rename;
import g.swing.undo.ValueEdit;
import g.util.ArrayBuilder;

public class TagNodeEditor extends UndoableEditSupport implements TreeCellEditor{
	private JPanel panel;
	private JDialog dialog;
	private LongHeapSequence lhs;
	private HexEditorPanel hex;
	private JTextField nameTextField,df,hf,cx,cy;
	private JTextArea textArea;
	private JScrollPane textAreaPane;
	private JLabel labl1;
	private String setName;
	private Object setValue;
	private long minInt,maxInt;
	private Function<Object,Object> fieldChecker;
	private Tag editorV;
	private JButton dialogButton;
	private WeakReference<NBTEditor> myEditor;
	private boolean lock;
	private int typeCode;
	private byte extraData;
	private boolean lock(){
		if(lock){
			return true;
		}
		lock = true;
		return false;
	}
	private void unlock(){
		lock = false;
	}
	@SuppressWarnings({ "nls", "deprecation" })
	public TagNodeEditor(JFrame frame,JFileChooser fc,NBTEditor edt) {//Looong looong constructor...
		myEditor = new WeakReference<>(edt);
		dialog = new JDialog(frame);
		dialog.setModal(true);
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setBackground(Color.WHITE);
		labl1 = new JLabel();
		labl1.setVerticalAlignment(SwingConstants.TOP);
		
		setValue = setName = new String();
		nameTextField = new JTextField(setName,10);
		nameTextField.addCaretListener((CaretEvent e) -> {
			String text = nameTextField.getText();
			if(text.getBytes(Tag.charset).length <= Short.MAX_VALUE){
				setName = text;
				nameTextField.setForeground(Color.BLACK);
			}else{
				nameTextField.setForeground(Color.RED);
			}
		});
		textAreaPane = new JScrollPane(textArea = new JTextArea(1,100));
		textAreaPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		textArea.setLineWrap(true);
		textAreaPane.setBorder(nameTextField.getBorder());
		textArea.setFont(nameTextField.getFont());
		textArea.addCaretListener((CaretEvent e) -> {
			String text = textArea.getText();
			if(setValue == null || !text.equals(setValue.toString())){
				setValue = fieldChecker.apply(text);
				textArea.setForeground(setValue == null ? Color.RED : Color.BLACK);
			}
		});
		Font font = new Font(Font.MONOSPACED, Font.BOLD, 12);
		df = new JTextField(21);
		hf = new JTextField(17);
		cx = new JTextField(2);
		cy = new JTextField(2);
		df.setFont(font);
		hf.setFont(font);
		cx.setFont(font);
		cy.setFont(font);
		df.setBorder(nameTextField.getBorder());
		hf.setBorder(nameTextField.getBorder());
		cx.setBorder(nameTextField.getBorder());
		cy.setBorder(nameTextField.getBorder());
		df.addCaretListener((CaretEvent e) -> {
			if(lock()){
				return;
			}
			try{
				String str = df.getText();
				try{
					long l;
					if(str.charAt(0) == '-'){
						l = Long.parseLong(str);
					}else{
						l = Long.parseUnsignedLong(str);
					}
					if(l >= minInt && l <= maxInt){
						if(maxInt != Long.MAX_VALUE && l < 0){
							l += maxInt+1;
						}
						Long value = new Long(l);
						if(!value.equals(setValue)){
							setValue = value;
							hf.setText(Long.toHexString(l));
							hf.setForeground(Color.BLACK);
						}
						df.setForeground(Color.BLACK);
						return;
					}
				}catch(NumberFormatException | IndexOutOfBoundsException nfe){
					//
				}
				df.setForeground(Color.RED);
			}finally{
				unlock();
			}
		});
		hf.addCaretListener((CaretEvent e) -> {
			if(lock()){
				return;
			}
			try{
				try{
					String str = hf.getText();
					long l = Long.parseUnsignedLong(str,16);
					if(l >= minInt && l <= maxInt){
						Long value = new Long(l);
						if(!value.equals(setValue)){
							setValue = value;
							df.setText(Long.toUnsignedString(value.longValue()));
							df.setForeground(Color.BLACK);
						}
						hf.setForeground(Color.BLACK);
						return;
					}
				}catch(NumberFormatException nfe){
					//
				}
				hf.setForeground(Color.RED);
			}finally{
				unlock();
			}
		});
		cx.addCaretListener((CaretEvent e) -> {
			if(lock()){
				return;
			}
			try{
				try{
					String str = cx.getText();
					byte b = Byte.parseByte(str);
					if(b >= 0 && b < 32){
						if((extraData & 1) == 1){
							extraData--;
						}
						cx.setForeground(Color.BLACK);
						return;
					}
				}catch(NumberFormatException nfe){
					//
				}
				if((extraData & 1) == 0){
					extraData++;
				}
				cx.setForeground(Color.RED);
			}finally{
				unlock();
			}
		});
		cy.addCaretListener((CaretEvent e) -> {
			if(lock()){
				return;
			}
			try{
				try{
					String str = cy.getText();
					byte b = Byte.parseByte(str);
					if(b >= 0 && b < 32){
						if((extraData & 2) != 0){
							extraData -= 2;
						}
						cy.setForeground(Color.BLACK);
						return;
					}
				}catch(NumberFormatException nfe){
					//
				}
				if((extraData & 2) == 0){
					extraData += 2;
				}
				cy.setForeground(Color.RED);
			}finally{
				unlock();
			}
		});
		dialog.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.FIRST_LINE_START;
		c.weightx = 1;
		JButton save = new JButton("Save as"),load = new JButton("Load"),ok = new JButton("OK");
		dialog.add(load,c);
		c.gridx = 1;
		dialog.add(save,c);
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 2;
		dialog.add(ok,c);
		c.weighty = 1;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		lhs = new LongHeapSequence();
		hex = new HexEditorPanel(lhs);
		dialog.add(hex,c);
		save.addActionListener((ActionEvent e) -> {
			int ret = fc.showSaveDialog(dialog);
			if(ret == JFileChooser.APPROVE_OPTION){
				File f = fc.getSelectedFile();
				if(f.exists()){
					String str = "The file with the same name already exists!"; //$NON-NLS-1$
					if(f.isDirectory()){
						JOptionPane.showMessageDialog(panel, 
								str, "ERROR", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
						return;
					}
					if(f.isFile()){
						int res = JOptionPane.showConfirmDialog(panel, str, 
								"Overwrite existing file?", JOptionPane.OK_CANCEL_OPTION); //$NON-NLS-1$
						if(res != JOptionPane.OK_OPTION){
							return;
						}
					}
					try(FileOutputStream output = new FileOutputStream(f, false)){
						lhs.saveTo(output);
						output.flush();
						JOptionPane.showMessageDialog(panel, "File saved!"); //$NON-NLS-1$
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(panel, e1.toString(), "Error while writing file",JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
					}
				}
			}
		});
		load.addActionListener((ActionEvent e) -> {
			int ret = fc.showOpenDialog(panel);
			if(ret == JFileChooser.APPROVE_OPTION){
				File f = fc.getSelectedFile();
				if(!f.isFile()){
					JOptionPane.showMessageDialog(panel, "The file does not exists!", "ERROR", JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
				try(FileInputStream input = new FileInputStream(f)){
					long fS = f.length();
					lhs.truncate(fS);
					lhs.ensureCapacity(fS);
					lhs.trimToCapacity();
					lhs.loadFrom(input);
					input.close();
				} catch (IOException e1) {
					JOptionPane.showMessageDialog(panel, e1.toString(), "Error while loading file",JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
				}
			}
			hex.update();
		});
		dialogButton = new JButton();
		dialogButton.addActionListener((ActionEvent e) -> {
			hex.update();
			dialog.pack();
			dialog.show();
		});
		ok.addActionListener((ActionEvent e) -> {
			int t = editorV.getTypeCode();
			if(t == 11){
				ByteBuffer buf = ByteBuffer.allocate(1024);
				IntBuffer ibuf = IntBuffer.allocate((int)((lhs.size()+3)/4));
				long pos = 0;
				while(lhs.read(buf, pos) > 0){
					buf.flip();
					pos += buf.remaining();
					while(buf.remaining() > 3){
						ibuf.put(buf.getInt());
					}
					buf.clear();
				}
				setValue = ibuf.array();
			}else if(t == 12){
				ByteBuffer buf = ByteBuffer.allocate(1024);
				LongBuffer ibuf = LongBuffer.allocate((int)((lhs.size()+7)/8));
				long pos = 0;
				while(lhs.read(buf, pos) > 0){
					buf.flip();
					pos += buf.remaining();
					while(buf.remaining() > 3){
						ibuf.put(buf.getLong());
					}
					buf.clear();
				}
				setValue = ibuf.array();
			}else{
				ByteBuffer buf = ByteBuffer.allocate((int)lhs.size());
				lhs.read(buf, 0);
				setValue = buf.array();
			}
			dialog.dispose();
			dialogButton.setText(Tag.valueString(setValue));
		});
	}
	@Override
	public Object getCellEditorValue() {
		if(editorV == null){
			return null;
		}
		switch(typeCode){
		case 1:
		case 2:
		case 3:
		case 4:return fieldChecker.apply(setValue);
		case 5:
		case 6:
		case 7:
		case 8:
		case 11:
		case 12:return setValue;
		default:return null;
		}
	}
	@Override
	public boolean isCellEditable(EventObject anEvent) {
		if(anEvent instanceof MouseEvent){
			MouseEvent moEvent = (MouseEvent)anEvent;
			return moEvent.getClickCount() > 1;
		}
		return anEvent == null;
	}
	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		return true;
	}
	@Override
	public boolean stopCellEditing() {
		if(editorV == null){
			return true;
		}
		Object b = getCellEditorValue();
		boolean isComp = editorV.getAllowsChildren();
		if(b != null || isComp || extraData == 0){
			Object a = editorV.getValue();
			ArrayBuilder<AbstractTagEdit> builder = new ArrayBuilder<>(AbstractTagEdit.class);
			if(editorV instanceof NamedTag){
				if(editorV instanceof Chunk){
					String xstr = cx.getText(),ystr = cy.getText();
					try{
						int xi = Integer.parseInt(xstr),yi = Integer.parseInt(ystr);
						if(xi < 0 || xi > 31 || yi < 0 || yi > 31){
							return false;
						}
						xi |= yi*32;
						Chunk ch = (Chunk)editorV;
						if(ch.getChunkId() != xi){
							NBTEditor m = myEditor.get();
							builder.accept(new ChunkIdEdit(ch,ch.getChunkId(),xi));
							ch.setChunkId(xi);
							if(m != null){
								m.updateModel(ch.getParent());
							}
						}
					}catch(NumberFormatException e){
						return false;
					}
				}
				NamedTag node = (NamedTag)editorV;
				String pn = node.getName();
				if(!pn.equals(setName)){
					builder.accept(new Rename(node,pn,node.setName(setName)));
				}
			}
			dialog.dispose();
			panel.removeAll();
			if(!editorV.getAllowsChildren() && !a.equals(b)){
				builder.accept(new ValueEdit(editorV,a,b));
			}
			UndoableEdit edit = MultiTagEdit.combine(builder.get());
			if(edit.isSignificant()){//An edit is significant if it does something.
				if(!isComp){
					editorV.setValue(b);
				}
				editorV.setSignificant();
				postEdit(edit);
			}
			return true;
		}
		return false;
	}
	@Override
	public void cancelCellEditing() {
		dialog.dispose();
		//
	}
	@SuppressWarnings("nls")
	@Override
	public synchronized Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
			boolean leaf, int row) {
		a: if(value instanceof Tag){
			if(value instanceof FileNode && !((FileNode)value).isLoaded()){
				break a;
			}
			int tc = ((Tag)value).getTypeCode();
			if(tc < 1 || tc > 12){
				break a;
			}
			panel.removeAll();
			panel.add(labl1);
			editorV = (Tag) value;
			if(editorV.getClass() == FileNode.class){
				labl1.setText(((BaseFileNode)editorV).getFile().getName()+" > ");
			}else{
				labl1.setText(null);
			}
			extraData = 0;
			typeCode = tc;
			ImageIcon icon = (ImageIcon)labl1.getIcon();
			NBTEditor edt1 = Objects.requireNonNull(myEditor.get());
			if(icon == null || !icon.getImage().equals(edt1.images[typeCode])){
				labl1.setIcon(new ImageIcon(edt1.images[typeCode]));
			}
			if(value instanceof NamedTag){
				if(value instanceof Chunk){
					int chunkId = ((Chunk)value).getChunkId(),b = chunkId & 31;
					chunkId >>= 5;
					cx.setText(Integer.toString(b));
					cy.setText(Integer.toString(chunkId));
					panel.add(cx);
					panel.add(cy);
				}
				setName = ((NamedTag)editorV).getName();
				nameTextField.setText(setName);
				panel.add(nameTextField);
			}
			if(typeCode == 9 || typeCode == 10){
				if(!(value instanceof NamedTag)){
					break a;
				}
				return panel;
			}
			setValue = editorV.getValue();
			switch(typeCode){
			case 1:minInt = Byte.MIN_VALUE;maxInt = 0xff;fieldChecker = (Object obj) -> {
				if(obj instanceof Number){
					Number num = (Number)obj;
					return new Byte((byte) (num.longValue() & 0xff));
				}
				return null;
				};break;
			case 2:minInt = Short.MIN_VALUE;maxInt = 0xffff;fieldChecker = (Object obj) -> {
				if(obj instanceof Number){
					Number num = (Number)obj;
					return new Short((short) (num.longValue() & 0xffff));
				}
				return null;
				};break;
			case 3:minInt = Integer.MIN_VALUE;maxInt = 0xffffffffL;fieldChecker = (Object obj) -> {
				if(obj instanceof Number){
					Number num = (Number)obj;
					return new Integer((int) (num.longValue() & 0xffffffff));
				}
				return null;
				};break;
			case 4:minInt = Long.MIN_VALUE;maxInt = Long.MAX_VALUE;fieldChecker = (Object obj) -> {
				if(obj instanceof Long){
					return obj;
				}
				return null;
				};break;
			case 5:fieldChecker = (Object obj) -> {
				try{
					return Float.valueOf(String.valueOf(obj));
				}catch(NumberFormatException e){
					return null;
				}
			};break;
			case 6:fieldChecker = (Object obj) -> {
				try{
					return Double.valueOf(String.valueOf(obj));
				}catch(NumberFormatException e){
					return null;
				}
			};break;
			case 8:fieldChecker = (Object obj) -> {
				String str = obj.toString();
				return str.getBytes(Tag.charset).length > Short.MAX_VALUE ? null : str;
			};break;
			}
			switch(typeCode){
			case 1:
			case 2:
			case 3:
			case 4:
				setValue = new Long(((Number)setValue).longValue());
				df.setText(setValue.toString());
				hf.setText(Long.toHexString(((Number)setValue).longValue()));
				panel.add(hf);
				panel.add(df);break;
			case 8:
				textArea.setColumns(100);
				textAreaPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
				//$FALL-THROUGH$
			case 5:
			case 6:
				if(typeCode != 8){
					textArea.setColumns(50);
					textAreaPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
				}
				textArea.setText(setValue.toString());
				panel.add(textAreaPane);break;
			case 7:
			case 11:
			case 12:
				int i = editorV.getTypeCode();
				switch(i){
				case 7:i = 1;break;
				case 11:i = 4;break;
				case 12:i = 8;break;
				}
				hex.setUserLimit(((long)Integer.MAX_VALUE-4)*i);
				ByteBuffer tBuf = i != 1 ? ByteBuffer.allocate(1024) : 
					(ByteBuffer)(ByteBuffer.wrap((byte[])editorV.getValue())).duplicate().clear();
				lhs.truncate(0);
				if(i == 4){
					IntBuffer iBuf = (IntBuffer)((IntBuffer.wrap((int[])editorV.getValue())).duplicate().clear());
					lhs.ensureCapacity(((long)iBuf.remaining())*4);
					while(iBuf.hasRemaining()){
						tBuf.putInt(iBuf.get());
						if(!tBuf.hasRemaining()){
							tBuf.flip();
							lhs.write(tBuf, lhs.size());
							tBuf.clear();
						}
					}
					tBuf.flip();
				}
				if(i == 8){
					LongBuffer iBuf = (LongBuffer)((LongBuffer.wrap((long[])editorV.getValue())).duplicate().clear());
					lhs.ensureCapacity(((long)iBuf.remaining())*8);
					while(iBuf.hasRemaining()){
						tBuf.putLong(iBuf.get());
						if(!tBuf.hasRemaining()){
							tBuf.flip();
							lhs.write(tBuf, lhs.size());
							tBuf.clear();
						}
					}
					tBuf.flip();
				}
				lhs.write(tBuf, lhs.size());
				lhs.trimToCapacity();
				dialog.setIconImage(myEditor.get().images[i == 1 ? 7 : 11]);
				dialogButton.setText("Edit content");
				panel.add(dialogButton);break;
			}
			return panel;
		}
		editorV = null;
		return tree.getCellRenderer().
				getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, true);
	}
	public Tag getCurrentEditingNode(){
		return editorV;
	}
	@Override
	public void addCellEditorListener(CellEditorListener l) {
		//
	}
	@Override
	public void removeCellEditorListener(CellEditorListener l) {
		//
	}
}