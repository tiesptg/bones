package com.palisand.bones.meta;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntityType extends Type<Member> {

	public Entity type;

	@Override
	public String getId() {
		return type.getId();
	}
	
}
