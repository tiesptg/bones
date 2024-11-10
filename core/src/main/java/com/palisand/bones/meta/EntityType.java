package com.palisand.bones.meta;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EntityType extends Type<Member> {

	public Entity type;

	@Override
	public String getId() {
		return type.getId();
	}
	
}
