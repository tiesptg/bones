package com.palisand.bones.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
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
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.palisand.bones.di.Classes;
import com.palisand.bones.log.Logger;
import com.palisand.bones.tt.Document;
import com.palisand.bones.tt.Link;
import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.ObjectConverter;
import com.palisand.bones.tt.ObjectConverter.Property;
import com.palisand.bones.tt.Repository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class Editor extends JFrame implements TreeSelectionListener {
	private static final long serialVersionUID = 4886239584255305072L;
	private static final Logger LOG = Logger.getLogger(Editor.class);
	private JTree tree = new JTree();
	private JPanel properties = new JPanel();
	private JPanel info = new JPanel();
	private RepositoryModel repositoryModel = new RepositoryModel(new Repository());
	private JPanel buttons = new JPanel();
	private JPanel top = new JPanel();
	private Map<Class<?>,List<Class<?>>> concreteClasses = new HashMap<>();
	private final Repository repository = new Repository();
	private File lastDirectory = new File(".").getAbsoluteFile();
	
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Config {
		private int left;
		private int top;
		private int width;
		private int height;
		private String lastDirectory;
	}
	
	private void saveConfig() {
		Rectangle rect = getBounds();
		Config config = new Config(rect.x,rect.y,rect.width,rect.height
				,lastDirectory.getAbsolutePath());
		try {
			repository.write("config.tt", config);
		} catch (Exception ex) {
			handleException(ex);
		}
	}
	
	private void restoreConfig() {
		try {
			Config config = (Config)repository.read("config.tt");
			setBounds(config.getLeft(),config.getTop(),config.getWidth(),config.getHeight());
			lastDirectory = new File(config.getLastDirectory());
		} catch (Exception ex) {
			if (!(ex instanceof FileNotFoundException)) {
				handleException(ex);
			}
			setBounds(100,100,800,600);
			saveConfig();
		}
	}
	
	private void init(JSplitPane pane) {
		pane.setBorder(BorderFactory.createEmptyBorder());
		pane.setResizeWeight(0.7);
		pane.setOneTouchExpandable(false);
		pane.setContinuousLayout(true);
	}
	
	private void initMenu(JMenuBar menuBar) {
		JMenu file = new JMenu("File");
		menuBar.add(file);
		List<Class<?>> documents = getConcreteAssignableClasses("",Document.class);
		for (Class<?> c: documents) {
			JMenuItem newM = new JMenuItem("New " + c.getSimpleName());
			file.add(newM);
			newM.addActionListener(e -> addRoot((Document)newInstance(c)));
		}
		file.addSeparator();
		JMenuItem openM = new JMenuItem("Open");
		openM.addActionListener(e -> openFile());
		file.add(openM);
		JMenuItem saveM = new JMenuItem("Save");
		saveM.addActionListener(e -> saveToFile());
		file.add(saveM);
		file.addSeparator();
		JMenuItem quit = new JMenuItem("Quit");
		quit.addActionListener(e -> dispose());
		file.add(quit);
	}
	
	private void openFile() {
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(lastDirectory);
		fc.addChoosableFileFilter(new FileNameExtensionFilter("TypedText Files", ".tt"));
		if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(this)) {
			try {
				Document root = (Document)repository.read(fc.getSelectedFile().getAbsolutePath());
				addRoot(root);
				lastDirectory = fc.getSelectedFile().getParentFile();
				saveConfig();
			} catch (Exception ex) {
				handleException(ex);
			}
		}
	}
	
	private void saveToFile() {
		repositoryModel.getRoots().forEach(root -> {
			try {
				String file = root.getFilename();
				if (root.getFilename() == null) {
					JFileChooser fc = new JFileChooser();
					fc.setCurrentDirectory(lastDirectory);
					fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(this)) {
						file = fc.getSelectedFile().getAbsolutePath();
						lastDirectory = fc.getSelectedFile();
						saveConfig();
					}
				}
				repository.write(file, root);
			} catch (Exception ex) {
				handleException(ex);
			}
		});
	}
	
	private void addRoot(Document root) {
		TreePath path = repositoryModel.addRoot(root);
		tree.setSelectionPath(path);
	}
	
	private Node<?> newInstance(Class<?> cls) {
		try {
			return (Node<?>)cls.getConstructor().newInstance();
		} catch (Exception ex) {
			handleException(ex);
		}
		return null;
	}
	
	private void handleException(Exception ex) {
		LOG.log("Unexpected Exception").with(ex).error();
	}
	
	private List<Class<?>> getConcreteAssignableClasses(Class<?> cls) {
		return getConcreteAssignableClasses(cls.getPackageName(), cls);
	}
	
	private List<Class<?>> getConcreteAssignableClasses(String path, Class<?> cls) {
		List<Class<?>> result = concreteClasses.get(cls);
		if (result == null) {
			try {
				result = Classes.findClasses(path, 
						c -> !Modifier.isAbstract(c.getModifiers()) && cls.isAssignableFrom(c));
				concreteClasses.put(cls, result);
			} catch (Exception ex) {
				handleException(ex);
			}
		}
		return result;
	}
	
	private Editor() {
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				saveConfig();
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				saveConfig();
			}

		});
		JMenuBar menuBar = new JMenuBar();
		initMenu(menuBar);
		setJMenuBar(menuBar);
		setLayout(new GridLayout(1,1));
		JScrollPane sp = new JScrollPane(tree);
		JPanel below = new JPanel();
		below.setLayout(new BorderLayout());
		below.add(properties,BorderLayout.NORTH);
		JScrollPane sp2 = new JScrollPane(below);
		sp.setBorder(BorderFactory.createEmptyBorder());
		sp2.setBorder(BorderFactory.createEmptyBorder());
		JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT
				,sp, sp2);
		pane.setBorder(BorderFactory.createEmptyBorder());
		init(pane);
		top.setLayout(new BorderLayout());
		top.add(pane,BorderLayout.CENTER);
		top.add(buttons,BorderLayout.SOUTH);
		JSplitPane all = new JSplitPane(JSplitPane.VERTICAL_SPLIT
				,top,info);
		init(all);
		add(all);
		
		tree.setRootVisible(false);
		tree.setExpandsSelectedPaths(true);
		tree.setModel(repositoryModel);
		tree.setCellRenderer(repositoryModel);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
		restoreConfig();
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
			if (value instanceof Node<?> child) {
				repositoryModel.fireChildChanged(node,child);
			} else {
				repositoryModel.fireNodeChanged(tree.getSelectionPath());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void newChildToList(Property property,Node<?> node, Class<?> type) {
		try {
			Node<?> child = (Node<?>)type.getConstructor().newInstance();
			List<Object> list = (List<Object>)property.getGetter().invoke(node);
			list.add(child);
			child.setContainer(node,property.getName());
			TreePath path = repositoryModel.fireChildAdded(child);
			tree.setSelectionPath(path);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void makeStringComponent(JPanel panel,Node<?> node, String value, Property property) {
		JTextField field = new JTextField(value);
		field.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				field.selectAll();
			}
			
		});
		panel.add(field);
		field.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				setValue(node,property.getSetter(),field.getText());
			}
			
		});
	}
	
	private void makeNodeComponent(JPanel panel,Node<?> node, Node<?> selected, Property property) {
		List<Node<?>> nodes = new ArrayList<>();
		nodes.add(null);
		try {
			List<Class<?>> classes = Classes.findClasses(node.getClass().getPackageName(), 
					cls -> !Modifier.isAbstract(cls.getModifiers()) && property.getType().isAssignableFrom(cls));
			classes.forEach(cls -> nodes.add(newInstance(cls)));
		} catch (Exception ex) {
			handleException(ex);
		}
		int selectedItem = 0;
		if (selected != null) {
			for (int i = 0; i < nodes.size(); ++i) {
				if (nodes.get(i) != null && nodes.get(i).getClass() == selected.getClass()) {
					nodes.set(i, selected);
					selectedItem = i;
					break;
				}
			}
		}
		JComboBox<Node<?>> box = new JComboBox<>(nodes.toArray(len -> new Node[len]));
		panel.add(box);
		box.setSelectedIndex(selectedItem);
		box.addActionListener(e -> setValue(node,property.getSetter(),box.getSelectedItem()));

	}
	
	private void makeNumberComponent(JPanel panel,Node<?> node, Object value, Property property) {
		JSpinner spinner = new JSpinner();
		spinner.setModel(new SpinnerNumberModel((Number)value, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
		JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)spinner.getEditor();
		spinner.addFocusListener(new FocusListener() {
			@Override public void focusGained(FocusEvent e) {
				SwingUtilities.invokeLater(() -> editor.getTextField().selectAll());
			}
			@Override public void focusLost(FocusEvent e) {
				setValue(node,property.getSetter(),spinner.getValue());
			}
		});
		panel.add(spinner);
		spinner.addChangeListener(e -> setValue(node,property.getSetter(),spinner.getValue()));
	}
	
	@SuppressWarnings("unchecked")
	private void makeLinkComponent(JPanel panel,Node<?> node, Link<?,?> link, Property property) {
		Node<?> container = link.getContainer();
		try {
 			List<Node<?>> candidates = repository.find(container, link.getPathPattern());
			candidates.add(0,null);
			JComboBox<Node<?>> box = new JComboBox<>(candidates.toArray(len -> new Node[len]));
			final Link<Node<?>,Node<?>> value = (Link<Node<?>,Node<?>>)getValue(node,property.getGetter());
			box.setSelectedItem(value.get());
			panel.add(box);
			box.addActionListener(e -> {
				try {
					value.set((Node<?>)box.getSelectedItem());
				} catch (Exception e1) {
					handleException(e1);
				}
			});
		} catch (Exception ex) {
			handleException(ex);
		}
	}
	
	private <D> void makeListComponent(JPanel panel,Node<?> node, List<D> value, Property property) {
		JLabel label = new JLabel("<html>"+ value.stream().map(obj -> obj.toString()).collect(Collectors.joining("<br>")));
		label.setOpaque(true);
		UIDefaults defaults = UIManager.getDefaults();
		label.setBackground(defaults.getColor("List.background"));
		label.setForeground(defaults.getColor("List.foreground"));
		label.setFocusable(true);
		label.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
				label.setBackground(defaults.getColor("List.selectionBackground"));
				label.setForeground(defaults.getColor("List.selectionForeground"));
			}

			@Override
			public void focusLost(FocusEvent e) {
				label.setBackground(defaults.getColor("List.background"));
				label.setForeground(defaults.getColor("List.foreground"));
			}
			
		});
		label.addMouseListener(new MouseAdapter() {

			@SuppressWarnings("unchecked")
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
					ListEditor<D> editor = (ListEditor<D>)ListEditor.dialogFor(Editor.this, "Edit " + property.getLabel(), property.getComponentType());
					if (editor != null && editor.editData(value)) {
						setValue(node, property.getSetter(), editor.getData());
					}
				}
			}
		});
		JScrollPane pane = new JScrollPane(label);
		pane.setPreferredSize(new Dimension(100,85));
		panel.add(pane);
	}
	
	private void select(Node<?> node) {
		properties.removeAll();
		buttons.removeAll();
		ObjectConverter converter = (ObjectConverter)repositoryModel.getRepository().getConverter(node.getClass());
		buttons.setLayout(new GridLayout(1,1));
		BoxLayout box = new BoxLayout(properties,BoxLayout.PAGE_AXIS);
		properties.setLayout(box);
		for (Property property: converter.getProperties()) {
			Object value = getValue(node,property.getGetter());
			if (!property.isList()) {
				JPanel row = new JPanel();
				row.setLayout(new GridLayout(1,2));
				properties.add(row);
				row.add(new JLabel(property.getLabel()));
				if (property.getType() == String.class) {
					makeStringComponent(row,node,(String)value,property);
				} else if (Link.class.isAssignableFrom(property.getType())) {
					makeLinkComponent(row,node,(Link<?,?>)value,property);
				} else if (property.getType() == boolean.class || property.getType() == Boolean.class) {
					row.add(new JCheckBox("",(Boolean)getValue(node,property.getGetter())));
				} else if (property.getType() == int.class || property.getType() == Integer.class
						|| property.getType() == long.class || property.getType() == Long.class
						|| property.getType() == BigInteger.class) {
					makeNumberComponent(row,node, value, property);
				} else if (Node.class.isAssignableFrom(property.getType())) {
					makeNodeComponent(row,node,(Node<?>)value,property);
				}
			} else if (Node.class.isAssignableFrom(property.getComponentType())) {
				List<Class<?>> classes = getConcreteAssignableClasses(property.getComponentType());
				for (Class<?> c: classes) {
					JButton button = new JButton("New " + c.getSimpleName());
					button.addActionListener(e -> newChildToList(property,node,c));
					buttons.add(button);
				}
			} else {
				JPanel row = new JPanel();
				row.setLayout(new GridLayout(1,2));
				properties.add(row);
				row.add(new JLabel(property.getLabel()));
				makeListComponent(row,node,(List<?>)value,property);
			}
		}
		JButton remove = new JButton("Delete");
		buttons.add(remove);
		properties.validate();
		top.validate();
	}


	
	public static void main(String...args) {
		new Editor().setVisible(true);
	}

}
