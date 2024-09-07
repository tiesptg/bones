package com.palisand.bones.ui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.ObjectConverter;
import com.palisand.bones.tt.ObjectConverter.Property;
import com.palisand.bones.tt.Repository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public class RepositoryModel implements TreeModel, TreeCellRenderer {
	private final Repository repository;
	private final List<Node<?>> roots = new ArrayList<>();
	private final DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
	private final List<TreeModelListener> listeners = new ArrayList<>();
	
	@Override
	public Object getRoot() {
		return this;
	}
	
	public void addRoot(Node<?> root) {
		roots.add(root);
		fireChildAdded(root);
	}
	
	@SuppressWarnings("unchecked")
	private List<Node<?>> getChildren(Object parent) {
		if (parent == this) {
			return roots;
		}
		List<Node<?>> result = new ArrayList<>();
		ObjectConverter converter = (ObjectConverter)repository.getConverter(parent.getClass());
		converter.getProperties().values().forEach(property -> {
			if (Node.class.isAssignableFrom(property.getType()) ||
					(property.isList() && Node.class.isAssignableFrom(property.getComponentType()))) {
				try {
					if (property.isList()) {
						List<Node<?>> list = (List<Node<?>>)property.getGetter().invoke(parent);
						result.addAll(list);
					} else {
						Node<?> node = (Node<?>)property.getGetter().invoke(parent);
						if (node != null) {
							result.add(node);
						}
					}
				} catch (Exception ex) {
					handleException(ex);
				}
			}
		});
		return result;
	}
	
	private void handleException(Exception ex) {
		ex.printStackTrace();
	}

	@Override
	public Object getChild(Object parent, int index) {
		return getChildren(parent).get(index);
	}

	@Override
	public int getChildCount(Object parent) {
		return getChildren(parent).size();
	}

	@Override
	public boolean isLeaf(Object node) {
		return getChildCount(node) == 0;
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		return getChildren(parent).indexOf(child);
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		listeners.add(l);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		listeners.remove(l);
	}
	
	private void getPathFor(List<Node<?>> list, Node<?> n) {
		if (n.getContainer() != null) {
			getPathFor(list,n.getContainer());
		}
		list.add(n);
	}
	
	public void fireChildAdded(Node<?> n) {
		if (n.getContainer() == null) {
			TreeModelEvent event = new TreeModelEvent(this,new TreePath(this),new int[]{getIndexOfChild(this, n)},new Object[]{n});
			listeners.forEach(l -> l.treeStructureChanged(event));
		} else {
			List<Node<?>> list = new ArrayList<>();
			getPathFor(list,n.getContainer());
			TreeModelEvent event = new TreeModelEvent(this,new TreePath(list),new int[]{getIndexOfChild(n.getContainer(), n)},new Object[]{n});
			listeners.forEach(l -> l.treeStructureChanged(event));
		}
	}
	
	public void fireNodeChanged(TreePath path) {
		TreeModelEvent event = new TreeModelEvent(this,path);
		listeners.forEach(l -> l.treeNodesChanged(event));
	}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {
		Component c = cellRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		if (c instanceof JLabel label && value instanceof Node node) {
			if (node.getRootContainer() != node) {
				label.setText(node.getContainingAttribute() + ": " + value.toString() );
			}
		}
		return c;
	}

}
