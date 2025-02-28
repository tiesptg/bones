package com.palisand.bones.meta;

import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.EnumRules;
import com.palisand.bones.tt.Rules.NumberRules;
import com.palisand.bones.tt.Rules.RulesMap;
import com.palisand.bones.tt.Rules.StringRules;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Attribute extends Member {
	private static final RulesMap RULES = Rules.map()
		.and("type", EnumRules.builder().notNull(true).notAllowed(Type.OBJECT).build())
		.and("maxLength", NumberRules.builder().enabled(attribute -> ((Attribute)attribute).getType() == Type.STRING).build())
		.and("minLength", NumberRules.builder().enabled(attribute -> ((Attribute)attribute).getType() == Type.STRING).build())
		.and("pattern", StringRules.builder().enabled(attribute -> ((Attribute)attribute).getType() == Type.STRING).build())
		.and("before", NumberRules.builder().enabled(attribute -> ((Attribute)attribute).getType() == Type.TIMESTAMP).build())
		.and("after", NumberRules.builder().enabled(attribute -> ((Attribute)attribute).getType() == Type.TIMESTAMP).build())
		.and("maxValue", NumberRules.builder().enabled(attribute -> ((Attribute)attribute).getType().isNumber()).build())
		.and("minValue", NumberRules.builder().enabled(attribute -> ((Attribute)attribute).getType().isNumber()).build())
		.and("step", NumberRules.builder().enabled(attribute -> ((Attribute)attribute).getType().isNumber()).build());
	
	@Override
	public Rules getConstraint(String field) {
		return RULES.of(field,super::getConstraint);
	}

	private Type type = Type.STRING;
	private String defaultValue = null;
	private boolean notNull = false;
	private Integer maxLength = 40;
	private Integer minLength = 0;
	private String pattern = null;
	private Long maxValue = null;
	private Long minValue = null;
	private Long step = 1l;
	private String before = null;
	private String after = null;
}
