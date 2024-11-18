package com.palisand.bones.meta;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Reference extends Member {

	private String pointerPattern;
	private boolean external = false;
	
	@Override
	public Type getType() {
		return Type.OBJECT;
	}
}
