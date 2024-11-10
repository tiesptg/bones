package com.palisand.bones.meta;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class Member extends Item<Entity> {
	
	private boolean multiple;
	
	public abstract Type<?> getType();

}
