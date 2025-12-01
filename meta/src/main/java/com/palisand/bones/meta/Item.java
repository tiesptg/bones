package com.palisand.bones.meta;

import java.io.IOException;
import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.RulesMap;
import com.palisand.bones.tt.Rules.StringRules;
import com.palisand.bones.tt.TextIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@FieldOrder({"name", "description"})
public class Item<N extends Node<?>> extends Node<N> {
  private static final RulesMap<Item<?>> RULES = Rules.<Item<?>>map().and("description",
      StringRules.<Item<?>>builder().multiLine(true).build());

  @SuppressWarnings("unchecked")
  @Override
  public <M extends Node<?>> Rules<M> getConstraint(String field) {
    return (Rules<M>) RULES.of(field, super::getConstraint);
  }

  private String name = null;
  private String description = null;

  public String getName(String prefix) {
    StringBuilder sb = new StringBuilder(prefix);
    sb.append(Character.toUpperCase(getName().charAt(0)));
    sb.append(getName().substring(1));
    return sb.toString();
  }

  @TextIgnore
  @Override
  public String getId() {
    return name;
  }

  public void setName(String name) throws IOException {
    beforeIdChange(getName(), name);
    this.name = name;
  }

}
