package com.palisand.bones.meta;

import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.tt.Link;
import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.BooleanRules;
import com.palisand.bones.tt.Rules.LinkRules;
import com.palisand.bones.tt.Rules.RulesMap;
import com.palisand.bones.tt.Rules.StringRules;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldOrder({"entity", "multiple", "notEmpty"})
public class ContainerRole extends Member {
  private static final RulesMap<ContainerRole> RULES = Rules.<ContainerRole>map()
      .and("name", StringRules.<ContainerRole>builder().notNull(true).pattern("[a-z]\\w+").build())
      .and("notEmpty",
          BooleanRules.<ContainerRole>builder()
              .enabled(object -> ((ContainerRole) object).isMultiple()).build())
      .and("entity", LinkRules.<ContainerRole>builder().notNull(true).build());

  @Override
  public Rules<? extends Node<?>> getConstraint(String field) {
    return RULES.of(field, super::getConstraint);
  }

  private Link<ContainerRole, Entity> entity =
      Link.newLink(this, ".*#/entities/.*", entity -> entity.getEntityContainer());
  private boolean multiple = true;
  private boolean notEmpty = false;

  @Override
  public Type getType() {
    return Type.OBJECT;
  }

}
