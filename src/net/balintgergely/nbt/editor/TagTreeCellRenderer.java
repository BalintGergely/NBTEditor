package net.balintgergely.nbt.editor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.TreeCellRenderer;

import net.balintgergely.nbt.Tag;

class TagTreeCellRenderer extends JLabel implements TreeCellRenderer{
	private static final long serialVersionUID = 1L;
	private NBTEditor editor;
	TagTreeCellRenderer(NBTEditor editor0) {
		editor = editor0;
		super.setBackground(new Color(0x5555ff));
	}
	@Override
	public Dimension getPreferredSize(){
		Dimension dim = super.getPreferredSize();
		if(!isPreferredSizeSet()){
			dim.height = 16;
		}
		return dim;
	}
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {
		if(value instanceof Tag){
    		Tag<?> t = (Tag<?>)value;
    		setIcon(editor.getIconForTag(t));
    	}else{
    		setIcon(null);
    	}
    	if(selected){
    		setOpaque(true);
    		setForeground(Color.WHITE);
    	}else{
    		setOpaque(false);
    		setForeground(Color.BLACK);
    	}
		setText(value.toString());
    	return this;
	}

}
