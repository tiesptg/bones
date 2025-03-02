package com.palisand.bones.meta;

import java.io.IOException;

import com.palisand.bones.tt.Link;
import com.palisand.bones.tt.Rules;
import com.palisand.bones.tt.Rules.EnumRules;
import com.palisand.bones.tt.Rules.LinkRules;
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
		.and("step", NumberRules.builder().enabled(attribute -> ((Attribute)attribute).getType().isNumber()).build())
		.and("enumType", LinkRules.builder().enabled(attribute -> ((Attribute)attribute).getType() == Type.ENUM).build());
	
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
	private Link<Attribute,EnumType> enumType = Link.newLink(this,".*#/enumTypes/.*");
	
	public String getJavaType() throws IOException {
	  switch (type) {
	  case STRING: return "String";
	  case INTEGER: return "Integer";
	  case LONG: return "Long";
	  case DOUBLE: return "Double";
	  case FLOAT: return "Float";
	  case TIMESTAMP: return "OffsetDateTime";
	  case BOOLEAN: return "Boolean";
	  case ENUM: return enumType.get().getName();
	  case OBJECT: break;
	  }
	  throw new IOException("attribute " + getName() + " has unsupported type " + type);
	}
	
	public String getJavaDefaultValue() {
	  if (getDefaultValue() != null) {
	    return getDefaultValue();
	  }
	  return "null";
	}
}
