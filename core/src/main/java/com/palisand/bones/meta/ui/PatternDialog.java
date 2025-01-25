package com.palisand.bones.meta.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.Repository;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

public class PatternDialog extends JDialog {

  private static final long serialVersionUID = -469430723877885985L;
  
  @Setter
  @RequiredArgsConstructor
  class ModelTreeModel implements TreeModel {
    private final List<TreeModelListener> listeners = new ArrayList<>();
    private Repository repository;
    private final Node<?> context;
    private final boolean absolute; 
    
    @Override
    public void addTreeModelListener(TreeModelListener l) {
      listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
      listeners.remove(l);
    }
    

    @Override
    public Object getChild(Object arg0, int arg1) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public int getChildCount(Object arg0) {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public int getIndexOfChild(Object arg0, Object arg1) {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public Object getRoot() {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean isLeaf(Object arg0) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public void valueForPathChanged(TreePath arg0, Object arg1) {
      // TODO Auto-generated method stub
      
    }
    
  }
  
  private JTree tree = new JTree();
  private JTextField field = new JTextField();
  

}
