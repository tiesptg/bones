package com.palisand.bones.tt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class StringConstraint<N extends Node<?>> extends PropertyConstraint<N> {
	@Builder.Default
	private int maxLength = Integer.MAX_VALUE;
	@Builder.Default
	private int minLength = Integer.MIN_VALUE;
	@Builder.Default
	private boolean notEmpty = false;
	@Builder.Default
	private String pattern = null;
	
	@Override
	protected void doValidate(Validator validator, String field, Object value) {
		String str = (String)value;
		if (notEmpty && str == null || str.isBlank()) {
			validator.addViolation(field, "Field " + field + " should contain text");
		}
		if (str != null) {
			if (str.length() < minLength) {
				validator.addViolation(field, "Value should be at least " + minLength + " long");
			}
			if (str.length() > maxLength) {
				validator.addViolation(field, "Value should be at most " + maxLength + " long");
			}
			if (pattern != null && !str.matches(pattern)) {
				validator.addViolation(field, "Value should match pattern: " + pattern);
			}
		}
	}
}
