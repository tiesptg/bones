package com.palisand.bones.meta;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Reference extends Member {

	private String pointerPattern;
	private EntityType type = new EntityType();
	private boolean external = false;
}
