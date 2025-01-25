package com.palisand.bones.tt;

import java.util.function.Consumer;

import javax.swing.JComponent;

public interface CustomEditor {
  void setNode(Node<?> node);
  void setValue(Object object);
  Object getValue();
  JComponent getComponent();
  void onValueChanged(Consumer<Object> handler);
}
