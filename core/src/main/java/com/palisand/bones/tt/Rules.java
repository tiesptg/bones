package com.palisand.bones.tt;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class Rules<N extends Node<?>> {
	@Builder.Default
	private Function<N,Boolean> enabled = null;
	@Builder.Default
	private boolean notNull = false;
	
	public enum Severity {
		ERROR, WARNING
	}
	
	public record ConstraintViolation(Severity severity, Node<?> node, String field, String message) {}
	
	protected void doValidate(Validator validator,String field, Object value) {
		validator.assertNotNull(field, value);
	}
	
	public boolean isEnabled(N node) {
		if (enabled != null) {
			return enabled.apply(node);
		}
		return true;
	}
	
	public static class RulesMap<M extends Node<?>> {
		private final Map<String,Rules<M>> propertyRules = new TreeMap<>();
		
		public RulesMap<M> and(String fieldName, Rules<M> rules) {
			propertyRules.put(fieldName, rules);
			return this;
		}
		
		public Rules<M> of(String fieldName) {
			return propertyRules.get(fieldName);
		}
	}
	
	public static <X extends Node<?>> RulesMap<X> map() {
		return new RulesMap<X>();
	}
	
	@Getter
	@Setter
	@SuperBuilder
	public static class NumberRules<N extends Node<?>> extends Rules<N> {
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
			super.doValidate(validator, field, value);
			Number number = (Number)value;
			if (max != null && number.doubleValue() > max) {
				validator.addViolation(field, "Value should not be higher than " + max);
			}
			if (min != null && number.doubleValue() < min) {
				validator.addViolation(field, "Value should not be lower than " + min);
			}
			if (notZero && number.longValue() == 0) {
				validator.addViolation(field, "Value should not be 0");
			}
			if (number.toString().length() > size + 1) {
				validator.addViolation(field, "Value should not have more digits than " + size);
			}
			if (new BigDecimal(number.toString()).precision() > precision) {
				validator.addViolation(field, "Value should not have more than " + precision + " digits after the decimal point");
			}
		}
	}

	
	@Getter
	@Setter
	@SuperBuilder
	public static class StringRules<N extends Node<?>> extends Rules<N> {
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
			super.doValidate(validator, field, value);
			String str = (String)value;
			if (notEmpty && (str == null || str.isBlank())) {
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
	
	@Getter
	@Setter
	@SuperBuilder
	public static class EnumRules<N extends Node<?>> extends Rules<N> {
		
		@Singular("notAllowed")
		private List<Object> notAllowed;
		
		@Override
		protected void doValidate(Validator validator, String field, Object value) {
			super.doValidate(validator, field, value);
			if (value != null && notAllowed.contains(value)) {
				validator.addViolation(field, "Value " + value + " is not allowed for field " + field);
			}
		}

	}

	@Getter
	@Setter
	@SuperBuilder
	public static class ListRules<N extends Node<?>> extends Rules<N> {
		@Builder.Default()
		private boolean notEmpty = false;
		
		@Override
		protected void doValidate(Validator validator, String field, Object value) {
			super.doValidate(validator, field, value);
			List<?> list = (List<?>)value;
			if (notEmpty && list.isEmpty()) {
				validator.addViolation(field, "Field " + field + " should not be empty");
			}
		}

	}


}
