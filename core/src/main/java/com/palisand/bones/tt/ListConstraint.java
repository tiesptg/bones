package com.palisand.bones.tt;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class ListConstraint<N extends Node<?>> extends PropertyConstraint<N> {
	@Builder.Default
	private boolean notNull = true;
	@Builder.Default()
	private boolean notEmpty = false;
	
	@Override
	protected void doValidate(Validator validator, String field, Object value) {
		List<?> list = (List<?>)value;
		if (notNull && list == null) {
			validator.addViolation(field, "Field " + field + " should not be null");
		} else if (notEmpty && list.isEmpty()) {
			validator.addViolation(field, "Field " + field + " should not be empty");
		}
	}

}
