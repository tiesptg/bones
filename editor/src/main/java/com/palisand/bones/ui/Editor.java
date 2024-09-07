package com.palisand.bones.ui;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;

import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.ObjectConverter;
import com.palisand.bones.tt.ObjectConverter.Property;
import com.palisand.bones.tt.Repository;

public class Editor extends JFrame implements TreeSelectionListener {
	private static final long serialVersionUID = 4886239584255305072L;
	private JTree tree = new JTree();
	private JPanel properties = new JPanel();
	private JPanel info = new JPanel();
	private RepositoryModel repositoryModel = new RepositoryModel(new Repository());
	
	private void init(JSplitPane pane) {
		pane.setBorder(BorderFactory.createEmptyBorder());
		pane.setResizeWeight(0.7);
		pane.setOneTouchExpandable(false);
		pane.setContinuousLayout(true);
	}
	
	private void initMenu(JMenuBar menuBar) {
		JMenu file = new JMenu("File");
		menuBar.add(file);
		JMenuItem newM = new JMenuItem("New");
		file.add(newM);
		newM.addActionListener(e -> repositoryModel.addRoot(newInstance()));
	}
	
	private Node<?> newInstance() {
		try {
			return (Node<?>) Class.forName("com.palisand.bones.meta.Model").getConstructor().newInstance();
		} catch (Exception ex) {
			handleException(ex);
		}
		return null;
	}
	
	private void handleException(Exception ex) {
		ex.printStackTrace();
	}
	
	private Editor() {
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		JMenuBar menuBar = new JMenuBar();
		initMenu(menuBar);
		setJMenuBar(menuBar);
		setLayout(new GridLayout(1,1));
		JScrollPane sp = new JScrollPane(tree);
		JScrollPane sp2 = new JScrollPane(properties);
		properties.setOpaque(true);
		properties.setBackground(Color.white);
		properties.setLayout(new TableLayout(2));
		sp.setBorder(BorderFactory.createEmptyBorder());
		sp2.setBorder(BorderFactory.createEmptyBorder());
		JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT
				,sp, sp2);
		pane.setBorder(BorderFactory.createEmptyBorder());
		init(pane);
		JSplitPane all = new JSplitPane(JSplitPane.VERTICAL_SPLIT
				,pane,info);
		init(all);
		add(all);
		
		tree.setRootVisible(false);
		tree.setModel(repositoryModel);
		tree.setCellRenderer(repositoryModel);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
		setLocation(100,100);
		setSize(600,400);
	}
	
	@Override
	public void valueChanged(TreeSelectionEvent e) {
		select((Node<?>)e.getPath().getLastPathComponent());
	}
	
	private Object getValue(Node<?> node, Method getter) {
		try {
			return getter.invoke(node);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	private void setValue(Node<?> node, Method setter, Object value) {
		try {
			setter.invoke(node,value);
			repositoryModel.fireNodeChanged(tree.getSelectionPath());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void newChildToList(Property property, Node<?> node) {
		try {
			Node<?> child = (Node<?>)property.getComponentType().getConstructor().newInstance();
			List<Object> list = (List<Object>)property.getGetter().invoke(node);
			list.add(child);
			child.setContainer(node,property.getName());
			repositoryModel.fireChildAdded(child);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void select(Node<?> node) {
		properties.removeAll();
		ObjectConverter converter = (ObjectConverter)repositoryModel.getRepository().getConverter(node.getClass());
		for (Property property: converter.getProperties().values()) {
			properties.add(new JLabel(property.getLabel()));
			if (property.getType() == String.class) {
				JTextField field = new JTextField((String)getValue(node,property.getGetter()));
				properties.add(field);
				field.addKeyListener(new KeyAdapter() {

					@Override
					public void keyReleased(KeyEvent e) {
						setValue(node,property.getSetter(),field.getText());
					}
					
				});
			} else if (property.getType() == boolean.class || property.getType() == Boolean.class) {
				properties.add(new JCheckBox("",(Boolean)getValue(node,property.getGetter())));
			} else if (Number.class.isAssignableFrom(property.getType())) {
				properties.add(new JSpinner());
			} else if (property.isList()) {
				JButton button = new JButton("New " + property.getComponentType().getSimpleName());
				button.addActionListener(e -> newChildToList(property,node));
				properties.add(button);
			}
		}
		properties.validate();
	}


	
	public static void main(String...args) {
		new Editor().setVisible(true);
	}

}
