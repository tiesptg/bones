package com.palisand.bones.meta;

import java.util.ArrayList;
import java.util.List;

import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.ListRules;
import com.palisand.bones.tt.Rules.RulesMap;
import com.palisand.bones.tt.Rules.StringRules;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EnumType extends Item<Model> {
	private static final RulesMap RULES = Rules.map()
	    .and("name",StringRules.builder().notNull(true).pattern("[A-Z]\\w+").build())
	    .and("values",ListRules.builder().notEmpty(true).build());
	
	@Override
	public Rules getConstraint(String field) {
		return RULES.of(field,super::getConstraint);
	}
	
	public List<String> values = new ArrayList<>();

}
