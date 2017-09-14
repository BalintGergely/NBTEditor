package g;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoableEdit;

import g.io.BaseFileNode;
import g.io.DirectoryNode;
import g.io.FileNode;
import g.io.RemappableFileNode;
import g.nbt.Chunk;
import g.nbt.TagCompound;
import g.nbt.NamedTag;
import g.nbt.Region;
import g.nbt.TagCollection;
import g.nbt.TagList;
import g.nbt.Tag;
import g.swing.TagNodeEditor;
import g.swing.TagTreeModel;
import g.swing.undo.AbstractTagEdit;
import g.swing.undo.Add;
import g.swing.undo.ChunkIdEdit;
import g.swing.undo.MultiTagEdit;
import g.swing.undo.OpenUndoManager;
import g.swing.undo.Remove;
import g.swing.undo.TagSwapEdit;
import g.util.ArrayBuilder;
import g.util.MultiEntryQueue;

public class NBTEditor implements Runnable, ExecutorService{
	private static final int REGION = 13,SAVE = 14,SAVE_AS = 15,LOAD = 16,RELOAD = 17,DELETE = 18,UNDO = 19,REDO = 20,
			UP = 21,DOWN = 22,PENCIL = 23;
	private static final String 
			NEW_COMMAND = "N", //$NON-NLS-1$
			SAVE_COMMAND = "SV", //$NON-NLS-1$
			SAVE_AS_COMMAND = "SVS", //$NON-NLS-1$
			LOAD_COMMAND = "L", //$NON-NLS-1$
			RELOAD_COMMAND = "RL", //$NON-NLS-1$
			DELETE_COMMAND = "D",
			UP_COMMAND = "UP",
			DOWN_COMMAND = "DOWN",
			EDIT_COMMAND = "EDT",//$NON-NLS-1$
			UNDO_COMMAND = "Z", //$NON-NLS-1$
			REDO_COMMAND = "Y", //$NON-NLS-1$
			ADD_PREFIX = "+", //$NON-NLS-1$
			REGION_SUFFIX = "REGION"; //$NON-NLS-1$
	@SuppressWarnings("nls")
	public static void main(String[] atgs) throws Throwable{
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		final NBTEditor editor = new NBTEditor();
		for(String str : atgs){
			switch(str){
			case "refreshbt":
				editor.addTask(0,() -> {
					JButton b = new JButton("Refresh");
					editor.toolBar.add(b);
					b.addActionListener((ActionEvent e) -> editor.treeModel.reload());
				});
			case "demo": //$NON-NLS-1$
				if(editor.root == null){
					RemappableFileNode root = new RemappableFileNode("Test",128); //$NON-NLS-1$
					root.setState(2);
					Chunk node = ((Region)root.getValue()).createChunk("",10,0);
					TagCompound comp = (TagCompound)node.getValue();
					int i = 1;
					while(i < 13){
						NamedTag tN = new NamedTag(Tag.getTypeName(i),i);
						if(i == 9){
							TagList ls = (TagList)tN.getValue();
							ls.add(new Tag("A")); //$NON-NLS-1$
							ls.add(new Tag("B")); //$NON-NLS-1$
							ls.add(new Tag("C")); //$NON-NLS-1$
						}
						if(i == 10){
							TagCompound com = (TagCompound)tN.getValue();
							com.add(new NamedTag("A",new Integer(0x7fffffff))); //$NON-NLS-1$
							com.add(new NamedTag("B",new Double(0.5))); //$NON-NLS-1$
							com.add(new NamedTag("aäáeéoöóõuüúû",new String("AÄÁEÉOÖÓÕUÜÚÛ"))); //$NON-NLS-1$ //$NON-NLS-2$
						}
						comp.add(tN);
						i++;
					}
					editor.root = root;
					editor.modsSinceSave = 1;
				}
				break;
			default:
				if(editor.root == null){
					File f = new File(str);
					if(f.exists()){
						editor.root = createForFile(f);
					}
				}
			}
		}
		int errors = 0;
		try{
			editor.run();
		}catch(Throwable e){
			e.printStackTrace();
			errors = 1;
		}
		System.exit(editor.lastRunInfo+errors);
	}
	@SuppressWarnings("unchecked")
	public static <E extends Throwable> void lazyThrow(Throwable e) throws E{
		throw (E)e;
	}
	public static BaseFileNode createForFile(File file){
		if(file.isFile()){
			return new RemappableFileNode(file);
		}
		return new DirectoryNode(file);
	}
	public NBTEditor() {
		try{
			DataInputStream in = new DataInputStream(getClass().getResourceAsStream("NBTEditor$assets.dll")); //$NON-NLS-1$
			images = new BufferedImage[24];
			int i = 0,lz = 0;
			while(i < images.length){
				int w = in.read();
				int z = in.read();
				int h = in.read();
				int xo = z >> 4,yo = z & 0xf;
				if(z == 0){
					lz = i;
					images[i] = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
				}else{
					images[i] = new BufferedImage(images[lz].getColorModel(),images[lz].copyData(null),false,null);
				}
				int x = 0,y;
				while(x < w){
					y = 0;
					while(y < h){
						int a = in.read();
						if(a != 0){
							images[i].setRGB(xo+x, yo+y, a << 24 | in.read() << 16 | in.read() << 8 | in.read());
						}
						y++;
					}
					x++;
				}
				i++;
			}
		}catch(Throwable e){
			throw new IllegalStateException(e);
		}
		taskList = new MultiEntryQueue<>(2);
		listener = new Listener();
		undoManager = new OpenUndoManager();
		undoManager.setLimit(256);
	}
	private class Listener implements WindowListener,UndoableEditListener,TreeSelectionListener,KeyListener,TreeWillExpandListener{
		@Override
		public void windowOpened(WindowEvent e) {}
		@Override
		public void windowClosing(WindowEvent e) {}
		@Override
		public void windowClosed(WindowEvent e) {
			shutdown();
		}
		@Override
		public void windowIconified(WindowEvent e) {}
		@Override
		public void windowDeiconified(WindowEvent e) {}
		@Override
		public void windowActivated(WindowEvent e) {}
		@Override
		public void windowDeactivated(WindowEvent e) {}
		@Override
		public void undoableEditHappened(UndoableEditEvent e) {
			undoManager.undoableEditHappened(e);
			submitUpdate();
		}
		@Override
		public void valueChanged(TreeSelectionEvent e) {
			TreePath[] paths = tree.getSelectionPaths();
			if(paths != null){
				for(TreePath path : paths){
					Object node = path.getLastPathComponent();
					if(node instanceof FileNode && !(node instanceof RemappableFileNode)){
						handleFile((FileNode)node,true);
					}
				}
			}
			submitUpdate();
		}
		@Override
		public void keyTyped(KeyEvent e) {
		}
		@Override
		public void keyPressed(KeyEvent e) {
		}
		@Override
		public void keyReleased(KeyEvent e) {
			Object o = e.getSource();
			if(e.getKeyCode() == KeyEvent.VK_ENTER && e.getSource() == tree){
				actionEvent(new ActionEvent(o, e.getID(), EDIT_COMMAND));
			}
		}
		@Override
		public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
			TreePath p = event.getPath();
			if(p != null){
				Object o = p.getLastPathComponent();
				if(o instanceof FileNode){
					handleFile((FileNode)o,true);
				}
				if(o instanceof DirectoryNode){
					DirectoryNode n = (DirectoryNode)o;
					addTask(0,() -> {
						n.ensureChildren();
						Enumeration<BaseFileNode> nodes = n.children();
						while(nodes.hasMoreElements()){
							BaseFileNode node = nodes.nextElement();
							if(node instanceof FileNode){
								handleFile((FileNode)node,false);
							}
						}
					});
				}
			}
		}
		@Override
		public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
			//TreeCellRenderer will handle this. We don't always receive an event when this happens.
		}
	}
	private final Listener listener;
	
	public final BufferedImage[] images;
	
	private JFrame frame;
	private JFileChooser fileChooser;
	private JDialog dialog;
	private JTree tree;
	private JButton reloadButton,saveButton,saveAsButton,undoButton,redoButton,removeButton,moveUpButton,moveDownButton,
		editButton,regionButton,tagButtons[];
	private TagTreeModel treeModel;
	private BaseFileNode root;
	private DefaultTreeCellRenderer cellRenderer;
	private LookAndFeel laf;
	private TagNodeEditor tagEditor;
	private JToolBar toolBar;
	private JProgressBar progressBar;
	private MultiEntryQueue<Runnable> taskList;
	
	protected OpenUndoManager undoManager;
	
	private boolean signal = true;
	private int modsSinceSave = 0;
	private int lastRunInfo;
	private Thread mainThread;
	@SuppressWarnings("nls")
	@Override
	public void run(){
		Thread mT = Thread.currentThread();
		try{
			synchronized(taskList){
				if(mainThread != null){
					return;
				}
				mainThread = mT;
				lastRunInfo = 0;
				EventQueue.invokeAndWait(() -> {
					frame = new JFrame("NBTEditor");
					frame.setLayout(new BorderLayout());
					frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					frame.addWindowListener(listener);
					
					dialog = new JDialog(frame);
					dialog.setModal(true);
					fileChooser = new JFileChooser();
					fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
					
					tree = new JTree(treeModel = new TagTreeModel());
					tagEditor = new TagNodeEditor(frame,fileChooser,this);
					tree.setCellEditor(tagEditor);
					tree.addTreeWillExpandListener(listener);
					tree.addTreeSelectionListener(listener);
					tree.addKeyListener(listener);
					tagEditor.addUndoableEditListener(listener);
					tree.setInvokesStopCellEditing(true);
					tree.setExpandsSelectedPaths(true);
					tree.setEditable(true);
					frame.add(new JScrollPane(tree),BorderLayout.CENTER);
					tree.setFocusable(true);
					tree.setRowHeight(17);
					
					cellRenderer = new DefaultTreeCellRenderer();
					
					//Note that treeCellRenderer handles nodes that are closed.
					
					tree.setCellRenderer((JTree tree0, Object value,
							boolean sel, boolean expanded,
							boolean leaf, int row, boolean hasFocus) -> {
								if(value instanceof Tag){
									Tag node = (Tag)value;
									int type = node.getTypeCode();
									if(type != 0){
										//DefaultTreeCellRenderer returns itself as a component.
										Component comp = cellRenderer.getTreeCellRendererComponent
												(tree0, value, sel, false, true, row, hasFocus);
										if(type < 128){
											if(expanded && (node.getChildCount() > 0)){
												cellRenderer.setIcon(new ImageIcon(images[0]));
											}else{
												cellRenderer.setIcon(new ImageIcon(images[type]));
											}
										}else if(type == 128){
											cellRenderer.setIcon(new ImageIcon(images[REGION]));
										}else{
											cellRenderer.setIcon(null);
										}
										return comp;
									}
								}
								Icon ic = null;
								if(value instanceof FileNode){
									ic = FileSystemView.getFileSystemView().getSystemIcon(((FileNode)value).getFile());
								}else if(value instanceof DirectoryNode){
									if(!expanded){
										((DirectoryNode)value).weakChildren();
									}
									ic = FileSystemView.getFileSystemView().getSystemIcon(((DirectoryNode)value).file);
								}
								if(ic == null){
									//TreeCellRenderer still has it's default icons.
									return cellRenderer.getTreeCellRendererComponent
											(tree0, value, sel, expanded, leaf, row, hasFocus);
								}
								Component comp = cellRenderer.getTreeCellRendererComponent
										(tree0, value, sel, false, true, row, hasFocus);
								cellRenderer.setIcon(ic);
								return comp;
							});
					toolBar = new JToolBar();
					frame.add(toolBar,BorderLayout.BEFORE_FIRST_LINE);
					toolBar.setFloatable(false);
					
					ActionListener al = this::actionEvent;
					
		toolBar.add(				configButton(al,images[0],		"New",							NEW_COMMAND));
		toolBar.add(				configButton(al,images[LOAD],	"Select folder or directory",	LOAD_COMMAND));
		toolBar.add(reloadButton =	configButton(al,images[RELOAD],	"Reload and discard changes",	RELOAD_COMMAND));
		toolBar.add(saveButton =	configButton(al,images[SAVE],	"Save every modified file",		SAVE_COMMAND));
		toolBar.add(saveAsButton =	configButton(al,images[SAVE_AS],"Save to a different file",		SAVE_AS_COMMAND));
		
		toolBar.addSeparator();
		
		toolBar.add(undoButton =	configButton(al,images[UNDO],	null,		UNDO_COMMAND));
		toolBar.add(redoButton =	configButton(al,images[REDO],	null,		REDO_COMMAND));
		
		toolBar.addSeparator();
		
		toolBar.add(removeButton =	configButton(al,images[DELETE],	"Delete",	DELETE_COMMAND));
		toolBar.add(moveUpButton =	configButton(al,images[UP],		"Move up",	UP_COMMAND));
		toolBar.add(moveDownButton =configButton(al,images[DOWN],	"Move down",DOWN_COMMAND));
		toolBar.add(editButton =	configButton(al,images[PENCIL],	"Edit",		EDIT_COMMAND));
					
		toolBar.addSeparator();
		
					tagButtons = new JButton[12];
					int i = 0;
					while(i < 12){
						String str = Tag.getTypeName(i+1);
						toolBar.add(tagButtons[i] = configButton(al,images[i+1],"TAG_"+str,ADD_PREFIX+str));
						i++;
					}

		toolBar.add(regionButton = configButton(al,images[REGION],"Region",ADD_PREFIX+REGION_SUFFIX));
					
					toolBar.addSeparator();
					progressBar = new JProgressBar(0, 1);
					progressBar.setValue(1);
					progressBar.setStringPainted(true);
					toolBar.add(progressBar);
					frame.pack();
					frame.setSize(700, 500);
					frame.setLocationRelativeTo(null);
					frame.setVisible(true);
					laf = UIManager.getLookAndFeel();
				});
			}
			update();
			System.out.println("Initialised");
			int taskC = 0;
			while(signal || !taskList.isEmpty()){
				Runnable rn;
				synchronized(taskList){
					if(!taskList.isEmpty()){
						progressBar.setMaximum(taskC+taskList.size());
						rn = taskList.poll();
					}else{
						taskC = 0;
						taskList.wait();
						continue;
					}
				}
				rn.run();
				taskC++;
				progressBar.setValue(taskC);
			}
		} catch (InterruptedException e) {
			if(signal){
				Thread.currentThread().interrupt();
			}
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}finally{
			frame.dispose();
			tagEditor.removeUndoableEditListener(undoManager);
			undoManager.discardAllEdits();
			tagEditor = null;
			frame = null;
			fileChooser = null;
			dialog = null;
			tree = null;
			reloadButton = null;
			toolBar = null;
			treeModel = null;
			cellRenderer = null;
			synchronized(this){
				mainThread = null;
				notifyAll();
			}
		}
	}
	private static JButton configButton(ActionListener ac,BufferedImage image,String toolTipText,String command){
		JButton button = new JButton(new ImageIcon(image));
		button.setToolTipText(toolTipText);
		button.setActionCommand(command);
		button.addActionListener(ac);
		return button;
	}
	@Override
	public synchronized void shutdown(){
		signal = false;
		if(mainThread != null){
			mainThread.interrupt();
		}
	}
	public void submitUpdate(){
		addTask(0,this::update);
	}
	/**
	 * WARNING!!! Never invoke under the taskList lock!
	 */
	private void update(){
		LookAndFeel l = UIManager.getLookAndFeel();
		if(!Objects.equals(laf, l) || laf == l){//This might abuse UIManager. Better be careful.
			cellRenderer = new DefaultTreeCellRenderer();
			laf = l;
		}
		if(treeModel.getRoot() != root){
			treeModel.setRoot(root);
		}
		compEN(reloadButton,root != null && root.getFile() != null);
		boolean canSave = root != null && modsSinceSave != 0;
		compEN(saveButton,canSave && root.getFile() != null);
		compEN(saveAsButton,canSave && root instanceof RemappableFileNode);
		compEN(undoButton,undoManager.canUndo());
		undoButton.setToolTipText(undoManager.getUndoPresentationName());
		compEN(redoButton,undoManager.canRedo());
		redoButton.setToolTipText(undoManager.getRedoPresentationName());
		boolean ab = containsPN(tree.getSelectionPaths());
		compEN(moveUpButton,ab);
		compEN(moveDownButton,ab);
		if(root == null){
			compEN(removeButton,false);
			compEN(regionButton,true);
		}else{
			compEN(regionButton,false);
			TreePath path = tree.getSelectionPath();
			a: if(path != null){
				Object o = path.getLastPathComponent();
				if(o instanceof Tag){
					compEN(editButton,true);
					Tag node = (Tag)o,d = node;
					if(!node.getAllowsChildren()){
						TreeNode parent = node.getParent();
						if(!(parent instanceof Tag)){
							break a;
						}
						node = (Tag)parent;
					}
					compEN(removeButton,d != root);
					if(node.getTypeCode() == 9){
						buttonEN(((TagList)node.getValue()).getTypeCode());
					}else{
						buttonEN(0);
					}
					return;
				}
			}
			compEN(removeButton,false);
			buttonEN(-1);
		}
		compEN(editButton,false);
	}
	private static boolean containsPN(TreePath[] o){
		if(o != null){
			for(TreePath nod : o){
				Object obj = nod.getLastPathComponent();
				if(obj instanceof Tag && !(obj instanceof BaseFileNode)){
					return true;
				}
			}
		}
		return false;
	}
	private static void compEN(JComponent comp,boolean en){
		if(comp.isEnabled() != en){
			comp.setEnabled(en);
		}
	}
	private void buttonEN(int p){
		int i = 0;
		while(i < tagButtons.length){
			compEN(tagButtons[i],i+1 == p || p == 0);
			i++;
		}
	}
	/**
	 * Returns true if saved what's save able
	 * @param as
	 * @return
	 */
	@SuppressWarnings("nls")
	private boolean save(boolean as){
		BaseFileNode nod = root;
		if(as || nod.getFile() == null){
			if(!(nod instanceof RemappableFileNode)){
				modsSinceSave = 0;
				return true;
			}
			if(fileChooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION){
				return false;
			}
			((RemappableFileNode)nod).setFile(fileChooser.getSelectedFile());
		}
		if(!nod.saveWave()){
			JOptionPane.showMessageDialog(frame,"An error has occurred during saving.", "ERROR", JOptionPane.ERROR_MESSAGE);
			lastRunInfo++;
			return false;
		}
		modsSinceSave = 0;
		return true;
	}
	@SuppressWarnings("nls")
	private boolean savePrompt(String title){
		switch(JOptionPane.showConfirmDialog(frame,
				"Save unsaved changes?", title, JOptionPane.YES_NO_CANCEL_OPTION)){
		case JOptionPane.YES_OPTION:
			if(!save(false)){
				return false;
			}
			//$FALL-THROUGH$
		case JOptionPane.NO_OPTION:
			return true;
		default:
			return false;
		}
	}
	private void registerNewEdit(){
		if(modsSinceSave < 0){
			modsSinceSave = undoManager.numberOfEdits()+1;
		}else{
			modsSinceSave++;
		}
	}
	@SuppressWarnings({ "nls", "unchecked" })
	private void actionEvent(ActionEvent e){
		String cmd = e.getActionCommand();
		addTask(0,() -> {
			switch(cmd){
			case NEW_COMMAND:
				if(undoManager.canUndoOrRedo()){
					if(!savePrompt("New")){
						return;
					}
				}
				undoManager.discardAllEdits();
				root = null;
				break;
			case SAVE_COMMAND:
				save(false);
				break;
			case SAVE_AS_COMMAND:
				save(true);
				break;
			case LOAD_COMMAND:
				if(fileChooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION){
					return;
				}
				if(undoManager.canUndoOrRedo()){
					if(!savePrompt("Load")){
						return;
					}
				}
				undoManager.discardAllEdits();
				modsSinceSave = 0;
				root = createForFile(fileChooser.getSelectedFile());break;
			case RELOAD_COMMAND:
				if(undoManager.canUndoOrRedo()){
					if(!savePrompt("Reload")){
						return;
					}
				}
				if(root.getFile() == null){
					return;
				}
				File f;
				if(root instanceof FileNode){
					f = ((FileNode) root).getFile();
				}else if(root instanceof DirectoryNode){
					f = ((DirectoryNode) root).file;
				}else{
					return;
				}
				undoManager.discardAllEdits();
				modsSinceSave = 0;
				root = createForFile(f);break;
			case UNDO_COMMAND:
				if(undoManager.canUndo()){
					UndoableEdit edit = undoManager.editToBeUndone();
					undoManager.undo();
					modsSinceSave--;
					tree.clearSelection();
					TreePath path = reloadTagsOfEdit(edit,false);
					if(path != null){
						tree.setSelectionPath(path);
					}
				}break;
			case REDO_COMMAND:
				if(undoManager.canRedo()){
					UndoableEdit edit = undoManager.editToBeRedone();
					undoManager.redo();
					modsSinceSave++;
					tree.clearSelection();
					TreePath path = reloadTagsOfEdit(edit,true);
					if(path != null){
						tree.setSelectionPath(path);
					}
				}break;
			case DELETE_COMMAND:
				TreePath[] paths = tree.getSelectionPaths();
				if(paths == null || paths.length == 0){
					return;
				}
				ArrayBuilder<AbstractTagEdit> ab = new ArrayBuilder<>(AbstractTagEdit.class);
				for(TreePath path : tree.getSelectionPaths()){
					if(path == null){
						continue;
					}
					Object o = path.getLastPathComponent();
					if(o.equals(root)){
						continue;
					}
					if(o instanceof Tag && !(o instanceof BaseFileNode)){
						Tag node = (Tag)o,parent = (Tag)node.getParent();
						int index = parent.getIndex(node);
						parent.remove(node);
						ab.accept(new Remove(node,index));
						registerNewEdit();
						treeModel.reload(parent);
						tree.setSelectionPath(new TreePath(treeModel.getPathToRoot(parent)));
					}
				}
				undoManager.addEdit(MultiTagEdit.combine(ab.get()));
				break;
			case UP_COMMAND:
				paths = tree.getSelectionPaths();
				if(paths == null || paths.length == 0){
					return;
				}
				Arrays.sort(paths,(TreePath a,TreePath b) -> {
					int i = Integer.compare(a.getPathCount(),b.getPathCount());
					if(i != 0){
						return i;
					}
					i = a.getPathCount()-1;
					while(i > 0){
						Object ac = a.getPathComponent(i),bc = b.getPathComponent(i);
						i--;
						Object ap = a.getPathComponent(i),bp = b.getPathComponent(i);
						if(ac instanceof TreeNode && bc instanceof TreeNode && ap instanceof TreeNode && bp instanceof TreeNode){
							int aindex = ((TreeNode)ap).getIndex((TreeNode) ac),bindex = ((TreeNode)bp).getIndex((TreeNode)bc);
							if(aindex == -1 || bindex == -1){
								return 0;
							}
							if(aindex != bindex){
								return Integer.compare(aindex,bindex);
							}
						}else{
							return 0;
						}
					}
					return 0;
				});
				ab = new ArrayBuilder<AbstractTagEdit>(AbstractTagEdit.class);
				int i = 0;
				while(i < paths.length){
					Object o = paths[i].getLastPathComponent();
					a: if(o instanceof Tag && !(o instanceof FileNode)){
						Tag node = (Tag)o,parent = (Tag)node.getParent();
						int index = parent.getIndex(node);
						if(index < 0){
							break a;
						}
						if(index == 0){
							if(parent instanceof FileNode){
								break a;
							}
							Tag grandParent = (Tag)parent.getParent(),newNode;
							TagCollection<? super Chunk> c = (TagCollection<? super Chunk>)grandParent.getValue();
							int newIndex = grandParent.getIndex(parent);
							if(newIndex < 0){
								break a;
							}
							String name = node instanceof NamedTag ? ((NamedTag)node).getName() : "";
							if((c instanceof TagCompound) == (node instanceof NamedTag)){
								newNode = TagCollection.cloneTag(node);
							}else{
								if(c instanceof TagList){
									newNode = new Tag(null);
								}else if(c instanceof Region){
									newNode = ((Region) c).createChunk(name, index);
								}else{
									newNode = new NamedTag(name,null);
								}
								newNode.setValue(TagCollection.cloneValue(node.getValue(), newNode));
							}
							index = grandParent.getIndex(parent);
							parent.remove(node);
							treeModel.reload(parent);
							ab.accept(new Remove(node,0));
							((TagCollection<Tag>)c).add(index,newNode);
							treeModel.reload(grandParent);
							if(c instanceof Region){
								index = ((Region)c).indexOf(newNode);
							}
							ab.accept(new Add(newNode,index));
							paths[i] = new TreePath(treeModel.getPathToRoot(newNode));
						}else{
							((TagCollection<?>)parent.getValue()).swap(index, index-1);
							treeModel.reload(parent);
							ab.accept(new TagSwapEdit(parent,index,index-1));
						}
					}
					i++;
				}
				tree.setSelectionPaths(paths);
				if(ab.size() > 0){
					undoManager.addEdit(MultiTagEdit.combine(ab.get()));
				}
				break;
			case DOWN_COMMAND:
				paths = tree.getSelectionPaths();
				if(paths == null || paths.length == 0){
					return;
				}
				Arrays.sort(paths,(TreePath a,TreePath b) -> {
					int r = Integer.compare(a.getPathCount(),b.getPathCount());
					if(r != 0){
						return r;
					}
					r = a.getPathCount()-1;
					while(r > 0){
						Object ac = a.getPathComponent(r),bc = b.getPathComponent(r);
						r--;
						Object ap = a.getPathComponent(r),bp = b.getPathComponent(r);
						if(ac instanceof TreeNode && bc instanceof TreeNode && ap instanceof TreeNode && bp instanceof TreeNode){
							int aindex = ((TreeNode)ap).getIndex((TreeNode) ac),bindex = ((TreeNode)bp).getIndex((TreeNode)bc);
							if(aindex == -1 || bindex == -1){
								return 0;
							}
							if(aindex != bindex){
								return Integer.compare(bindex,aindex);
							}
						}else{
							return 0;
						}
					}
					return 0;
				});
				ab = new ArrayBuilder<AbstractTagEdit>(AbstractTagEdit.class);
				i = 0;
				while(i < paths.length){
					Object o = paths[i].getLastPathComponent();
					a: if(o instanceof Tag && !(o instanceof FileNode)){
						Tag node = (Tag)o,parent = (Tag)node.getParent();
						int index = parent.getIndex(node);
						if(index < 0){
							break a;
						}
						int note = parent.getChildCount()-1;
						if(index == note){
							if(parent instanceof FileNode){
								break a;
							}
							Tag grandParent = (Tag)parent.getParent(),newNode;
							TagCollection<? super Chunk> c = (TagCollection<? super Chunk>)grandParent.getValue();
							int newIndex = grandParent.getIndex(parent);
							if(newIndex < 0){
								break a;
							}
							String name = node instanceof NamedTag ? ((NamedTag)node).getName() : "";
							if((c instanceof TagCompound) == (node instanceof NamedTag)){
								newNode = TagCollection.cloneTag(node);
							}else{
								if(c instanceof TagList){
									newNode = new Tag(null);
								}else if(c instanceof Region){
									newNode = ((Region) c).createChunk(name, index);
								}else{
									newNode = new NamedTag(name,null);
								}
								newNode.setValue(TagCollection.cloneValue(node.getValue(), newNode));
							}
							index = grandParent.getIndex(parent)+1;
							parent.remove(node);
							treeModel.reload(parent);
							ab.accept(new Remove(node,note));
							((TagCollection<Tag>)c).add(index,newNode);
							treeModel.reload(grandParent);
							if(c instanceof Region){
								index = ((Region)c).indexOf(newNode);
							}
							ab.accept(new Add(newNode,index));
							paths[i] = new TreePath(treeModel.getPathToRoot(newNode));
						}else{
							((TagCollection<?>)parent.getValue()).swap(index, index+1);
							treeModel.reload(parent);
							ab.accept(new TagSwapEdit(parent,index,index+1));
						}
					}
					i++;
				}
				tree.setSelectionPaths(paths);
				if(ab.size() > 0){
					undoManager.addEdit(MultiTagEdit.combine(ab.get()));
				}
				break;
			case EDIT_COMMAND:
				TreePath selection = tree.getSelectionPath();
				if(selection == null){
					return;
				}
				Object o = selection.getLastPathComponent();
				if(o instanceof Tag){
					tree.startEditingAtPath(selection);
				}else{
					return;
				}
				break;
			default:if(cmd.startsWith(ADD_PREFIX)){
					int type = Tag.getTypeCode(cmd.substring(ADD_PREFIX.length()));
					if(root == null){
						if(type != 0){
							root = new RemappableFileNode("",type);
						}else if(cmd.substring(ADD_PREFIX.length()).equals(REGION_SUFFIX)){
							RemappableFileNode node = new RemappableFileNode("",null);
							node.setValue(new Region(node));
							root = node;
						}else{
							return;
						}
						break;
					}
					if(type == 0){
						return;
					}
					Object obj = tree.getSelectionPath().getLastPathComponent();
					if(obj instanceof Tag){
						Tag node = (Tag)obj;
						int index = 0;
						if(!node.getAllowsChildren()){
							TreeNode parent = node.getParent();
							if(!(parent instanceof Tag)){
								return;
							}
							index = parent.getIndex(node)+1;
							node = (Tag)parent;
						}
						Tag newNode;
						int t = node.getTypeCode();
						if(t == 9){
							int subType = ((TagList)node.getValue()).getTypeCode();
							if(subType != 0 && subType != type){
								return;
							}
							node.insert(newNode = new Tag(type), index);
						}else if(node.getTypeCode() == 10){
							node.insert(newNode = new NamedTag("", type), index); //$NON-NLS-1$
						}else if(node.getTypeCode() == 128){
							newNode = ((Region)node.getValue()).createChunk("", type, index);
							if(newNode == null){
								return;
							}
							index = ((Region)node.getValue()).indexOf(newNode);
						}else{
							return;
						}
						treeModel.reload(node);
						tree.setSelectionPath(new TreePath(treeModel.getPathToRoot(newNode)));
						registerNewEdit();
						undoManager.addEdit(new Add(newNode,index));
					}
				}else{
					return;
				}
			}
			update();
		});
	}
	private TreePath reloadTagsOfEdit(UndoableEdit edit,boolean redo){
		if(edit instanceof MultiTagEdit){
			MultiTagEdit e = (MultiTagEdit)edit;
			ArrayBuilder<TreePath> ab = new ArrayBuilder<>(TreePath.class);
			for(AbstractTagEdit ed : e){
				TreePath pat = reloadTagsOfEdit(ed,redo);
				if(pat != null){
					ab.accept(pat);
				}
			}
			tree.setSelectionPaths(ab.get());
		}
		if(edit instanceof AbstractTagEdit){
			Tag node = ((AbstractTagEdit)edit).getNode();
			if(edit instanceof ChunkIdEdit || edit instanceof Remove || edit instanceof Add){
				TreeNode p = node.getParent();
				if(p != null){
					treeModel.reload(p);
					if(redo ? edit instanceof Remove : edit instanceof Add){
						TreeNode[] nodes = treeModel.getPathToRoot(p);
						if(nodes != null){
							TreePath path = new TreePath(nodes);
							tree.expandPath(path);
							return path;
						}
					}
				}
			}else{
				treeModel.reload(node);
			}
			TreeNode[] nodes = treeModel.getPathToRoot(node);
			if(nodes != null){
				return new TreePath(nodes);
			}
		}
		return null;
	}
	public void updateModel(TreeNode node){
		EventQueue.invokeLater(() -> {
			treeModel.reload(node);
		});
	}
	private void addTask(int endPoint,Runnable task){
		Objects.requireNonNull(task);
		synchronized(taskList){
			taskList.setEndPoint(endPoint);
			taskList.add(task);
			taskList.notifyAll();
		}
	}
	@Override
	public void execute(Runnable command) {
		addTask(1,() -> {
			try{
				command.run();
			}catch(Throwable e){
				//
			}
		});
	}
	public void execute(Callable<?> callable){
		addTask(1,() -> {
			try{
				callable.call();
			}catch(Throwable e){
				//
			}
		});
	}
	private void handleFile(FileNode node,boolean expanded){
		if(node.getState() == -1){
			return;
		}
		addTask(expanded ? 0 : 1,() -> {
			if(node.getState() == 0){
				node.scan();
			}
			if(node.getState() > 0 && !node.isLoaded()){
				node.load();
			}
			treeModel.reload(node);
		});
	}
	@Override
	public synchronized List<Runnable> shutdownNow() {
		synchronized(taskList){
			ArrayList<Runnable> alist = new ArrayList<>(taskList);
			signal = false;
			taskList.clear();
			taskList.notifyAll();
			return alist;
		}
	}
	@Override
	public boolean isShutdown() {
		return !signal;
	}
	@Override
	public boolean isTerminated() {
		return !signal && mainThread == null;
	}
	@Override
	public synchronized boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		unit.timedWait(this, timeout);
		return isTerminated();
	}
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> ftask = new FutureTask<>(task);
		addTask(1,ftask);
		return ftask;
	}
	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		FutureTask<T> ftask = new FutureTask<>(task, result);
		addTask(1,ftask);
		return ftask;
	}
	@Override
	public Future<?> submit(Runnable task) {
		return submit(task,null);
	}
	public <T> List<Future<T>> submitAll(Collection<? extends Callable<T>> tasks1){
		ArrayList<Future<T>> list = new ArrayList<>(tasks1.size());
		synchronized(taskList){
			for(Callable<T> c : tasks1){
				if(c != null){
					list.add(submit(c));
				}else{
					list.add(null);
				}
			}
		}
		return list;
	}
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks0) throws InterruptedException {
		return invokeAll(tasks0,0,null);
	}
	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks0, long timeout, TimeUnit unit)
			throws InterruptedException {
		List<Future<T>> list = submitAll(tasks0);
		int i = list.size();
		while(i > 0){
			i--;
			Future<T> f = list.get(i);//Take the last element...
			if(f != null){
				try {
					if(unit != null){
						f.get(timeout, unit);
					}else{
						f.get();
					}
				} catch (CancellationException | ExecutionException e) {
					//
				} catch (InterruptedException | TimeoutException e){
					f.cancel(false);
					synchronized(taskList){
						taskList.removeAll(list);
					}
					while(i > 0){
						i--;
						f = list.get(i);
						if(f != null){
							f.cancel(false);
						}
					}
					if(e instanceof InterruptedException){
						throw (InterruptedException)e;
					}
				}
				break;
			}
		}
		return list;
	}
	/**
	 * This method returns <code>submit(c).get()</code> where c is the first non-null element in the collection.
	 * Returns null if there were no non-null elements in the collection.
	 */
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks0) throws InterruptedException, ExecutionException {
		for(Callable<T> c : tasks0){
			if(c != null){
				return submit(c).get();
			}
		}
		return null;
	}
	/**
	 * This method returns <code>submit(c).get(timeout,unit)</code> where c is the first non-null element in the collection.
	 * Returns null if there were no non-null elements in the collection
	 */
	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks0, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		for(Callable<T> c : tasks0){
			if(c != null){
				return submit(c).get(timeout, unit);
			}
		}
		return null;
	}
}
