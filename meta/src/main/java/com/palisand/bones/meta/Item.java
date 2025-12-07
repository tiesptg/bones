package com.palisand.bones.meta;

import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.TextIgnore;
import com.palisand.bones.validation.NoXss;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@FieldOrder({"name", "description"})
public abstract class Item<N extends Node<?>> extends Node<N> {

  @MultiLine
  @NoXss private String description = null;

  public abstract String getName();

  public String getName(String prefix) {
    StringBuilder sb = new StringBuilder(prefix);
    sb.append(Character.toUpperCase(getName().charAt(0)));
    sb.append(getName().substring(1));
    return sb.toString();
  }

  @TextIgnore
  @Override
  public String getId() {
    return getName();
  }

}
