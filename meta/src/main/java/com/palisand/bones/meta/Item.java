package com.palisand.bones.meta;

import java.io.IOException;
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
public class Item<N extends Node<?>> extends Node<N> {
  private static final RulesMap RULES =
      Rules.map().and("description", StringRules.builder().multiLine(true).build());

  @Override
  public Rules getConstraint(String field) {
    return RULES.of(field, super::getConstraint);
  }

  private String name = null;
  private String description = null;

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
