package com.palisand.bones.ui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.palisand.bones.tt.Document;
import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.ObjectConverter;
import com.palisand.bones.tt.Repository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
public class RepositoryModel implements TreeModel, TreeCellRenderer {
	private final Repository repository;
	private final List<Document> roots = new ArrayList<>();
	private final DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
	private final List<TreeModelListener> listeners = new ArrayList<>();
	
	@Override
	public Object getRoot() {
		return this;
	}
	
	public TreePath addRoot(Document root) {
		roots.add(root);
		return fireChildAdded(root);
	}
	
	public void clear() {
		repository.clear();
		roots.forEach(document -> fireChildRemoved(getRoot(),document));
		roots.clear();
	}
	
	@SuppressWarnings("unchecked")
	private List<Node<?>> getChildren(Object parent) {
		if (parent == this) {
			return (List<Node<?>>)(Object)roots;
		}
		List<Node<?>> result = new ArrayList<>();
		ObjectConverter converter = (ObjectConverter)repository.getConverter(parent.getClass());
		converter.getProperties().forEach(property -> {
			if (Node.class.isAssignableFrom(property.getType()) ||
					(property.isList() && Node.class.isAssignableFrom(property.getComponentType()))) {
				try {
					if (property.isList()) {
						if (!property.isLink()) {
							List<Node<?>> list = (List<Node<?>>)property.getGetter().invoke(parent);
							result.addAll(list);
						}
					} else if (!property.isLink()) {
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
		return getChildren(node).isEmpty();
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
	
	private void getPathFor(List<Object> list, Node<?> n) {
		if (n.getContainer() != null) {
			getPathFor(list,n.getContainer());
		}
		list.add(n);
	}
	
	public TreePath fireChildAdded(Node<?> n) {
		if (n.getContainer() == null) {
			TreeModelEvent event = new TreeModelEvent(this,new TreePath(this),new int[]{getIndexOfChild(this, n)},new Object[]{n});
			listeners.forEach(l -> l.treeStructureChanged(event));
			return event.getTreePath().pathByAddingChild(n);
		}
		List<Object> list = new ArrayList<>();
		list.add(this);
		getPathFor(list,n.getContainer());
		TreeModelEvent event = new TreeModelEvent(this,new TreePath(list.toArray()),new int[]{getIndexOfChild(n.getContainer(), n)},new Object[]{n});
		listeners.forEach(l -> l.treeStructureChanged(event));
		return event.getTreePath().pathByAddingChild(n);
	}
	
	public TreePath fireChildChanged(Node<?> n,Node<?> child) {
		List<Object> list = new ArrayList<>();
		list.add(this);
		getPathFor(list,n);
		TreeModelEvent event = new TreeModelEvent(this,new TreePath(list.toArray()),new int[]{getIndexOfChild(n, child)},new Object[]{child});
		listeners.forEach(l -> l.treeStructureChanged(event));
		return event.getTreePath().pathByAddingChild(n);
	}
	
	public TreePath fireChildRemoved(Object n,Node<?> child) {
		List<Object> list = new ArrayList<>();
		list.add(this);
		if (n != this) {
			getPathFor(list,(Node<?>)n);
		}
		TreeModelEvent event = new TreeModelEvent(this,new TreePath(list.toArray()),new int[]{getIndexOfChild(n, child)},new Object[]{child});
		listeners.forEach(l -> l.treeNodesRemoved(event));
		return event.getTreePath();
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
	
	public TreePath getTreePath(Node<?> node) {
		List<Object> list = new ArrayList<>();
		list.add(this);
		while (node != null) {
			list.add(1,node);
			node = node.getContainer();
		}
		return new TreePath(list.toArray());
	}
	


}
