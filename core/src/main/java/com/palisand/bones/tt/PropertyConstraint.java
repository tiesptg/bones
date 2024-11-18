package com.palisand.bones.tt;

import java.util.function.Function;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public abstract class PropertyConstraint<N extends Node<?>> {
	@Builder.Default
	private Function<N,Boolean> enabled = null;
	@Builder.Default
	private boolean notNull = false;
	
	public record ConstraintViolation(Node<?> node, String field, String message) {}
	
	protected void doValidate(Validator validator,String field, Object value) {
		validator.assertNotNull(field, value);
	}
	
	public boolean isEnabled(N node) {
		if (enabled != null) {
			return enabled.apply(node);
		}
		return true;
	}

}
