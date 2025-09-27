package com.palisand.bones.meta.ui;

import java.util.function.Consumer;

import javax.swing.JComponent;
import com.palisand.bones.tt.Node;

public interface CustomEditor {
  void setNode(Node<?> node);
  void setValue(Object object);
  Object getValue();
  JComponent getComponent();
  void onValueChanged(Consumer<Object> handler);
}
