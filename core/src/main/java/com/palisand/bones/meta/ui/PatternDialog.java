package com.palisand.bones.meta.ui;

import java.awt.GridLayout;
import java.awt.Rectangle;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.palisand.bones.meta.Contained;
import com.palisand.bones.meta.Entity;
import com.palisand.bones.meta.Model;
import com.palisand.bones.tt.Document;

import lombok.Getter;
import lombok.Setter;

public class PatternDialog extends JDialog {

  private static final long serialVersionUID = -469430723877885985L;
  
  record Branch(Entity entity, Contained contained) {
    
    boolean isUp() throws IOException {
      return contained == null && entity.getEntityContainer().get() != null;
    }
    
    @Override
    public String toString() {
      try {
        if (isUp()) {
          return "..:<" + entity.getName() + ">";
        } 
        if (contained == null) {
          return "#:<" + entity.getName() + ">";
        }
        return contained.getName() + ":<" + entity.getName() + ">";
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
          for (SoftReference<Document> ref: context.getRepository().getDocuments().values()) {
            Document document = ref.get();
            if (document != null && document instanceof Model model) {
              for (Entity entity: model.getEntities()) {
                if (entity.getEntityContainer().get() == null) {
                  result.add(new Branch(entity,null));
                }
              }
            }
          }
        } else if (object instanceof Branch branch) {
          Entity entity = branch.entity();
          if (!absolute && branch.isUp() && entity.getEntityContainer().get() != null) {
            result.add(new Branch(entity.getEntityContainer().get().getContainer(),null));
          }
          for (Contained contained: entity.getContainedEntities()) {
            result.add(new Branch(contained.getEntity().get(),contained));
          }
        }
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      return result;
    }
    

    @Override
    public Object getChild(Object branch, int i) {
      return getChildren((Branch)branch).get(i);
    }

    @Override
    public int getChildCount(Object branch) {
      return getChildren((Branch)branch).size();
    }

    @Override
    public int getIndexOfChild(Object branch, Object child) {
      return getChildren((Branch)branch).indexOf(child);
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
  private boolean accepted = false;
  private ModelTreeModel treeModel = null;
  
  public PatternDialog(JFrame frame, Entity context) {
    super(frame,"Choose Link Pattern",true);
    setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
    getRootPane().setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    add(tree);
    tree.setRootVisible(false);
    treeModel = new ModelTreeModel(context);
    add(field);
    
    JPanel buttons = new JPanel();
    buttons.setLayout(new GridLayout(1,2));
    add(buttons);
    
    JButton cancel = new JButton("Cancel");
    buttons.add(cancel);
    cancel.addActionListener(e -> setVisible(false));
    JButton ok = new JButton("OK");
    buttons.add(ok);
    ok.addActionListener(e -> {
      accepted = true;
      setVisible(false);
    });
    
    setSize(400,300);
    Rectangle rect = getParent().getBounds();
    setLocation((rect.width - getWidth())/2+rect.x,(rect.height-getHeight())/2+rect.y);
  }
  
  public void setPattern(String pattern) {
    field.setText(pattern);
    TreePath path = treeModel.getPath(pattern);
    tree.setSelectionPath(path);
  }
  
  public String getPattern() {
    return field.getText();
  }
  
  public static String editPattern(JFrame frame, Entity context, String pattern) {
    PatternDialog dialog = new PatternDialog(frame,context);
    dialog.setVisible(true);
    if (!dialog.accepted) {
      return null;
    }
    return dialog.getPattern();
  }
  
}
