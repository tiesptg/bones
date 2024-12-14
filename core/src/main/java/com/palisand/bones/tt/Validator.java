package com.palisand.bones.tt;

import java.util.List;

import com.palisand.bones.tt.Rules.ConstraintViolation;
import com.palisand.bones.tt.Rules.Severity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Validator {
	private Node<?> node;
	private List<ConstraintViolation> violations;
	
	public void assertNotNull(String field, Object value) {
		if (value == null) {
			violations.add(new ConstraintViolation(Severity.ERROR,node,field,"Field " + field + " should not be null"));
		}
	}
	
	public void addViolation(String field, String message) {
		violations.add(new ConstraintViolation(Severity.ERROR,node, field, message));
	}
	
	public void addWarning(String field, String message) {
		violations.add(new ConstraintViolation(Severity.WARNING,node,field,message));
	}
	
	public boolean containsErrors() {
		return violations.stream().anyMatch(violation -> violation.severity() == Severity.ERROR);
	}
}
