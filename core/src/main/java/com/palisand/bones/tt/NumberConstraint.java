package com.palisand.bones.tt;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class NumberConstraint<N extends Node<?>> extends PropertyConstraint<N> {
	@Builder.Default
	private Double max = null;
	@Builder.Default
	private Double min = null;
	@Builder.Default
	private boolean notZero = false;
	@Builder.Default
	private int size = 40;
	@Builder.Default
	private int precision = 0;
	
	@Override
	protected void doValidate(Validator validator, String field, Object value) {
		Number number = (Number)value;
	}
}
