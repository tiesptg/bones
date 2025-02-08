package com.palisand.bones.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.ContainerOrderFocusTraversalPolicy;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.palisand.bones.di.Classes;
import com.palisand.bones.log.Logger;
import com.palisand.bones.tt.CustomEditor;
import com.palisand.bones.tt.Document;
import com.palisand.bones.tt.Link;
import com.palisand.bones.tt.LinkList;
import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.ObjectConverter;
import com.palisand.bones.tt.ObjectConverter.Property;
import com.palisand.bones.tt.Repository;
import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.ConstraintViolation;
import com.palisand.bones.tt.Validator;
import com.palisand.bones.ui.ListEditor.LinkListEditor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class Editor extends JFrame implements TreeSelectionListener {
	private static final long serialVersionUID = 4886239584255305072L;
	private static final String RULE = "Rule";
	private static final Logger LOG = Logger.getLogger(Editor.class);
	private JTree tree = new JTree();
	private JPanel properties = new JPanel();
	private JTable violations = new JTable() {
		
		@Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
        int condition, boolean pressed) {
			if (e.getKeyCode() == KeyEvent.VK_TAB) {
				Editor.this.dispatchEvent(e);
				return true;
			}
			return super.processKeyBinding(ks, e, condition, pressed);
		}

	};
	private JPanel buttons = new JPanel();
	private JPanel top = new JPanel();
	private Map<Class<?>,List<Class<?>>> concreteClasses = new HashMap<>();
	private final Repository repository = new Repository();
  private RepositoryModel repositoryModel = new RepositoryModel(repository);
	private File lastDirectory = new File(".").getAbsoluteFile();
	private Node<?> selectedNode = null;
	private List<JComponent> propertyEditors = new ArrayList<>();
	private ProblemsModel problemsModel = new ProblemsModel();
	private final Map<Character,ActionListener> keyListeners = new TreeMap<>();
	private KeyAdapter escListener = new KeyAdapter() {

		@Override
		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				tree.requestFocus();
			}
		}
		
	};
	private ComponentAdapter componentAdapter = new ComponentAdapter() {

		@Override
		public void componentResized(ComponentEvent e) {
			saveConfig();
		}

		@Override
		public void componentMoved(ComponentEvent e) {
			saveConfig();
		}

	};
	private ContainerOrderFocusTraversalPolicy editorPolicy = new ContainerOrderFocusTraversalPolicy() {
		
		private static final long serialVersionUID = -2780244947286348700L;
		
		@Override
		public boolean accept(Component component) {
			if (super.accept(component)) {
				return component instanceof JTree || component instanceof JTable || component instanceof JCheckBox
					|| component instanceof JTextComponent || component instanceof JComboBox || component instanceof JButton;
			}
			return false;
		}

	};
	
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
		JMenuItem closeM = new JMenuItem("Close");
		closeM.addActionListener(e -> closeFiles());
		file.add(closeM);
		file.addSeparator();
		JMenuItem quit = new JMenuItem("Quit");
		quit.addActionListener(e -> dispose());
		file.add(quit);
	}
	
	private void closeFiles() {
		repositoryModel.clear();
	}
	
	private void openFile() {
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(lastDirectory);
		fc.addChoosableFileFilter(new FileNameExtensionFilter("TypedText Files", ".tt"));
		if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(this)) {
			try {
				Document doc = (Document)repository.read(fc.getSelectedFile().getAbsolutePath());
				addRoot(doc);
				lastDirectory = fc.getSelectedFile().getParentFile();
				saveConfig();
				validateDocuments();
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
	
	private void addRoot(Document document) {
	  try {
  		TreePath path = repositoryModel.addRoot(document);
  		tree.setSelectionPath(path);
	  } catch (Exception ex) {
	    handleException(ex);
	  }
	}
	
	private <A> A newInstance(Class<A> cls) {
		try {
			return (A)cls.getConstructor().newInstance();
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
	
	private JTree initTree() {
		tree.setRootVisible(false);
		tree.setExpandsSelectedPaths(true);
		tree.setModel(repositoryModel);
		tree.setCellRenderer(repositoryModel);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(this);
		tree.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		tree.setInputMap(JComponent.WHEN_FOCUSED, new InputMap());
		
		tree.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent arg0) {
				UIDefaults defaults = UIManager.getDefaults();
				tree.setBorder(BorderFactory.createLineBorder(defaults.getColor("textHighlight"),2));
			}

			@Override
			public void focusLost(FocusEvent arg0) {
				tree.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			}
			
		});
		tree.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
					TreePath selection = tree.getSelectionPath();
					if (selection != null) {
						if (tree.isExpanded(tree.getSelectionRows()[0])) {
							tree.collapsePath(selection);
						} else {
							tree.expandPath(selection);
						}
					}
				} else if (e.getKeyCode() == KeyEvent.VK_UP) {
					int newRow = tree.getLeadSelectionRow();
					if (newRow > 0) {
						tree.setSelectionRow(newRow-1);
					}
				} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
					int newRow = tree.getLeadSelectionRow();
					if (newRow < tree.getRowCount()-1) {
						tree.setSelectionRow(newRow+1);
					}

				} else if (!e.isAltDown() && !e.isControlDown() && !e.isShiftDown()) {
					ActionListener listener = keyListeners.get(e.getKeyChar());
					if (listener != null) {
						listener.actionPerformed(new ActionEvent(e.getSource(),ActionEvent.ACTION_PERFORMED,"key"));
					}
				}

			}
			
		});
		return tree;
	}
	
	private JTable initViolations() {
		violations.setModel(problemsModel);
		violations.addKeyListener(escListener);
		violations.setRowSelectionAllowed(true);
		violations.setColumnSelectionAllowed(false);
		violations.setFocusable(true);
		violations.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					selectViolation(violations.getSelectedRow());
					e.consume();
				}
			}
		});
		violations.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getClickCount() >= 2) {
					selectViolation(violations.getSelectedRow());
				}
			}
		});
		return violations;

	}
	
	private void selectViolation(int row) {
		ConstraintViolation violation = problemsModel.getProblems().get(row);
		TreePath path = repositoryModel.getTreePath(violation.node());
		tree.setSelectionPath(path);
		for (JComponent comp: propertyEditors) {
			if (comp.getName().equals(violation.field())) {
				if (comp instanceof JSpinner spinner) {
					JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)spinner.getEditor();
					comp = editor.getTextField();
				}
					
				comp.requestFocus();
				return;
			}
		}
	}
	
	private Editor() throws Exception {
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setFocusCycleRoot(true);
		setFocusTraversalPolicy(editorPolicy);
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		getRootPane().setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		addComponentListener(componentAdapter);
		JMenuBar menuBar = new JMenuBar();
		initMenu(menuBar);
		setJMenuBar(menuBar);
		setLayout(new GridLayout(1,1));
		JScrollPane sp = new JScrollPane(initTree());
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

		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Problems", new JScrollPane(initViolations()));
		
		JSplitPane all = new JSplitPane(JSplitPane.VERTICAL_SPLIT
				,top,tabs);
		init(all);
		pane.setResizeWeight(0.5);
		add(all);
		tree.addKeyListener(escListener);
		
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
	
	private <C extends Node<?>,X extends Node<?>> void setLinkListValue(C node, LinkList<C,X> linkList, List<X> nodes) throws IOException {
		linkList.clear();
		for (X child: nodes) {
			linkList.add(child);
		}
		validateProperties();
		validateDocuments();
	}
	
	private void setValue(Node<?> node, Method setter, Object value) {
		try {
			setter.invoke(node,value);
			if (value instanceof Node<?> child) {
				repositoryModel.fireChildChanged(node,child);
			} else {
				repositoryModel.fireNodeChanged(tree.getSelectionPath());
			}
			validateProperties();
			validateDocuments();
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
			SwingUtilities.invokeLater(() -> tree.setSelectionPath(path));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private <N extends Node<?>> void validateProperties() {
		for (JComponent component: propertyEditors) {
			Rules<N> rules = (Rules<N>)component.getClientProperty(RULE);
			if (rules != null) {
				component.setEnabled(rules.isEnabled((N)selectedNode));
			}
		}
	}
	
	private <N extends Node<?>> void makeStringComponent(JPanel panel,N node, String value, Property property) {
		JTextField field = new JTextField(value);
		field.setName(property.getName());
		field.putClientProperty(RULE, property.getRules());
		propertyEditors.add(field);
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
		field.addKeyListener(escListener);
	}

	@SuppressWarnings("unchecked")
	private <E extends Enum<E>> void makeEnumComponent(JPanel row, Node<?> node, Object value, Property property) {
		Rules<?> rules = property.getRules();
		E[] values = ((Class<E>)property.getType()).getEnumConstants();
		if (rules != null && !rules.isNotNull()) {
			E[] ne = Arrays.copyOf(values,1);
			ne[0] = null;
			values = Stream.concat(Arrays.asList(ne).stream(),Arrays.stream(values)).toArray(len -> (E[])Array.newInstance(property.getType(),len));
		}
		JComboBox<Enum<E>> box = new JComboBox<>(values);
		if (value != null) {
			box.setSelectedItem(value);
		}
		box.setName(property.getName());
		box.putClientProperty(RULE, rules);
		propertyEditors.add(box);
		box.addActionListener(e -> setValue(node,property.getSetter(),box.getSelectedItem()));
		box.addKeyListener(escListener);
		row.add(box);
	}
	
	private void makeBooleanComponent(JPanel row,Node<?> node, Boolean selected, Property property) {
		JTextField label = new JTextField(selected.toString());
		label.setName(property.getName());
		label.putClientProperty(RULE, property.getRules());
		label.setEditable(false);
		propertyEditors.add(label);

		label.setOpaque(true);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		if (!property.isReadonly()) {
			label.setFocusable(true);
			label.addMouseListener(new MouseAdapter() {
	
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getButton() == MouseEvent.BUTTON1) {
						Boolean value = !Boolean.valueOf(label.getText());
						label.setText(value.toString());
						setValue(node,property.getSetter(),value);
						label.selectAll();
					}
				}
			});
			label.addKeyListener(new KeyAdapter() {
	
				@Override
				public void keyReleased(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
						Boolean value = !Boolean.valueOf(label.getText());
						label.setText(value.toString());
						setValue(node,property.getSetter(),value);
						label.selectAll();
					}
				}
				
			});
			label.addFocusListener(new FocusAdapter() {
	
				@Override
				public void focusGained(FocusEvent e) {
					label.selectAll();
				}
			});
		} else {
			label.setEnabled(false);
			label.setFocusable(false);
		}
		label.addKeyListener(escListener);

		row.add(label);
	}

	private void makeNodeComponent(JPanel panel,Node<?> node, Node<?> selected, Property property) {
		List<Node<?>> nodes = new ArrayList<>();
		nodes.add(null);
		try {
			List<Class<?>> classes = Classes.findClasses(node.getClass().getPackageName(), 
					cls -> !Modifier.isAbstract(cls.getModifiers()) && property.getType().isAssignableFrom(cls));
			classes.forEach(cls -> nodes.add((Node<?>)newInstance(cls)));
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
		box.setName(property.getName());
		box.putClientProperty(RULE, property.getRules());
		propertyEditors.add(box);
		panel.add(box);
		box.setSelectedIndex(selectedItem);
		box.addActionListener(e -> setValue(node,property.getSetter(),box.getSelectedItem()));
		box.addKeyListener(escListener);
		box.setEnabled(!property.isReadonly());
	}
	
	private void makeNumberComponent(JPanel panel,Node<?> node, Object value, Property property) {
		final JSpinner spinner = new JSpinner();
		spinner.setName(property.getName());
		spinner.putClientProperty(RULE, property.getRules());
		propertyEditors.add(spinner);

		if (value == null) {
			value = 0;
		}
		if (property.getType() == Long.class || property.getType() == long.class) {
			spinner.setModel(new SpinnerNumberModel(((Number)value).longValue(), null,null, 1));
		} else if (property.getType() == Integer.class || property.getType() == int.class) {
			spinner.setModel(new SpinnerNumberModel(((Number)value).intValue(), null,null, 1));
		}
		JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor)spinner.getEditor();
		editor.getTextField().addFocusListener(new FocusListener() {
			@Override public void focusGained(FocusEvent e) {
				SwingUtilities.invokeLater(() -> editor.getTextField().selectAll());
			}
			@Override public void focusLost(FocusEvent e) {
				setValue(node,property.getSetter(),spinner.getValue());
			}
		});
		panel.add(spinner);
		spinner.addChangeListener(e -> setValue(node,property.getSetter(),spinner.getValue()));
		editor.getTextField().addKeyListener(escListener);
		spinner.setEnabled(!property.isReadonly());
	}
	
	@SuppressWarnings("unchecked")
	private <A extends Node<?>> void makeLinkComponent(JPanel panel,Node<?> node, Link<?,A> link, Property property) {
		try {
      final Link<Node<?>,Node<?>> value = (Link<Node<?>,Node<?>>)getValue(node,property.getGetter());
      Node<?> linked = value.get();
		  
 			List<A> candidates = repository.find((Class<A>)property.getComponentType(),node, link.getPathPattern());
			candidates.add(0,null);
			JComboBox<Node<?>> box = new JComboBox<>(candidates.toArray(len -> new Node[len]));
			box.setName(property.getName());
      box.setSelectedItem(linked);
			box.putClientProperty(RULE, property.getRules());
			propertyEditors.add(box);
			panel.add(box);
			box.addActionListener(e -> {
				try {
					value.set((Node<?>)box.getSelectedItem());
				} catch (Exception e1) {
					handleException(e1);
				}
			});
			box.addKeyListener(escListener);
			box.setEnabled(!property.isReadonly());
		} catch (Exception ex) {
			handleException(ex);
		}
	}
	
	private <C extends Node<?>,X extends Node<?>> void makeLinkListComponent(JPanel panel, C container, LinkList<C,X> value, Property property) {
		JTextArea label = new JTextArea();
		label.setName(property.getName());
		label.putClientProperty(RULE, property.getRules());
		propertyEditors.add(label);
		label.setEditable(false);

		showValue(label,value.getList());
		label.setOpaque(true);
		label.setFocusable(true);
		label.addFocusListener(new FocusAdapter() {

			@Override
			public void focusGained(FocusEvent e) {
				label.selectAll();
			}
		});
		label.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
					showLinkListEditor(label, container, value, property);
				}
			}
		});
		label.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
					showLinkListEditor(label,container,value,property);
				}
			}
			
		});
		label.addKeyListener(escListener);
		JScrollPane pane = new JScrollPane(label);
		pane.setPreferredSize(new Dimension(100,85));
		panel.add(pane);
		label.setEnabled(!property.isReadonly());
		
	}
	
	private <D> void makeListComponent(JPanel panel,Node<?> node, List<D> value, Property property) {
		JTextArea label = new JTextArea();
		label.setName(property.getName());
		label.putClientProperty(RULE, property.getRules());
		propertyEditors.add(label);
		label.setEditable(false);

		showValue(label,value);
		label.setOpaque(true);
		label.setFocusable(true);
		label.addFocusListener(new FocusAdapter() {

			@Override
			public void focusGained(FocusEvent e) {
				label.selectAll();
			}
		});
		label.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
					showListEditor(label, node, value, property);
				}
			}
		});
		label.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
					showListEditor(label,node,value,property);
				}
			}
			
		});
		label.addKeyListener(escListener);
		JScrollPane pane = new JScrollPane(label);
		pane.setPreferredSize(new Dimension(100,85));
		panel.add(pane);
		label.setEnabled(!property.isReadonly());
	}
	
	public void makeCustomComponent(JPanel panel,Node<?> node, Object value, Property property) {
	  CustomEditor editor = newInstance(property.getEditor());
	  JComponent field = editor.getComponent();
    field.setName(property.getName());
    editor.setNode(node);
    editor.setValue(value);
    field.putClientProperty(RULE, property.getRules());
    propertyEditors.add(field);
    panel.add(field);
    editor.onValueChanged(newValue ->  setValue(node,property.getSetter(),newValue));
    field.addKeyListener(escListener);
	}
	
	@SuppressWarnings("unchecked")
	private <D> void showListEditor(JTextArea label, Node<?> node, List<D> value, Property property) {
		ListEditor<D> editor = (ListEditor<D>)ListEditor.dialogFor(Editor.this, "Edit " + property.getLabel(), property.getComponentType());
		if (editor != null && editor.editData(value)) {
			setValue(node, property.getSetter(), editor.getData());
			showValue(label,editor.getData());
		}
	}
	
	@SuppressWarnings("unchecked")
	private <C extends Node<?>, X extends Node<?>>void showLinkListEditor(JTextArea label, C node, LinkList<C,X> value, Property property) {
		LinkListEditor editor = (LinkListEditor)ListEditor.dialogFor(Editor.this, "Edit " + property.getLabel(), property.getComponentType());
		try {
			List<X> list = (List<X>)repository.find((Class<X>)property.getComponentType(),node, value.getPattern());
			List<X> candidates = list;
			// check whether this is an absolute or relative path
			editor.setOptions((List<Node<?>>)candidates);
			List<Node<?>> valueList = new ArrayList<>();
			value.getList().forEach(link -> {
				try {
					valueList.add(link.get());
				} catch (Exception ex) {
					handleException(ex);
				}
			});
			if (editor != null && editor.editData(valueList)) {
				setLinkListValue(node, value, (List<X>)editor.getData());
				showValue(label,editor.getData());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void showValue(JTextArea label, List<?> data) {
		label.setText(data.stream().map(obj -> obj.toString()).collect(Collectors.joining("\n")));
	}
	
	private String registerAction(String label, ActionListener listener) {
		String chars = label.toLowerCase();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < chars.length(); ++i) {
			char c = chars.charAt(i);
			if (!keyListeners.containsKey(c)) {
				sb.append(label.substring(0, i));
				sb.append("<u>");
				sb.append(label.charAt(i));
				sb.append("</u>");
				sb.append(label.substring(i+1));
				keyListeners.put(c, listener);
				return sb.toString();
			}
		}
		return null;
	}
	
	private void deleteNode() {
		
	}
	
	@SuppressWarnings("unchecked")
	private <C extends Node<?>, X extends Node<?>> void select(C node) {
		selectedNode = node;
		keyListeners.clear();
		properties.removeAll();
		propertyEditors.clear();
		buttons.removeAll();
		ActionListener deleteAction = e -> deleteNode();
		String deleteLabel = registerAction("Delete", deleteAction);
		ObjectConverter converter = (ObjectConverter)repositoryModel.getRepository().getConverter(node.getClass());
		buttons.setLayout(new GridLayout(1,1));
		BoxLayout box = new BoxLayout(properties,BoxLayout.PAGE_AXIS);
		properties.setLayout(box);
		for (Property property: converter.getProperties()) {
			Object value = getValue(node,property.getGetter());
			if (!property.hasTextIgnoreAnnotation()) {
				if (!property.isList() || property.isLink()) {
					JPanel row = new JPanel();
					row.setLayout(new GridLayout(1,2));
					properties.add(row);
					row.add(new JLabel(property.getLabel()));
					if (property.getEditor() != null) {
            makeCustomComponent(row,node,value,property);
					} else if (property.getType() == String.class) {
						makeStringComponent(row,node,(String)value,property);
					} else if (Link.class.isAssignableFrom(property.getType())) {
						makeLinkComponent(row,node,(Link<?,?>)value,property);
					} else if (LinkList.class.isAssignableFrom(property.getType())) {
						makeLinkListComponent(row,node,(LinkList<C,X>)value,property);
					} else if (property.getType() == boolean.class || property.getType() == Boolean.class) {
						makeBooleanComponent(row,node,(Boolean)value,property);
					} else if (property.getType() == int.class || property.getType() == Integer.class
							|| property.getType() == long.class || property.getType() == Long.class
							|| property.getType() == BigInteger.class) {
						makeNumberComponent(row,node, value, property);
					} else if (Node.class.isAssignableFrom(property.getType())) {
						makeNodeComponent(row,node,(Node<?>)value,property);
					} else if (property.getType().isEnum()) {
						makeEnumComponent(row,node,value,property);
					}
				} else if (!property.isReadonly() && Node.class.isAssignableFrom(property.getComponentType())) {
					List<Class<?>> classes = getConcreteAssignableClasses(property.getComponentType());
					for (Class<?> c: classes) {
						ActionListener listener = e -> newChildToList(property,node,c);
						String label = registerAction(c.getSimpleName(),listener);
						JButton button = new JButton("<html>New " + label);
						button.addActionListener(listener);
						button.addKeyListener(escListener);
						buttons.add(button);
					}
				} else if (!property.isLink()) {
					JPanel row = new JPanel();
					row.setLayout(new GridLayout(1,2));
					properties.add(row);
					row.add(new JLabel(property.getLabel()));
					makeListComponent(row,node,(List<?>)value,property);
				}
			}
		}
		validateProperties();
		JButton remove = new JButton("<html>" + deleteLabel);
		remove.addKeyListener(escListener);
		buttons.add(remove);
		properties.validate();
		top.validate();
	}
	
	private void validateDocuments() {
		Validator validator = new Validator();
		repositoryModel.getRoots().forEach(doc -> doc.validate(validator));
		problemsModel.setProblems(validator.getViolations());
		problemsModel.fireTableDataChanged();
	}

	public static void main(String...args) throws Exception {
		new Editor().setVisible(true);
	}

}
