package com.palisand.bones.tt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class EnumConstraint<N extends Node<?>> extends PropertyConstraint<N> {
	@Builder.Default
	private boolean notNull = true;
	
	@Override
	protected void doValidate(Validator validator, String field, Object value) {
		if (notNull && value == null) {
			validator.addViolation(field, "Field " + field + " should not be null");
		}
	}

}
