package com.palisand.bones.meta;

import java.util.ArrayList;
import java.util.List;

import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.ListRules;
import com.palisand.bones.tt.Rules.RulesMap;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EnumType extends Item<Model> {
	private static final RulesMap<EnumType> RULES = Rules.<EnumType>map().and("values",ListRules.<EnumType>builder().notEmpty(true).build());
	
	@SuppressWarnings("unchecked")
	@Override
	public Rules<EnumType> getConstraint(String field) {
		return RULES.of(field);
	}
	
	public List<String> values = new ArrayList<>();

}
