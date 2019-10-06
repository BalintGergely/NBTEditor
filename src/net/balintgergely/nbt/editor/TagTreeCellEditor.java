package net.balintgergely.nbt.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.EventObject;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.CellEditorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreePath;

import net.balintgergely.nbt.LongHeapSequence;
import net.balintgergely.nbt.NBTType;
import net.balintgergely.nbt.NamedTag;
import net.balintgergely.nbt.Region.Chunk;
import net.balintgergely.nbt.StringUTF8;
import net.balintgergely.nbt.Tag;
import net.balintgergely.nbt.editor.TagEditState.ChunkEditState;
import net.balintgergely.nbt.editor.TagEditState.ListEntryEditState;
import net.balintgergely.nbt.editor.TagEditState.NamedTagEditState;

class TagTreeCellEditor extends JPanel implements TreeCellEditor{
	private static final long serialVersionUID = 1L;
	public static final byte	INTEGER_SIGNED = 0,
								INTEGER_UNSIGNED = 1,
								INTEGER_HEXADECIMAL = 2,
								FLOAT_DECIMAL = 0,
								FLOAT_HEXADECIMAL = 2;
	private byte integerFormat = INTEGER_SIGNED;
	private byte floatingFormat = FLOAT_DECIMAL;
	private TagEditState<?> state;
	private JTextField idA = cfc(new JTextField()),idB = cfc(new JTextField(2));
	private JLabel iconLabel = cfc(new JLabel());
	private JTextArea valueField = new JTextArea(4, 16);
	private JComboBox<String> typeBox = cfc(new JComboBox<>(new String[]{"uncompressed","zlib","gzip"}));
	private JButton hexEditorButton = cfc(new JButton());
	private NBTEditor editor;
	private LongHeapSequence lhs = new LongHeapSequence();
	private HexEditorPanel hexPanel = new HexEditorPanel(lhs);
	private JFileChooser fc = new JFileChooser();
	private JDialog dialog;
	private boolean undoable;
	TagTreeCellEditor(NBTEditor ed) {
		super(null);
		super.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		editor = ed;
		dialog = new JDialog(ed, "HexEditor", true);
		JPanel panel = new JPanel(new GridLayout(1, 0));
		dialog.add(panel,BorderLayout.PAGE_START);
		JButton inportButton = new JButton("Import"),
				exportButton = new JButton("Export"),
				confirmButton = new JButton("Confirm");
		panel.add(inportButton);
		panel.add(exportButton);
		dialog.add(confirmButton, BorderLayout.PAGE_END);
		dialog.add(hexPanel,BorderLayout.CENTER);
		inportButton.addActionListener((ActionEvent e) -> {
			if(fc.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION){
				File f = fc.getSelectedFile();
				if(f != null){
					try{
						lhs.inport(f);
					}catch(IOException ioe){
						ioe.printStackTrace();
						StringWriter sw = new StringWriter();
						ioe.printStackTrace(new PrintWriter(sw));
						JOptionPane.showMessageDialog(dialog, sw, "ERROR", JOptionPane.ERROR_MESSAGE);
					}finally{
						hexPanel.update();
					}
				}
			}
		});
		exportButton.addActionListener((ActionEvent e) -> {
			if(fc.showSaveDialog(dialog) == JFileChooser.APPROVE_OPTION){
				File f = fc.getSelectedFile();
				if(f != null){
					try{
						lhs.export(f);
					}catch(IOException ioe){
						ioe.printStackTrace();
						StringWriter sw = new StringWriter();
						ioe.printStackTrace(new PrintWriter(sw));
						JOptionPane.showMessageDialog(dialog, sw, "ERROR", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		confirmButton.addActionListener((ActionEvent e) -> {
			try{
				synchronized(TagTreeCellEditor.this){
					if(state instanceof ListEntryEditState){
						switch(state.tag.TYPE){
						case BYTE_ARRAY:
							state.setEx(lhs.getContentBytes());
							break;
						case INT_ARRAY:
							state.setEx(lhs.getContentIntegers());
							break;
						case LONG_ARRAY:
							state.setEx(lhs.getContentLongs());
							break;
						default:
							break;
						}
					}
				}
			}catch(Exception | OutOfMemoryError ex){
				ex.printStackTrace();
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				JOptionPane.showMessageDialog(dialog, sw, "ERROR", JOptionPane.ERROR_MESSAGE);
			}finally{
				dialog.dispose();
			}
		});
		hexEditorButton.addActionListener((ActionEvent e) -> {
			try{
				synchronized(TagTreeCellEditor.this){
					if(state instanceof ListEntryEditState){
						switch(state.tag.TYPE){
						case BYTE_ARRAY:lhs.setContent((byte[])state.getValue());break;
						case INT_ARRAY:lhs.setContent((int[])state.getValue());break;
						case LONG_ARRAY:lhs.setContent((long[])state.getValue());break;
						default:return;
						}
						dialog.pack();
						dialog.setVisible(true);
					}
				}
			}catch(Exception | OutOfMemoryError ex){
				ex.printStackTrace();
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				JOptionPane.showMessageDialog(dialog, sw, "ERROR", JOptionPane.ERROR_MESSAGE);
			}
		});
		valueField.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(DocumentEvent e) {
				changedUpdate(e);
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				changedUpdate(e);
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				synchronized(TagTreeCellEditor.this){
					if(state instanceof ListEntryEditState){
						state.setEx(state.tag.TYPE.parse(valueField.getText()));
					}
				}
			}
		});
		idA.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(DocumentEvent e) {
				changedUpdate(e);
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				changedUpdate(e);
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				synchronized(TagTreeCellEditor.this){
					if(state instanceof NamedTagEditState){
						String str = idA.getText();
						state.setName(str);
						idA.setForeground(StringUTF8.isValid(str) ? Color.BLACK : Color.RED);
					}
					if(state instanceof ChunkEditState){
						Integer nt = NBTType.INTEGER.parse(idA.getText());
						idA.setForeground(state.setX(nt == null ? -2 : nt.intValue()) ? Color.BLACK : Color.RED);
					}
				}
			}
		});
		idB.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(DocumentEvent e) {
				changedUpdate(e);
			}
			@Override
			public void removeUpdate(DocumentEvent e) {
				changedUpdate(e);
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				synchronized(TagTreeCellEditor.this){
					if(state instanceof ChunkEditState){
						Integer nt = NBTType.INTEGER.parse(idB.getText());
						idB.setForeground(state.setZ(nt == null ? -2 : nt.intValue()) ? Color.BLACK : Color.RED);
					}
				}
			}
		});
		add(iconLabel);
		add(typeBox);
		typeBox.addActionListener((ActionEvent e) -> {if(state != null)state.setFileType(typeBox.getSelectedIndex());});
		add(idA);
		add(idB);
		add(hexEditorButton);
		valueField.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		add(valueField);
		hideAll();
	}
	private void hideAll(){
		typeBox.setVisible(false);
		idA.setVisible(false);
		idB.setVisible(false);
		hexEditorButton.setVisible(false);
		valueField.setVisible(false);
	}
	private static <E extends JComponent> E cfc(E comp){
		comp.setMaximumSize(new Dimension(0x100,0x10));
		comp.setMinimumSize(new Dimension(0x0,0x10));
		comp.setAlignmentX(0);
		comp.setAlignmentY(0.5f);
		return comp;
	}
	@Override
	public Object getCellEditorValue() {
		return Boolean.valueOf(state instanceof ChunkEditState);
	}
	@Override
	public boolean isCellEditable(EventObject anEvent) {
		if(anEvent == null){
			return true;
		}
		if(anEvent instanceof MouseEvent && ((MouseEvent)anEvent).getClickCount() <= 1){
			return false;
		}
		Object obj = anEvent.getSource();
		if(obj instanceof JTree){
			TreePath pt = ((JTree)obj).getSelectionPath();
			if(pt != null){
				obj = pt.getLastPathComponent();
			}
		}
		if(obj instanceof Tag){
			return isTagEditable((Tag<?>)obj);
		}
		return false;
	}
	public boolean isTagEditable(Tag<?> tg){
		if(tg instanceof FileTreeNode){
			return ((FileTreeNode<?>) tg).hasLoadedNBTData();
		}
		if(tg instanceof NamedTag || tg instanceof Chunk){
			return true;
		}
		return tg.TYPE != NBTType.COMPOUND && tg.TYPE != NBTType.LIST;
	}
	@Override
	public boolean shouldSelectCell(EventObject anEvent) {
		return true;
	}
	@Override
	public synchronized boolean stopCellEditing() {
		if(state == null){
			return true;
		}
		byte b = state.sanitize();
		if(b == -1){
			return false;
		}
		if(b == 1){
			editor.submitUndoableEdit(state,undoable);
		}
		cancelCellEditing();
		return true;
	}
	@Override
	public synchronized void cancelCellEditing() {
		state = null;
		hideAll();
	}
	@SuppressWarnings("hiding")
	private Vector<CellEditorListener> listenerList = new Vector<>();
	@Override
	public void addCellEditorListener(CellEditorListener l) {
		if(l != null){
			listenerList.add(l);
		}
	}
	@Override
	public void removeCellEditorListener(CellEditorListener l) {
		listenerList.remove(l);
	}
	@Override
	public Dimension getMaximumSize(){
		Dimension ps = super.getMaximumSize();
		if(!isMaximumSizeSet()){
			TagEditState<?> st = state;
			if(st != null){
				Tag<?> t = st.tag;
				ps.height = (t != null && t.TYPE == NBTType.STRING) ? 64 : 16;
			}
		}
		return ps;
	}
	@Override
	public synchronized Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
			boolean leaf, int row) {
		Tag<?> tag = (Tag<?>)value;
		if(!isTagEditable(tag)){
			return tree.getCellRenderer().getTreeCellRendererComponent(tree, value, isSelected, expanded, leaf, row, true);
		}
		if(state != null){
			cancelCellEditing();
			state = null;
		}
		undoable = true;
		@SuppressWarnings("hiding")
		TagEditState<?> state = TagEditState.createEditState(tag);
		iconLabel.setIcon(editor.getIconForTag(tag));
		iconLabel.setText("");
		iconLabel.setVisible(true);
		if(tag instanceof NamedTag){
			idA.setColumns(8);
			idA.setText(state.getName());
			idA.setVisible(true);
		}else if(tag instanceof FileTreeNode){
			File f = ((FileTreeNode<?>)tag).getFile();
			if(f == null){
				iconLabel.setText(tag.toString());
			}else{
				iconLabel.setText(f.getName());
			}
			typeBox.setSelectedIndex(state.getFileType());
			typeBox.setVisible(true);
		}else if(tag instanceof Chunk){
			Chunk ch = (Chunk)tag;
			idA.setColumns(2);
			idB.setColumns(2);
			idA.setText(Integer.toString(ch.getX()));
			idA.setVisible(true);
			idB.setText(Integer.toString(ch.getZ()));
			idB.setVisible(true);
		}
		if(tag.TYPE != null){
			switch(tag.TYPE){
			case BYTE:
			case SHORT:
			case INTEGER:
			case LONG:
			case FLOAT:
			case DOUBLE:
			case STRING:
				valueField.setForeground(Color.BLACK);
				valueField.setText(format(state.getValue()));
				valueField.setEditable(true);
				valueField.setVisible(true);
				break;
			case BYTE_ARRAY:
			case INT_ARRAY:
			case LONG_ARRAY:
				hexEditorButton.setText(tag.TYPE.toString(state.getValue()));
				hexEditorButton.setVisible(true);
				break;
			default://The rest of the formats that are not editable.
				valueField.setText(tag.TYPE.toString(tag.getValue()));
				valueField.setRows(1);
				valueField.setColumns(0);
				valueField.setEditable(false);
				valueField.setVisible(true);
				break;
			}
			if(tag.TYPE == NBTType.STRING){
				valueField.setColumns(64);
				valueField.setRows(4);
			}else{
				valueField.setColumns(16);
				valueField.setRows(1);
			}
			valueField.setPreferredSize(null);
			iconLabel.setToolTipText(tag.TYPE.name);
		}else{
			iconLabel.setToolTipText(null);
		}
		this.state = state;
		return this;
	}
	void setUndoable(boolean u){
		undoable = u;
	}
	private String format(Object o){
		if(o instanceof StringUTF8){
			return o.toString();
		}
		Number n = (Number)o;
		if(n instanceof Float){
			switch(floatingFormat){
			case FLOAT_DECIMAL:return n.toString();
			case FLOAT_HEXADECIMAL:return Integer.toHexString(Float.floatToRawIntBits(n.floatValue()));
			default:throw new IllegalStateException();
			}
		}
		if(n instanceof Double){
			switch(floatingFormat){
			case FLOAT_DECIMAL:return n.toString();
			case FLOAT_HEXADECIMAL:return Long.toHexString(Double.doubleToRawLongBits(n.doubleValue()));
			default:throw new IllegalStateException();
			}
		}
		if(integerFormat == INTEGER_SIGNED){
			return n.toString();
		}
		long number;
		if(n instanceof Byte){
			number = Byte.toUnsignedLong(n.byteValue());
		}else if(n instanceof Short){
			number = Short.toUnsignedLong(n.shortValue());
		}else if(n instanceof Integer){
			number = Integer.toUnsignedLong(n.intValue());
		}else{
			number = n.longValue();
		}
		if(integerFormat == INTEGER_UNSIGNED){
			return Long.toUnsignedString(number);
		}
		if(integerFormat == INTEGER_HEXADECIMAL){
			return Long.toHexString(number);
		}
		throw new IllegalStateException();
	}
}
