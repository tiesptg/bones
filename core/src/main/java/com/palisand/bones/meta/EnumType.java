package com.palisand.bones.meta;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EnumType extends Type<Attribute> {
	
	public Class<? extends Enum<?>> type;

	@Override
	public String getId() {
		return type.getSimpleName();
	}
	
}
