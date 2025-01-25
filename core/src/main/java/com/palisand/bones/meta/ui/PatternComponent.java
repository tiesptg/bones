package com.palisand.bones.meta.ui;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.palisand.bones.tt.CustomEditor;
import com.palisand.bones.tt.Node;

import lombok.Setter;

public class PatternComponent extends JTextField implements CustomEditor {

  private static final long serialVersionUID = 2888545899054208488L;
  @Setter private Node<?> node;
  
  public PatternComponent() {
    addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        selectAll();
      }
      
    });
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
