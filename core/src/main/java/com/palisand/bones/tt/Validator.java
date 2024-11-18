package com.palisand.bones.tt;

import java.util.List;

import com.palisand.bones.tt.PropertyConstraint.ConstraintViolation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Validator {
	private Node<?> node;
	private List<ConstraintViolation> violations;
	
	public void assertNotNull(String field, Object value) {
		if (value == null) {
			violations.add(new ConstraintViolation(node,field,"Field " + field + " should not be null"));
		}
	}
	
	public void addViolation(String field, String message) {
		violations.add(new ConstraintViolation(node, field, message));
	}
}
