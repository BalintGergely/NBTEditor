package net.balintgergely.nbt.editor;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.balintgergely.nbt.Compound;
import net.balintgergely.nbt.Container;
import net.balintgergely.nbt.NBTType;
import net.balintgergely.nbt.NamedTag;
import net.balintgergely.nbt.StringUTF8;
import net.balintgergely.nbt.Tag;
import net.balintgergely.nbt.TagList;
import net.balintgergely.nbt.editor.FileTreeNode.RemappableFileTreeNode;
import net.balintgergely.nbt.editor.TagEditState.ChunkEditState;
import net.balintgergely.nbt.editor.TagEditState.TagAREdit;
import net.balintgergely.nbt.editor.TagEditState.TagIndexEdit;

public class NBTEditor extends JFrame{
	private static final long serialVersionUID = 1L;
	public static final int REGION = 13,SAVE = 14,SAVE_AS = 15,LOAD = 16,RELOAD = 17,DELETE = 18,UNDO = 19,REDO = 20,
			UP = 21,DOWN = 22,PENCIL = 23,COPY = 24,PASTE = 25;
	public static final String 
			NEW_COMMAND = "N",
			SAVE_COMMAND = "SV",
			SAVE_AS_COMMAND = "SVS",
			LOAD_COMMAND = "L",
			RELOAD_COMMAND = "RL",
			NEW_REGION_COMMAND = "REGION",
			DELETE_COMMAND = "D",
			UP_COMMAND = "UP",
			DOWN_COMMAND = "DOWN",
			EDIT_COMMAND = "EDT",
			UNDO_COMMAND = "Z",
			REDO_COMMAND = "Y",
			COPY_COMMAND = "C",
			PASTE_COMMAND = "V",
			ADD_PREFIX = "+";
	public static void main(String[] atgs) throws Throwable{
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		final NBTEditor editor = new NBTEditor();
		Compound fileSpace = new Compound(),root = new Compound();
		NamedTag<Compound> cm = new NamedTag<Compound>(NBTType.COMPOUND, StringUTF8.valueOf("root"), root);
		for(NBTType tp : NBTType.values()){
			if(tp != NBTType.END){
				Object val = tp.defaultValue();
				NamedTag<?> tg = new NamedTag<>(tp,StringUTF8.valueOf(tp.name),val);
				if(tp == NBTType.LIST){
					@SuppressWarnings("unchecked")
					TagList<String> sls = (TagList<String>)val;
					sls.add(new Tag<>(NBTType.STRING, "This"));
					sls.add(new Tag<>(NBTType.STRING, "is"));
					sls.add(new Tag<>(NBTType.STRING, "a"));
					sls.add(new Tag<>(NBTType.STRING, "tag"));
					sls.add(new Tag<>(NBTType.STRING, "list"));
				}
				if(tp == NBTType.COMPOUND){
					Compound com = (Compound)val;
					com.add(new NamedTag<>(NBTType.INTEGER, StringUTF8.valueOf("intVal"), Integer.valueOf(17)));
					com.add(new NamedTag<>(NBTType.STRING, StringUTF8.valueOf("stringVal"), "Hello world!"));
				}
				root.add(tg);
			}
		}
		fileSpace.add(cm);
		RemappableFileTreeNode<Compound> testRoot = new RemappableFileTreeNode<>(null);
		testRoot.setValue(fileSpace);
		editor.treeModel.setRoot(testRoot);
	}
	private final ImageIcon[] icons;
	
