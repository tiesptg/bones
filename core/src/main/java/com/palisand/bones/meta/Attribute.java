package com.palisand.bones.meta;

import java.util.Map;
import java.util.TreeMap;

import com.palisand.bones.tt.ListConstraint;
import com.palisand.bones.tt.NumberConstraint;
import com.palisand.bones.tt.PropertyConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Attribute extends Member {
	private static final Map<String,PropertyConstraint<Attribute>> CONSTRAINTS = new TreeMap<>();
	
	static {
		CONSTRAINTS.put("type",ListConstraint.<Attribute>builder().notEmpty(true).build());
		CONSTRAINTS.put("maxLength", NumberConstraint.<Attribute>builder().enabled(attribute -> attribute.getType() == Type.STRING).build());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public PropertyConstraint<Attribute> getConstraint(String field) {
		return CONSTRAINTS.get(field);
	}

	private Type type = null;
	private String defaultValue = null;
	private Integer maxLength = null;
	private Integer minLength = null;
	
}
