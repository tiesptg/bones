package com.palisand.bones.ui;

import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;

import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.Repository;

public class Editor extends JFrame {
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
		setLocation(100,100);
		setSize(600,400);
	}
	
	public static void main(String...args) {
		new Editor().setVisible(true);
	}

}
