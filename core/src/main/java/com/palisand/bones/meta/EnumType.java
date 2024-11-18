package com.palisand.bones.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.palisand.bones.tt.ListConstraint;
import com.palisand.bones.tt.PropertyConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EnumType extends Item<Model> {
	private static final Map<String,PropertyConstraint<?>> CONSTRAINTS = new TreeMap<>();
	
	static {
		CONSTRAINTS.put("values",ListConstraint.builder().notEmpty(true).build());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public PropertyConstraint<?> getConstraint(String field) {
		return CONSTRAINTS.get(field);
	}
	
	public List<String> values = new ArrayList<>();

}
