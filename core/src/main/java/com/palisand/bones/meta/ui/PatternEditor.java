package com.palisand.bones.meta.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.palisand.bones.meta.Contained;
import com.palisand.bones.meta.Entity;
import com.palisand.bones.meta.Model;
import com.palisand.bones.tt.Document;

import lombok.Getter;
import lombok.Setter;

public class PatternEditor extends JDialog implements TreeSelectionListener {

  private static final long serialVersionUID = -469430723877885985L;
  
  record Branch(Entity entity, Contained contained) {
    
    boolean isUp() throws IOException {
      return contained == null && entity.getEntityContainer().get() != null;
    }
    
    String getPart() {
      try {
        if (isUp()) {
          return "/..";
        } 
        if (contained == null) {
          return "#";
        }
        return '/' + contained.getName() + "/.*";
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      return "<Error while processing>";
    }
    
    @Override
    public String toString() {
      try {
        if (isUp()) {
          return "../ [" + entity.getName() + "]";
        } 
        if (contained == null) {
          return "# [" + entity.getName() + "]";
        }
        return contained.getName() + "/.* [" + entity.getName() + "]";
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      return "<Error while processing>";
    }
  }
  
  @Setter
  @Getter
  class ModelTreeModel implements TreeModel {
    private final List<TreeModelListener> listeners = new ArrayList<>();
    private final DefaultTreeCellRenderer cellRenderer = new DefaultTreeCellRenderer();
    private final Entity context;
    private boolean absolute; 
    
    ModelTreeModel(Entity entity) {
      context = entity;
    }
    
    @Override
    public void addTreeModelListener(TreeModelListener l) {
      listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
      listeners.remove(l);
    }
    
    private List<Branch> getChildren(Object object) {
      List<Branch> result = new ArrayList<>();
      try {
        if (object == this) {
          if (!absolute && context != null && context.getEntityContainer().get() != null 
              && context.getEntityContainer().get().getContainer().getEntityContainer().get() != null) {
            result.add(new Branch(context.getEntityContainer().get().getContainer(),null));
          }
          for (Document document: context.getRepository().getLoadedDocuments()) {
            if (document instanceof Model model) {
              for (Entity entity: model.getEntities()) {
                if (entity.getEntityContainer().get() == null) {
                  result.add(new Branch(entity,null));
                }
              }
            }
          }
        } else if (object instanceof Branch branch) {
          Entity entity = branch.entity();
          if (!absolute && branch.isUp() && entity.getEntityContainer().get() != null
            && entity.getEntityContainer().get().getContainer().getEntityContainer().get() != null) {
            result.add(new Branch(entity.getEntityContainer().get().getContainer(),null));
          }
          for (Contained contained: entity.getContainedEntities()) {
            if (contained.getEntity().get() != null) {
              result.add(new Branch(contained.getEntity().get(),contained));
            }
          }
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      return result;
    }
    
    public void setAbsolute(boolean value) {
      absolute = value;
      TreeModelEvent event = new TreeModelEvent(this,new Object[] {this});
      listeners.forEach(l -> l.treeStructureChanged(event));
    }
    

    @Override
    public Object getChild(Object branch, int i) {
      return getChildren(branch).get(i);
    }

    @Override
    public int getChildCount(Object branch) {
      return getChildren(branch).size();
    }

    @Override
    public int getIndexOfChild(Object branch, Object child) {
      return getChildren(branch).indexOf(child);
    }

    @Override
    public Object getRoot() {
      return this;
    }

    @Override
    public boolean isLeaf(Object object) {
      return getChildren(object).isEmpty();
    }

    @Override
    public void valueForPathChanged(TreePath arg0, Object arg1) {
      throw new UnsupportedOperationException();
    }

    public TreePath getPath(String pattern) {
      return null;
    }
    
  }
  
  private JTree tree = new JTree();
  private JTextField field = new JTextField();
  private JTextField relative = null;
  private JTextField external = null;
  private boolean accepted = false;
  private ModelTreeModel treeModel = null;
  
  public PatternEditor(JFrame frame, Entity context,String pattern) {
    super(frame,"Choose Link Pattern",true);
    setLayout(new BorderLayout());
    treeModel = new ModelTreeModel(context);
    getRootPane().setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    
    JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(2,2));
    panel.add(new JLabel("Relative Path"));
    relative = makeBooleanField(false, value -> setRelative(value));
    panel.add(relative);
    panel.add(new JLabel("External File Allowed"));
    external = makeBooleanField(false,value -> setExternal(value));
    panel.add(external);
    add(panel, BorderLayout.NORTH);
    
    JPanel middle = new JPanel();
    middle.setLayout(new BorderLayout());
    middle.add(initTree(),BorderLayout.CENTER);
    middle.add(field,BorderLayout.SOUTH);
    add(middle,BorderLayout.CENTER);
    
    JPanel buttons = new JPanel();
    buttons.setLayout(new GridLayout(1,2));
    add(buttons,BorderLayout.SOUTH);
    
    JButton cancel = new JButton("Cancel");
    buttons.add(cancel);
    cancel.addActionListener(e -> setVisible(false));
    JButton ok = new JButton("OK");
    buttons.add(ok);
    ok.addActionListener(e -> {
      accepted = true;
      setVisible(false);
    });
    setPattern(pattern);
    
    setSize(400,300);
    Rectangle rect = getParent().getBounds();
    setLocation((rect.width - getWidth())/2+rect.x,(rect.height-getHeight())/2+rect.y);
  }
  
  private void setRelative(boolean value) {
    treeModel.setAbsolute(!value);
    if (value) {
      external.setText("false");
    }
    external.setEnabled(!value);
    treeModel.setAbsolute(!value);
  }
  
  private void setExternal(boolean value) {
    resetPattern();
  }
  
  private JTree initTree() {
    tree.setRootVisible(false);
    tree.setExpandsSelectedPaths(true);
    tree.setFocusable(true);
    tree.setModel(treeModel);
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

         }

      }
      
    });
    return tree;
  }
  

  
  private JTextField makeBooleanField(boolean value, Consumer<Boolean> action) {
    JTextField label = new JTextField("true");
    label.setEditable(false);

    label.setOpaque(true);
    label.setHorizontalAlignment(SwingConstants.CENTER);
    label.setFocusable(true);
    label.addMouseListener(new MouseAdapter() {

      @Override
      public void mouseClicked(MouseEvent e) {
        if (label.isEnabled() && e.getButton() == MouseEvent.BUTTON1) {
          Boolean value = !Boolean.valueOf(label.getText());
          label.setText(value.toString());
          action.accept(label.getText().equals("true"));
          label.selectAll();
        }
      }
    });
    label.addKeyListener(new KeyAdapter() {

      @Override
      public void keyReleased(KeyEvent e) {
        if (label.isEnabled() && e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
          Boolean value = !Boolean.valueOf(label.getText());
          label.setText(value.toString());
          action.accept(label.getText().equals("true"));
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
    return label;
  }
  
  private void setPattern(String pattern) {
    if (pattern == null) {
      pattern = "";
    }
    relative.setText(!pattern.contains("#") ? "true" : "false");
    external.setText(pattern.indexOf('#') > 0 ? "true" : "false");
    external.setEnabled(relative.getText().equals("false"));
    field.setText(pattern);
    TreePath path = treeModel.getPath(pattern);
    tree.setSelectionPath(path);
  }
  
  public String getPattern() {
    return field.getText();
  }
  
  public static String editPattern(JFrame frame, Entity context, String pattern) {
    PatternEditor dialog = new PatternEditor(frame,context,pattern);
    dialog.setVisible(true);
    if (!dialog.accepted) {
      return pattern;
    }
    return dialog.getPattern();
  }

  @Override
  public void valueChanged(TreeSelectionEvent event) {
    resetPattern();
  }
  
  private void resetPattern() {
    StringBuilder sb = new StringBuilder();
    if (external.getText().equals("true")) {
      sb.append(".*");
    }
    TreePath path = tree.getSelectionPath();
    if (path != null) {
      for (int i = 1; i < path.getPathCount(); ++i) {
        String part = ((Branch)path.getPath()[i]).getPart();
        if (i == 1 && part.startsWith("/..")) {
          part = "..";
        }
        sb.append(part);
      }
    }
    field.setText(sb.toString());
  }
  
}
