package com.palisand.bones.meta;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AttributeType extends Type<Attribute> {
	
	@Override
	public String getId() {
		return this.getClass().getSimpleName();
	}
	
}
