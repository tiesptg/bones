package com.palisand.bones.meta.ui;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.palisand.bones.meta.Role;
import com.palisand.bones.tt.CustomEditor;
import com.palisand.bones.tt.Node;

public class PatternComponent extends JTextField implements CustomEditor {

  private static final long serialVersionUID = 2888545899054208488L;
  private Role role;
  
  public void setNode(Node<?> node) {
    role = (Role)node;
  }
  
  public PatternComponent() {
    addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        selectAll();
      }
      
    });
    addMouseListener(new MouseAdapter() {

      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
          showPatternEditor();
        }
      }
    });
    addKeyListener(new KeyAdapter() {

      @Override
      public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
          showPatternEditor();
        }
      }
      
    });
  }
  
  private void showPatternEditor() {
    PatternEditor.editPattern(getJFrame(),role.getContainer(),(String)getValue());
  }
  
  private JFrame getJFrame() {
    return (JFrame) SwingUtilities.getWindowAncestor(this);
  }

  @Override
  public void setValue(Object object) {
    if (object == null) {
      setText("");
    } else {
      setText(object.toString());
    }
  }

  @Override
  public Object getValue() {
    String str = getText();
    if (str == null || str.isBlank()) {
      return null;
    }
    return str;
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

  @Override
  public void onValueChanged(Consumer<Object> handler) {
    addKeyListener(new KeyAdapter() {

      @Override
      public void keyReleased(KeyEvent e) {
        handler.accept(getValue());
      }
      
    });
  }

}
