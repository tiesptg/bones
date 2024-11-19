package com.palisand.bones.meta;

import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.ListRules;
import com.palisand.bones.tt.Rules.NumberRules;
import com.palisand.bones.tt.Rules.RulesMap;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Attribute extends Member {
	private static final RulesMap<Attribute> RULES = Rules.<Attribute>map().and("type",ListRules.<Attribute>builder().notEmpty(true).build())
						.and("maxLength", NumberRules.<Attribute>builder().enabled(attribute -> attribute.getType() == Type.STRING).build());
	
	@SuppressWarnings("unchecked")
	@Override
	public Rules<Attribute> getConstraint(String field) {
		return RULES.of(field);
	}

	private Type type = null;
	private String defaultValue = null;
	private Integer maxLength = null;
	private Integer minLength = null;
	
}
