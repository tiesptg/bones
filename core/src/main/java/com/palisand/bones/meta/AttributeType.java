package com.palisand.bones.meta;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class AttributeType extends Type<Attribute> {
	
	@Override
	public String getId() {
		return this.getClass().getSimpleName();
	}
	
}
