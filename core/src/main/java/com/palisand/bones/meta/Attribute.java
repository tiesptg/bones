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
	private static final RulesMap<Attribute> RULES = Rules.<Attribute>map()
		.and("type", EnumRules.<Attribute>builder().notNull(true).notAllowed(Type.OBJECT).build())
		.and("maxLength", NumberRules.<Attribute>builder().enabled(attribute -> attribute.getType() == Type.STRING).build())
		.and("minLength", NumberRules.<Attribute>builder().enabled(attribute -> attribute.getType() == Type.STRING).build())
		.and("pattern", StringRules.<Attribute>builder().enabled(attribute -> attribute.getType() == Type.STRING).build())
		.and("before", NumberRules.<Attribute>builder().enabled(attribute -> attribute.getType() == Type.TIMESTAMP).build())
		.and("after", NumberRules.<Attribute>builder().enabled(attribute -> attribute.getType() == Type.TIMESTAMP).build())
		.and("maxValue", NumberRules.<Attribute>builder().enabled(attribute -> attribute.getType().isNumber()).build())
		.and("minValue", NumberRules.<Attribute>builder().enabled(attribute -> attribute.getType().isNumber()).build())
		.and("step", NumberRules.<Attribute>builder().enabled(attribute -> attribute.getType().isNumber()).build());
	
	@SuppressWarnings("unchecked")
	@Override
	public Rules<Attribute> getConstraint(String field) {
		return RULES.of(field);
	}

	private Type type = Type.STRING;
	private String defaultValue = null;
	private boolean notNull = false;
	private Integer maxLength = null;
	private Integer minLength = 0;
	private String pattern = null;
	private Long maxValue = null;
	private Long minValue = null;
	private Long step = 1l;
	private String before = null;
	private String after = null;
}
