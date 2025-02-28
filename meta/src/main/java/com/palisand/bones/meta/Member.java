package com.palisand.bones.meta;

import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.RulesMap;
import com.palisand.bones.tt.Rules.StringRules;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class Member extends Item<Entity> {
  private static final RulesMap RULES = Rules.map()
  .and("name",StringRules.builder().notNull(true).pattern("[a-z]\\w+").build());
	
  @Override
  public Rules getConstraint(String field) {
    return RULES.of(field,super::getConstraint);
  }

	private boolean multiple;
	
	public abstract Type getType();

}
