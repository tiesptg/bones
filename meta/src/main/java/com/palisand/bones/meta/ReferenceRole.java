package com.palisand.bones.meta;

import com.palisand.bones.meta.ui.PatternComponent;
import com.palisand.bones.tt.Editor;
import com.palisand.bones.tt.Link;
import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.LinkRules;
import com.palisand.bones.tt.Rules.RulesMap;
import com.palisand.bones.tt.Rules.StringRules;
import com.palisand.bones.tt.TextIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReferenceRole extends Member {

  private static final RulesMap RULES =
      Rules.map().and("opposite", LinkRules.builder().notNull(true).build())
          .and("entity", LinkRules.builder().notNull(true).build())
          .and("pointerPattern", StringRules.builder().notEmpty(true).build());

  @Override
  public Rules getConstraint(String field) {
    return RULES.of(field, super::getConstraint);
  }


  private String pointerPattern;
  private boolean external = false;
  private boolean notEmpty = false;
  private final Link<ReferenceRole, ReferenceRole> opposite =
      Link.newLink(this, ".*#/entities/.*/members/.*", role -> role.getOpposite());
  private final Link<ReferenceRole, Entity> entity =
      Link.newLink(this, ".*#/entities/.*", entity -> entity.getReferencedFrom());

  @Editor(PatternComponent.class)
  public String getPointerPattern() {
    return pointerPattern;
  }

  @TextIgnore
  public Type getType() {
    return Type.OBJECT;
  }

}
