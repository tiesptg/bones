package com.palisand.bones.meta;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class Member extends Item<Entity> {
	
	private boolean multiple;
	
	public abstract Type<?> getType();

}
