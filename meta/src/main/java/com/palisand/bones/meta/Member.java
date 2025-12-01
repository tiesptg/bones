package com.palisand.bones.meta;

import com.palisand.bones.tt.FieldOrder;
import com.palisand.bones.tt.Node;
import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.RulesMap;
import com.palisand.bones.tt.Rules.StringRules;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@FieldOrder({"multiple", "notNull", "enableWhen"})
public abstract class Member extends Item<Entity> {
  private static final RulesMap<Member> RULES = Rules.<Member>map().and("name",
      StringRules.<Member>builder().notNull(true).pattern("[a-z]\\w+").build());

  @SuppressWarnings("unchecked")
  @Override
  public Rules<? extends Node<?>> getConstraint(String field) {
    return RULES.of(field, super::getConstraint);
  }

  private boolean multiple;
  private boolean notNull;
  private String enabledWhen = null;

  public abstract Type getType();

}