	private NBTEditor(){
		super("NBTEditor github.com/BalintGergely/NBTEditor");
		try{
			DataInputStream in = new DataInputStream(getClass().getResourceAsStream("NBTEditor$assets.dll")); //$NON-NLS-1$
			icons = new ImageIcon[26];
			int i = 0;
			BufferedImage lz = null;
			while(i < icons.length){
				int w = in.read();
				int z = in.read();
				int h = in.read();
				//System.out.println("READING: "+i+" offset "+z);
				int xo = z/10,yo = z-(xo*10);
				BufferedImage img;
				if(z == 0){
					img = lz = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
				}else{
					img = new BufferedImage(lz.getColorModel(),lz.copyData(null),false,null);
				}
				int x = 0,y;
				while(x < w){
					y = 0;
					while(y < h){
						int rgb = in.readInt();
						//System.out.println((xo+x)+" "+(yo+y));
						if(rgb != 0){
							img.setRGB(xo+x, yo+y, rgb);
						}
						y++;
					}
					x++;
				}
				icons[i] = new ImageIcon(img);
				i++;
			}
			in.close();
		}catch(Throwable e){
			throw new IllegalStateException(e);
		}
		treeModel = new TagTreeModel();
		undoManager = new LightUndoManager(0x100);
		fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		toolBar = new JToolBar();
		add(toolBar,BorderLayout.BEFORE_FIRST_LINE);
		toolBar.setFloatable(false);
		
		ActionListener al = this::actionEvent;
		
toolBar.add(				configButton(al,icons[0],		"New",							NEW_COMMAND));
toolBar.add(				configButton(al,icons[LOAD],	"Select file or directory",		LOAD_COMMAND));
toolBar.add(reloadButton =	configButton(al,icons[RELOAD],	"Reload and discard changes",	RELOAD_COMMAND));
toolBar.add(saveButton =	configButton(al,icons[SAVE],	"Save every modified file",		SAVE_COMMAND));
toolBar.add(saveAsButton =	configButton(al,icons[SAVE_AS],	"Save to a different file",		SAVE_AS_COMMAND));
toolBar.add(regionButton =	configButton(al,icons[REGION],	"Region",						NEW_REGION_COMMAND));

toolBar.addSeparator();

toolBar.add(undoButton =	configButton(al,icons[UNDO],	null,		UNDO_COMMAND));
toolBar.add(redoButton =	configButton(al,icons[REDO],	null,		REDO_COMMAND));
updateButtons();

toolBar.addSeparator();

toolBar.add(				configButton(al,icons[COPY],	"Copy SNBT",COPY_COMMAND));

toolBar.addSeparator();

toolBar.add(removeButton =	configButton(al,icons[DELETE],	"Delete",	DELETE_COMMAND));
toolBar.add(moveUpButton =	configButton(al,icons[UP],		"Move up",	UP_COMMAND));
toolBar.add(moveDownButton =configButton(al,icons[DOWN],	"Move down",DOWN_COMMAND));
toolBar.add(editButton =	configButton(al,icons[PENCIL],	"Edit",		EDIT_COMMAND));
		
toolBar.addSeparator();

		tagButtons = new JButton[12];
		int i = 0;
		while(i < 13){
			if(i > 0){
				String str = NBTType.get(i).toString();
				toolBar.add(tagButtons[i-1] = configButton(al,icons[i],str,ADD_PREFIX+str));
			}
			i++;
		}
		
		toolBar.addSeparator();
		progressBar = new JProgressBar(0, 1);
		progressBar.setValue(1);
		progressBar.setStringPainted(true);
		toolBar.add(progressBar);
		
		tagTree = new JTree(treeModel);
		tagTree.setRootVisible(true);
		tagTree.setCellRenderer(new TagTreeCellRenderer(this));
		tagTree.setCellEditor(editor = new TagTreeCellEditor(this));
		tagTree.setRowHeight(-1);
		tagTree.setEditable(true);
		tagTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tagTree.setInvokesStopCellEditing(true);
		tagTree.addTreeSelectionListener((TreeSelectionEvent e) -> {
			TreePath p = e.getPath();
			if(p.getLastPathComponent() instanceof FileTreeNode){
				loadFile(p);
			}
			tagTree.setSelectionPath(p);
		});
		super.add(new JScrollPane(tagTree),BorderLayout.CENTER);
		super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		super.pack();
		super.setLocationRelativeTo(null);
		super.setVisible(true);
	}
	private JFileChooser fileChooser;
	private JToolBar toolBar;
	@SuppressWarnings("unused")
	private JButton reloadButton,saveButton,saveAsButton,undoButton,redoButton,removeButton,moveUpButton,moveDownButton,editButton,
					regionButton;
	private JProgressBar progressBar;
	private JButton[] tagButtons;
	private JTree tagTree;
	private TagTreeModel treeModel;
	private TagTreeCellEditor editor;
	private LightUndoManager undoManager;
	private ExecutorService executorService = Executors.newSingleThreadExecutor();
	void submitUndoableEdit(TagEditState<?> ed,boolean asUndoable){
		ed.setCachePath(tagTree.getEditingPath());
		if(asUndoable){
			undoManager.addEdit(ed);
		}
		fireUpdateOnEdit(ed,true);
		updateButtons();
	}
	void updateButtons(){
		undoButton.setEnabled(undoManager.canUndo());
		redoButton.setEnabled(undoManager.canRedo());
		FileTreeNode<?> node = treeModel.getRoot();
		reloadButton.setEnabled(node != null);
		saveButton.setEnabled(node != null && node.getModFlag());
		saveAsButton.setEnabled(node instanceof RemappableFileTreeNode);
	}
	public Icon getIconForTag(Tag<?> tag){
		if(tag instanceof FileTreeNode){
			return ((FileTreeNode<?>)tag).getIcon();
		}else{
			return icons[tag.TYPE.typeCode];
		}
	}
	private static JButton configButton(ActionListener ac,Icon icon,String toolTipText,String command){
		JButton button = new JButton(icon);
		button.setToolTipText(toolTipText);
		button.setActionCommand(command);
		button.addActionListener(ac);
		return button;
	}
	void fireUpdateOnEdit(TagEditState<?> ed,boolean done){
		TreePath pt = ed.getCachePath(),at = pt;
		do{
			Object obj = at.getLastPathComponent();
			if(obj instanceof FileTreeNode){
				((FileTreeNode<?>)obj).setModFlag();
			}
			at = at.getParentPath();
		}while(at != null);
		if(ed instanceof TagAREdit){
			if(done != ed.isAddition()){
				treeModel.valueForPathRemoved(pt, ed.getIndexA());
				tagTree.setSelectionPath(pt.getParentPath());
			}else{
				treeModel.valueForPathAdded(pt, ed.getIndexA());
				tagTree.setSelectionPath(pt);
			}
		}if(ed instanceof TagIndexEdit){
			int a = ed.getIndexA(),b = ed.getIndexB();
			//Container<?> cnt = ed.getContainer();
			treeModel.valueForPathRemoved(pt,a);
			treeModel.valueForPathAdded(pt,b);
			tagTree.setSelectionPath(ed.getCachePath());
		}else{
			treeModel.valueForPathChanged(pt, ed instanceof ChunkEditState);
			tagTree.setSelectionPath(pt);
		}
	}
	private void actionEvent(ActionEvent e){
		String str = e.getActionCommand();
		boolean up = true;
		switch(str){
		case NEW_COMMAND:tagTree.stopEditing();
			setRootFile(null);
			break;
		case LOAD_COMMAND:tagTree.stopEditing();
			if(fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION){
				setRootFile(fileChooser.getSelectedFile());
			}break;
		case RELOAD_COMMAND:tagTree.stopEditing();
			FileTreeNode<?> r = treeModel.getRoot();
			r.discard();
			undoManager.removeAllEdits();
			treeModel.treeStructureChanged(new TreeModelEvent(treeModel, new TreePath(r)));
			break;
		case SAVE_AS_COMMAND:up = false;//$FALL-THROUGH$
		case SAVE_COMMAND:
			FileTreeNode<?> node = treeModel.getRoot();
			if(node instanceof RemappableFileTreeNode){
				if(node.getFile() == null){
					up = false;
				}
				if(!up){
					if(fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION){
						return;
					}
					((RemappableFileTreeNode<?>)node).setFile(fileChooser.getSelectedFile());
				}
			}
			node.save();
			break;
		case UNDO_COMMAND:tagTree.stopEditing();
			fireUpdateOnEdit((TagEditState<?>)undoManager.undo(),false);
			break;
		case REDO_COMMAND:tagTree.cancelEditing();
			fireUpdateOnEdit((TagEditState<?>)undoManager.redo(), true);
			break;
		case DELETE_COMMAND:tagTree.stopEditing();
			TreePath pt = tagTree.getSelectionPath();
			if(pt != null){
				TagAREdit<?> ts = TagEditState.createDeleteEdit(pt);
				if(ts != null){
					byte b = ts.sanitize();
					if(b == 1){
						submitUndoableEdit(ts,true);
					}
				}
			}break;
		case COPY_COMMAND:
			pt = tagTree.getSelectionPath();
			Object obj = pt.getLastPathComponent();
			if(obj instanceof Tag){
				Object data = ((Tag<?>)obj).getValue();
				NBTType clazz;
				if(data == null || (clazz = NBTType.forClass(data.getClass())) == null){
					break;
				}
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection selection = new StringSelection(clazz.toSNBT(data));
				clipboard.setContents(selection, selection);
			}
			break;
		case DOWN_COMMAND:up = false;
			//$FALL-THROUGH$
		case UP_COMMAND:tagTree.stopEditing();
			pt = tagTree.getSelectionPath();
			if(pt != null){
				TagEditState<?> ts = TagEditState.createMoveEdit(pt, up);
				if(ts != null){
					byte b = ts.sanitize();
					if(b == 1){
						submitUndoableEdit(ts,true);
					}
				}
			}break;
		case EDIT_COMMAND:tagTree.stopEditing();
			pt = tagTree.getSelectionPath();
			if(pt != null){
				tagTree.startEditingAtPath(pt);
			}break;
		default:tagTree.stopEditing();
			if(str.startsWith(ADD_PREFIX)){
				pt = tagTree.getSelectionPath();
				if(pt != null){
					obj = pt.getLastPathComponent();
					int index = -1;
					if(obj instanceof Tag){
						Tag<?> sub = ((Tag<?>)obj);
						while(!(sub.getValue() instanceof Container)){
							pt = pt.getParentPath();
							if(pt == null){
								return;
							}
							obj = pt.getLastPathComponent();
							if(obj instanceof Tag<?>){
								Object val = ((Tag<?>)obj).getValue();
								if(val instanceof Container){
									index = ((Container<?>)val).indexOf(sub);
									sub = (Tag<?>)obj;
								}else{
									return;
								}
							}
						}
						Container<?> container = (Container<?>)sub.getValue();
						if(index < 0){
							index = container.size();
						}
						NBTType nt = null;
						for(int i = 0;i < NBTType.TYPE_COUNT;i++){
							nt = NBTType.get(i);
							if(str.endsWith(nt.name)){
								break;
							}else{
								nt = null;
							}
						}
						if(nt == null){
							throw new IllegalStateException();
						}
						Tag<Object> tag = container instanceof TagList ? new Tag<>(nt,nt.defaultValue()) : new NamedTag<Object>(nt, StringUTF8.EMPTY, nt.defaultValue());
						pt = pt.pathByAddingChild(tag);
						TagEditState<?> st = TagEditState.createAddEdit(pt, index);
						if(st != null && st.sanitize() == 1){
							submitUndoableEdit(st,true);
							tagTree.startEditingAtPath(pt);
							editor.setUndoable(false);
						}
					}
				}
			}
		}
		updateButtons();
	}
	private void loadFile(TreePath path){
		Object obj = path.getLastPathComponent();
		if(obj instanceof FileTreeNode){
			FileTreeNode<?> tn = (FileTreeNode<?>)obj;
			executorService.execute(() -> {
				if(tn.load()){
					switch(tn.getType()){
					case FileTreeNode.TYPE_REGION:tn.setIcon(icons[REGION]);break;
					case FileTreeNode.TYPE_NBT:
					case FileTreeNode.TYPE_GZIP_NBT:
					case FileTreeNode.TYPE_DEFLATE_NBT:tn.setIcon(icons[10]);
					}
					EventQueue.invokeLater(() -> treeModel.treeStructureChanged(new TreeModelEvent(treeModel, path)));
				}
			});
		}
	}
	private void setRootFile(File f){
		EventQueue.invokeLater(() -> {
			if(f == null || f.isFile()){
				RemappableFileTreeNode<Compound> node = new RemappableFileTreeNode<>(f);
				if(f == null){
					node.setValue(new Compound());
					treeModel.setRoot(node);
				}else{
					treeModel.setRoot(node);
					loadFile(new TreePath(treeModel.getRoot()));
				}
			}else if(f.isDirectory()){
				treeModel.setRoot(new FileTreeNode<>(f));
			}
		});
	}
}
