package com.palisand.bones.meta;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Reference extends Member {

	private String pointerPattern;
	private EntityType type = new EntityType();
	private boolean external = false;
}
